/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
 *
 * This file is part of Xeres.
 *
 * Xeres is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xeres is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeres.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.xeres.app.xrs.service.turtle;

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.LocationService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceMaster;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.turtle.item.*;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.util.NoSuppressedRunnable;
import io.xeres.common.util.SecureRandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.xeres.app.xrs.service.RsServiceType.TURTLE;

@Component
public class TurtleRsService extends RsService implements RsServiceMaster<TurtleRsClient>, TurtleRouter
{
	private static final Logger log = LoggerFactory.getLogger(TurtleRsService.class);

	/**
	 * Time between tunnel management runs.
	 */
	private static final Duration TUNNEL_MANAGEMENT_DELAY = Duration.ofSeconds(2);

	public static final int MAX_TUNNEL_DEPTH = 6;

	public static final Duration EMPTY_TUNNELS_DIGGING_TIME = Duration.ofSeconds(50);

	private static final Duration REGULAR_TUNNELS_DIGGING_TIME = Duration.ofSeconds(300);

	private static final Duration TUNNEL_CLEANING_TIME = Duration.ofSeconds(10);

	private static final int MAX_SEARCH_REQUEST_IN_CACHE = 120;

	private static final int MAX_SEARCH_HITS = 100;

	private static final Duration SEARCH_REQUEST_TIMEOUT = Duration.ofSeconds(20);

	private static final Duration TUNNEL_REQUEST_TIMEOUT = Duration.ofSeconds(20);

	private final TunnelProbability tunnelProbability = new TunnelProbability();

	private final Map<Integer, SearchRequest> searchRequestsOrigins = new ConcurrentHashMap<>();

	private final Map<Integer, TunnelRequest> tunnelRequestsOrigins = new ConcurrentHashMap<>();

	private final Map<Sha1Sum, FileHash> incomingHashes = new ConcurrentHashMap<>();

	private final Map<Integer, Tunnel> localTunnels = new ConcurrentHashMap<>();

	private final Map<LocationId, Integer> virtualPeers = new ConcurrentHashMap<>();

	private final Set<Sha1Sum> hashesToRemove = ConcurrentHashMap.newKeySet();

	private final List<TurtleRsClient> turtleClients = new ArrayList<>();

	private final PeerConnectionManager peerConnectionManager;

	private final LocationService locationService;

	private final DatabaseSessionManager databaseSessionManager;

	private ScheduledExecutorService executorService;

	private Location ownLocation;

	private Instant lastTunnelCleanup = Instant.EPOCH;

	protected TurtleRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, LocationService locationService, DatabaseSessionManager databaseSessionManager)
	{
		super(rsServiceRegistry);
		this.peerConnectionManager = peerConnectionManager;
		this.locationService = locationService;
		this.databaseSessionManager = databaseSessionManager;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return TURTLE;
	}

	@Override
	public void addRsSlave(TurtleRsClient client)
	{
		turtleClients.add(client);
	}

	@Override
	public void initialize()
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			ownLocation = locationService.findOwnLocation().orElseThrow();
		}

		executorService = Executors.newSingleThreadScheduledExecutor();

		executorService.scheduleAtFixedRate((NoSuppressedRunnable) this::manageAll,
				getInitPriority().getMaxTime() + TUNNEL_MANAGEMENT_DELAY.toSeconds() / 2,
				TUNNEL_MANAGEMENT_DELAY.toSeconds(),
				TimeUnit.SECONDS
		);
	}

	@Override
	public void cleanup()
	{
		if (executorService != null) // Can happen when running tests
		{
			executorService.shutdownNow();
		}
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		if (item instanceof TurtleTunnelRequestItem turtleTunnelRequestItem)
		{
			handleTunnelRequest(sender, turtleTunnelRequestItem);
		}
		else if (item instanceof TurtleTunnelResultItem turtleTunnelResultItem)
		{
			handleTunnelResult(sender, turtleTunnelResultItem);
		}
		else if (item instanceof TurtleSearchRequestItem turtleSearchRequestItem)
		{
			handleSearchRequest(sender, turtleSearchRequestItem);
		}
		else if (item instanceof TurtleSearchResultItem turtleSearchResultItem)
		{
			handleSearchResult(sender, turtleSearchResultItem);
		}
	}

	private void handleTunnelRequest(PeerConnection sender, TurtleTunnelRequestItem item)
	{
		log.debug("Received tunnel request from peer {}: {}", sender, item);

		if (item.getFileHash() == null) // XXX: not sure what to do with those (RS has no code handling that...)
		{
			log.debug("null filehash, dropping...");
			return;
		}

		if (isBanned(item.getFileHash()))
		{
			log.debug("Rejecting banned file hash {}", item.getFileHash());
			return;
		}

		if (tunnelRequestsOrigins.putIfAbsent(item.getRequestId(), new TunnelRequest(sender.getLocation(), item.getDepth())) != null)
		{
			// This can happen when the same tunnel request is relayed by different peers.
			// Simply drop it.
			log.debug("Requests {} already exists", item.getRequestId());
			return;
		}

		var client = turtleClients.stream()
				.filter(turtleRsClient -> turtleRsClient.handleTunnelRequest(sender, item.getFileHash()))
				.findFirst();

		if (client.isPresent())
		{
			var tunnelId = item.getPartialTunnelId() ^ generatePersonalFilePrint(item.getFileHash(), tunnelProbability.getBias(), false);
			var resultItem = new TurtleTunnelResultItem(item.getRequestId(), tunnelId);
			peerConnectionManager.writeItem(sender, resultItem, this);

			// XXX: fix all that once I implemented the TunnelDirection.SERVER.. there are too many side effects (3 calls in the right order, etc...)
			localTunnels.put(tunnelId, new Tunnel(sender.getLocation(), ownLocation, item.getFileHash()));
			addDistantPeer(tunnelId);

			client.get().addVirtualPeer(item.getFileHash(), localTunnels.get(tunnelId).getVirtualId(), TunnelDirection.CLIENT);
			return;
		}

		if (tunnelProbability.isForwardable(item)) // XXX: this is different there! needs the number of peers and speed...
		{
			peerConnectionManager.doForAllPeersExceptSender(peerConnection -> {
						var itemToSend = item.clone();
						tunnelProbability.incrementDepth(itemToSend);
						peerConnectionManager.writeItem(peerConnection, itemToSend, this);
					},
					sender,
					this);
		}
	}

	private void handleTunnelResult(PeerConnection sender, TurtleTunnelResultItem item)
	{
		var tunnelRequest = tunnelRequestsOrigins.get(item.getRequestId());
		if (tunnelRequest == null)
		{
			log.warn("Tunnel result has no request");
			return;
		}

		if (tunnelRequest.hasResponseAlready(item.getTunnelId()))
		{
			log.error("Received a tunnel response twice. This should not happen.");
			return;
		}
		else
		{
			tunnelRequest.addResponse(item.getTunnelId());
		}

		var tunnel = localTunnels.getOrDefault(item.getTunnelId(),
				new Tunnel(tunnelRequest.getSource(), sender.getLocation())
		);

		if (Duration.between(tunnelRequest.getLastUsed(), Instant.now()).compareTo(TUNNEL_REQUEST_TIMEOUT) > 0)
		{
			log.warn("Tunnel request is known but the tunnel result arrived too late, dropping");
			return;
		}

		// Check if it's for ourselves
		if (tunnelRequest.getSource().equals(ownLocation))
		{
			var fileHash = findFileHashByRequest(item.getRequestId());
			if (fileHash != null)
			{
				fileHash.getValue().addTunnel(item.getTunnelId());

				tunnel.setHash(fileHash.getKey()); // local tunnel

				addDistantPeer(item.getTunnelId());
				fileHash.getValue().getClient().addVirtualPeer(fileHash.getKey(), tunnel.getVirtualId(), TunnelDirection.SERVER);
			}
		}
		else
		{
			// Forward the result back to its source
			peerConnectionManager.writeItem(tunnelRequest.getSource(), new TurtleTunnelResultItem(item.getTunnelId(), item.getRequestId()), this);
		}
	}

	private Map.Entry<Sha1Sum, FileHash> findFileHashByRequest(int requestId)
	{
		return incomingHashes.entrySet().stream()
				.filter(integerFileHashEntry -> integerFileHashEntry.getValue().getLastRequest() == requestId)
				.findFirst()
				.orElse(null);
	}

	private void addDistantPeer(int tunnelId)
	{
		localTunnels.get(tunnelId).setVirtualId(VirtualLocationId.fromTunnel(tunnelId));
	}

	int generatePersonalFilePrint(Sha1Sum hash, int bias, boolean symetrical)
	{
		var buf = hash.toString() + ownLocation.toString();
		int result = bias;
		int decal = 0;

		for (int i = 0; i < buf.length(); i++)
		{
			result += (int) (7 * buf.charAt(i) + Integer.toUnsignedLong(decal));

			if (symetrical)
			{
				decal = (int) (Integer.toUnsignedLong(decal) * 44497 + 15641 + (Integer.toUnsignedLong(result) % 86243));
			}
			else
			{
				decal = (int) (Integer.toUnsignedLong(decal) * 86243 + 15649 + (Integer.toUnsignedLong(result) % 44497));
			}
		}
		return result;
	}

	private void handleSearchRequest(PeerConnection sender, TurtleSearchRequestItem item)
	{
		log.debug("Received search request from peer {}: {}", sender, item);

		// XXX: check maximum size

		if (searchRequestsOrigins.size() > MAX_SEARCH_REQUEST_IN_CACHE)
		{
			log.debug("Request cache is full. Check if a peer is flooding.");
			return;
		}

		if (searchRequestsOrigins.putIfAbsent(item.getRequestId(), new SearchRequest(sender.getLocation(),
				item.getDepth(),
				item.getKeywords(),
				0,
				MAX_SEARCH_HITS)) != null)
		{
			log.debug("Request {} already in cache", item.getRequestId());
			return;
		}

		// XXX: perform local search and send back the results to the sender

		if (tunnelProbability.isForwardable(item))
		{
			peerConnectionManager.doForAllPeersExceptSender(peerConnection -> {
						var itemToSend = item.clone();
						tunnelProbability.incrementDepth(itemToSend);
						peerConnectionManager.writeItem(peerConnection, itemToSend, this);
					},
					sender,
					this);
		}
	}

	private void handleSearchResult(PeerConnection sender, TurtleSearchResultItem item)
	{
		log.debug("Received search result from peer {}: {}", sender, item);

		if (item instanceof TurtleFileSearchResultItem turtleFileSearchResultItem)
		{
			// XXX: check isBanned() on the fileInfo hashes
		}

		var searchRequest = searchRequestsOrigins.get(item.getRequestId());
		if (searchRequest == null)
		{
			log.warn("Search result for request {} doesn't exist in the cache", item);
			return;
		}

		if (Duration.between(searchRequest.getLastUsed(), Instant.now()).compareTo(SEARCH_REQUEST_TIMEOUT) > 0)
		{
			log.debug("Search result arrived too late, dropping...");
			return;
		}

		// XXX: make sure we don't forward when source is our own ID (and add some caching because that is done a lot)

		// XXX: if the searchRequest.getResultCount() + total is bigger than searchRequest.getHitLimit(), then trim the result
		// XXX: otherwise just increment the result count with the total. See what RS does there.

		// Forward the item to origin
		peerConnectionManager.writeItem(searchRequest.getSource(), item.clone(), this);
	}

	private boolean isBanned(Sha1Sum fileHash)
	{
		return false; // TODO: implement
	}

	@Override
	public void monitorTunnels(Sha1Sum hash, TurtleRsClient client, boolean allowMultiTunnels)
	{
		hashesToRemove.remove(hash); // the file hash was scheduled for removal, cancel it

		incomingHashes.putIfAbsent(hash, new FileHash(allowMultiTunnels, client));
	}

	private void manageAll()
	{
		manageTunnels();

		// XXX: compute traffic information

		cleanTunnelsIfNeeded();
		// XXX: clean every 10 seconds

		// XXX: estimate speed
	}

	private void manageTunnels()
	{
		var now = Instant.now();

		incomingHashes.entrySet().stream()
				.filter(entry -> {
					var fileHash = entry.getValue();
					var totalSpeed = fileHash.getTunnels().stream()
							.mapToDouble(tunnelId -> localTunnels.get(tunnelId).getSpeed())
							.sum();

					var tunnelKeepingFactor = (Math.max(1.0, totalSpeed / (50 * 1024)) - 1.0) + 1.0;

					return ((!fileHash.hasTunnels() && Duration.between(fileHash.getLastDiggTime(), now).compareTo(EMPTY_TUNNELS_DIGGING_TIME) > 0) ||
							(fileHash.isAggressiveMode() && Duration.between(fileHash.getLastDiggTime(), now).compareTo(Duration.ofSeconds((long) (REGULAR_TUNNELS_DIGGING_TIME.toSeconds() * tunnelKeepingFactor))) > 0));
				})
				.sorted(Comparator.comparing(entry -> entry.getValue().getLastDiggTime()))
				.map(Map.Entry::getKey)
				.findFirst()
				.ifPresent(this::diggTunnel);
	}

	private void diggTunnel(Sha1Sum hash)
	{
		var id = SecureRandomUtils.nextInt();

		var fileHash = incomingHashes.get(hash);

		fileHash.setLastRequest(id);
		fileHash.setLastDiggTime(Instant.now());

		var item = new TurtleTunnelRequestItem(hash, id, generatePersonalFilePrint(hash, tunnelProbability.getBias(), true));

		// XXX: send it! do not use handleTunnelRequest() directly but something close, though...
	}

	private void cleanTunnelsIfNeeded()
	{
		var now = Instant.now();

		if (Duration.between(lastTunnelCleanup, now).compareTo(TUNNEL_CLEANING_TIME) > 0)
		{
			lastTunnelCleanup = now;

			hashesToRemove.stream()
					.filter(incomingHashes::containsKey) // XXX: needed? what happens with null maps?
					.map(incomingHashes::get)
					.map(FileHash::getTunnels)
					.forEach(tunnelId -> tunnelId.forEach(this::closeTunnel));

			hashesToRemove.clear();
		}
	}

	private void closeTunnel(int id)
	{
		// XXX: implement...
	}
}
