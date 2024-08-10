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
import io.xeres.app.database.model.file.File;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.file.FileService;
import io.xeres.app.util.expression.ExpressionMapper;
import io.xeres.app.util.expression.NameExpression;
import io.xeres.app.util.expression.StringExpression;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemUtils;
import io.xeres.app.xrs.serialization.SerializerSizeCache;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceMaster;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.turtle.item.*;
import io.xeres.common.file.FileType;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.util.ExecutorUtils;
import io.xeres.common.util.SecureRandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import static io.xeres.app.xrs.service.RsServiceType.TURTLE;

/**
 * Implementation of the {@link TurtleRouter}. Only supports encrypted hashes.
 */
@Component
public class TurtleRsService extends RsService implements RsServiceMaster<TurtleRsClient>, TurtleRouter
{
	private static final Logger log = LoggerFactory.getLogger(TurtleRsService.class);

	/**
	 * Time between tunnel management runs.
	 */
	private static final Duration TUNNEL_MANAGEMENT_DELAY = Duration.ofSeconds(2);

	/**
	 * Maximum tunnel depth, that is the number of friends beyond you that are reachable.
	 */
	public static final int MAX_TUNNEL_DEPTH = 6;

	/**
	 * Time between checks of empty tunnels.
	 */
	public static final Duration EMPTY_TUNNELS_DIGGING_TIME = Duration.ofSeconds(50);

	/**
	 * Time between checks of normal tunnels.
	 */
	private static final Duration REGULAR_TUNNELS_DIGGING_TIME = Duration.ofSeconds(300);

	/**
	 * Time between tunnels cleanup.
	 */
	private static final Duration TUNNEL_CLEANING_TIME = Duration.ofSeconds(10);

	/**
	 * Time between tunnel speed estimation runs.
	 */
	private static final Duration SPEED_ESTIMATE_TIME = Duration.ofSeconds(5);

	/**
	 * Maximum number of search requests allowed in the cache.
	 */
	private static final int MAX_SEARCH_REQUEST_IN_CACHE = 120;

	/**
	 * Maximum number of search results forwarded by default.
	 */
	private static final int MAX_SEARCH_HITS = 100;

	private static final int MAX_SEARCH_REQUEST_ACCEPTED_SERIAL_SIZE = 200;

	private static final int MAX_SEARCH_RESPONSE_SERIAL_SIZE = 10000;

	/**
	 * Maximum lifetime of unused tunnels.
	 */
	private static final Duration MAX_TUNNEL_IDLE_TIME = Duration.ofSeconds(60);

	/**
	 * Lifetime of search requests in the cache.
	 */
	private static final Duration SEARCH_REQUEST_LIFETIME = Duration.ofSeconds(600);

	/**
	 * Lifetime of tunnel requests in the cache.
	 */
	private static final Duration TUNNEL_REQUEST_LIFETIME = Duration.ofSeconds(600);

	/**
	 * Lifetime of an ongoing search requests. Results coming after that time are dropped.
	 */
	private static final Duration SEARCH_REQUEST_TIMEOUT = Duration.ofSeconds(20);

	/**
	 * Lifetime of an ongoing tunnel requests. Results coming after that time are dropped.
	 */
	private static final Duration TUNNEL_REQUEST_TIMEOUT = Duration.ofSeconds(20);

	private final TunnelProbability tunnelProbability = new TunnelProbability();

	private final Map<Integer, SearchRequest> searchRequestsOrigins = new ConcurrentHashMap<>();

	private final Map<Integer, TunnelRequest> tunnelRequestsOrigins = new ConcurrentHashMap<>();

	private final Map<Sha1Sum, HashInfo> incomingHashes = new ConcurrentHashMap<>();

	private final Map<Integer, Tunnel> localTunnels = new ConcurrentHashMap<>();

	private final Map<LocationId, Integer> virtualPeers = new ConcurrentHashMap<>();

	private final Set<Sha1Sum> hashesToRemove = ConcurrentHashMap.newKeySet();

	private final Map<Integer, TurtleRsClient> outgoingTunnelClients = new ConcurrentHashMap<>();

	private final List<TurtleRsClient> turtleClients = new ArrayList<>();

	private final PeerConnectionManager peerConnectionManager;

	private final LocationService locationService;

	private final DatabaseSessionManager databaseSessionManager;

	private final FileService fileService;

	private ScheduledExecutorService executorService;

	private Location ownLocation;

	private Instant lastTunnelCleanup = Instant.EPOCH;

	private Instant lastSpeedEstimation = Instant.EPOCH;

	private TurtleStatistics turtleStatistics = new TurtleStatistics();
	private final TurtleStatistics turtleStatisticsBuffer = new TurtleStatistics();

	protected TurtleRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, LocationService locationService, DatabaseSessionManager databaseSessionManager, FileService fileService)
	{
		super(rsServiceRegistry);
		this.peerConnectionManager = peerConnectionManager;
		this.locationService = locationService;
		this.databaseSessionManager = databaseSessionManager;
		this.fileService = fileService;
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
		client.initializeTurtle(this);
	}

	@Override
	public void initialize()
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			ownLocation = locationService.findOwnLocation().orElseThrow();
		}

		executorService = ExecutorUtils.createFixedRateExecutor(this::manageAll,
				getInitPriority().getMaxTime() + TUNNEL_MANAGEMENT_DELAY.toSeconds() / 2,
				TUNNEL_MANAGEMENT_DELAY.toSeconds());
	}

	@Override
	public void cleanup()
	{
		ExecutorUtils.cleanupExecutor(executorService);
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		switch (item)
		{
			case TurtleGenericTunnelItem turtleGenericTunnelItem -> routeGenericTunnel(sender, turtleGenericTunnelItem);
			case TurtleTunnelRequestItem turtleTunnelRequestItem -> handleTunnelRequest(sender, turtleTunnelRequestItem);
			case TurtleTunnelResultItem turtleTunnelResultItem -> handleTunnelResult(sender, turtleTunnelResultItem);
			case TurtleSearchRequestItem turtleSearchRequestItem -> handleSearchRequest(sender, turtleSearchRequestItem);
			case TurtleSearchResultItem turtleSearchResultItem -> handleSearchResult(sender, turtleSearchResultItem);
			default -> log.debug("Unknown item {}", item);
		}
	}

	@Override
	public void forceReDiggTunnel(Sha1Sum hash)
	{
		if (!incomingHashes.containsKey(hash))
		{
			return;
		}
		diggTunnel(hash);
	}

	@Override
	public void sendTurtleData(Location virtualPeer, TurtleGenericTunnelItem item)
	{
		var tunnelId = virtualPeers.get(virtualPeer.getLocationId());
		if (tunnelId == null)
		{
			log.warn("Couldn't find tunnel for virtual peer id {}", virtualPeer.getLocationId());
			return;
		}

		var tunnel = localTunnels.get(tunnelId);
		if (tunnel == null)
		{
			log.warn("Client asked to send a packet through a tunnel that has been deleted.");
			return;
		}

		item.setTunnelId(tunnelId);

		var itemSerializedSize = ItemUtils.getItemSerializedSize(item, this);

		if (item.shouldStampTunnel())
		{
			tunnel.stamp();
		}

		tunnel.addTransferredBytes(itemSerializedSize);

		if (tunnel.getSource().equals(ownLocation))
		{
			item.setDirection(TunnelDirection.SERVER);
			turtleStatisticsBuffer.addToDataDownload(itemSerializedSize);
			peerConnectionManager.writeItem(tunnel.getDestination(), item, this);
		}
		else if (tunnel.getDestination().equals(ownLocation))
		{
			item.setDirection(TunnelDirection.CLIENT);
			turtleStatisticsBuffer.addToDataUpload(itemSerializedSize);
			peerConnectionManager.writeItem(tunnel.getSource(), item, this);
		}
		else
		{
			log.error("Asked to send a packet into a tunnel that is not registered, dropping packet");
		}
	}

	@Override
	public void startMonitoringTunnels(Sha1Sum hash, TurtleRsClient client, boolean allowMultiTunnels)
	{
		log.debug("Start monitoring tunnels for (encrypted) hash {}", hash);
		hashesToRemove.remove(hash); // if the file hash was scheduled for removal, cancel it

		incomingHashes.putIfAbsent(hash, new HashInfo(allowMultiTunnels, client));
	}

	@Override
	public void stopMonitoringTunnels(Sha1Sum hash)
	{
		log.debug("Stop monitoring tunnels for (encrypted) hash {}", hash);
		hashesToRemove.add(hash);
	}

	private void routeGenericTunnel(PeerConnection sender, TurtleGenericTunnelItem item)
	{
		var tunnel = localTunnels.get(item.getTunnelId());
		if (tunnel == null)
		{
			log.error("Got file map with unknown tunnel id {}", item.getTunnelId());
			return;
		}

		if (item.shouldStampTunnel())
		{
			tunnel.stamp();
		}

		var serializedSize = ItemUtils.getItemSerializedSize(item, this);
		tunnel.addTransferredBytes(serializedSize);

		if (sender.getLocation().equals(tunnel.getDestination()))
		{
			item.setDirection(TunnelDirection.CLIENT);
		}
		else if (sender.getLocation().equals(tunnel.getSource()))
		{
			item.setDirection(TunnelDirection.SERVER);
		}
		else
		{
			log.error("Generic tunnel mismatch source/destination id");
			return;
		}

		if (sender.getLocation().equals(tunnel.getDestination()) && !tunnel.getSource().equals(ownLocation))
		{
			log.debug("Forwarding generic item to {}", tunnel.getSource());

			turtleStatisticsBuffer.addToForwardTotal(serializedSize);

			peerConnectionManager.writeItem(tunnel.getSource(), item.clone(), this);
			return;
		}

		if (sender.getLocation().equals(tunnel.getSource()) && !tunnel.getDestination().equals(ownLocation))
		{
			log.debug("Forwarding generic item to {}", tunnel.getDestination());

			turtleStatisticsBuffer.addToForwardTotal(serializedSize);

			peerConnectionManager.writeItem(tunnel.getDestination(), item.clone(), this);
			return;
		}

		// Item is for us
		turtleStatisticsBuffer.addToDataDownload(serializedSize);

		handleReceiveGenericTunnel(item, tunnel);
	}

	@Override
	public boolean isVirtualPeer(Location location)
	{
		return virtualPeers.containsKey(location.getLocationId());
	}

	private void handleReceiveGenericTunnel(TurtleGenericTunnelItem item, Tunnel tunnel)
	{
		TurtleRsClient client = null;

		if (tunnel.getSource().equals(ownLocation))
		{
			var hashInfo = incomingHashes.get(tunnel.getHash());
			if (hashInfo == null)
			{
				log.warn("Hash {} for client side tunnel endpoint {} has been removed (late response?), dropping", tunnel.getHash(), item.getTunnelId());
				return;
			}
			client = hashInfo.getClient();
		}
		else if (tunnel.getDestination().equals(ownLocation))
		{
			client = outgoingTunnelClients.get(item.getTunnelId());
			if (client == null)
			{
				log.warn("Hash {} for server side tunnel endpoint {} has been removed (late response?), dropping", tunnel.getHash(), item.getTunnelId());
				return;
			}
		}
		assert client != null;
		client.receiveTurtleData(item, tunnel.getHash(), tunnel.getVirtualLocation(), item.getDirection());
	}

	private void handleTunnelRequest(PeerConnection sender, TurtleTunnelRequestItem item)
	{
		log.trace("Received tunnel request from peer {}: {}", sender, item);

		turtleStatisticsBuffer.addToTunnelRequestsDownload(SerializerSizeCache.getItemSize(item, this));

		// RS sometimes sends null (0000...) hashes
		if (item.getHash() == null)
		{
			log.debug("Null hash in tunnel request, dropping...");
			return;
		}

		if (isBanned(item.getHash()))
		{
			log.debug("Rejecting banned file hash {}", item.getHash());
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
				.filter(turtleRsClient -> turtleRsClient.handleTunnelRequest(sender, item.getHash()))
				.findFirst();

		if (client.isPresent())
		{
			log.debug("Honoring tunnel request from peer {}: {}", sender, item);
			var tunnelId = item.getPartialTunnelId() ^ generatePersonalFilePrint(item.getHash(), tunnelProbability.getBias(), false);
			var resultItem = new TurtleTunnelResultItem(tunnelId, item.getRequestId());

			var tunnel = new Tunnel(tunnelId, sender.getLocation(), ownLocation, item.getHash());
			localTunnels.put(tunnelId, tunnel);
			virtualPeers.put(tunnel.getVirtualLocation().getLocationId(), tunnelId);
			client.get().addVirtualPeer(item.getHash(), tunnel.getVirtualLocation(), TunnelDirection.CLIENT);

			outgoingTunnelClients.put(tunnelId, client.get());

			peerConnectionManager.writeItem(sender, resultItem, this);
			return;
		}

		// Perturb the partial tunnel id so that:
		// - the tunnel id is unique for a given route
		// - better balance of bandwidth for a given transfer
		// - avoids the waste of items that get lost when re-routing a tunnel
		item.setPartialTunnelId(generatePersonalFilePrint(item.getHash(), item.getPartialTunnelId() ^ tunnelProbability.getBias(), true));

		if (tunnelProbability.isForwardable(item))
		{
			var probability = tunnelProbability.getForwardingProbability(
					item,
					turtleStatistics.getTunnelRequestsUpload(),
					turtleStatistics.getTunnelRequestsDownload(),
					peerConnectionManager.getNumberOfPeers());// XXX: there's a difference with RS here, it's the number of peers USING the turtle service. do we care?

			peerConnectionManager.doForAllPeersExceptSender(peerConnection -> {
						var itemToSend = item.clone();
						tunnelProbability.incrementDepth(itemToSend);
						if (SecureRandomUtils.nextDouble() <= probability)
						{
							turtleStatisticsBuffer.addToTunnelRequestsUpload(SerializerSizeCache.getItemSize(itemToSend, this));
							peerConnectionManager.writeItem(peerConnection, itemToSend, this);
						}
					},
					sender,
					this);
		}
	}

	private void handleTunnelResult(PeerConnection sender, TurtleTunnelResultItem item)
	{
		log.debug("Got tunnel result from {}: {}", sender, item);
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

		if (Duration.between(tunnelRequest.getLastUsed(), Instant.now()).compareTo(TUNNEL_REQUEST_TIMEOUT) > 0)
		{
			log.warn("Tunnel request is known but the tunnel result arrived too late, dropping");
			return;
		}

		// Check if it's for ourselves
		if (tunnelRequest.getSource().equals(ownLocation))
		{
			var hashInfo = findHashInfoByRequest(item.getRequestId());
			if (hashInfo != null)
			{
				hashInfo.getValue().addTunnel(item.getTunnelId());

				// Local tunnel
				var tunnel = localTunnels.computeIfAbsent(item.getTunnelId(), tunnelId -> new Tunnel(item.getTunnelId(), tunnelRequest.getSource(), sender.getLocation(), hashInfo.getKey()));
				virtualPeers.put(tunnel.getVirtualLocation().getLocationId(), item.getTunnelId());
				hashInfo.getValue().getClient().addVirtualPeer(hashInfo.getKey(), tunnel.getVirtualLocation(), TunnelDirection.SERVER);
			}
		}
		else
		{
			// Forward the result back to its source
			peerConnectionManager.writeItem(tunnelRequest.getSource(), new TurtleTunnelResultItem(item.getTunnelId(), item.getRequestId()), this);
		}
	}

	private Map.Entry<Sha1Sum, HashInfo> findHashInfoByRequest(int requestId)
	{
		return incomingHashes.entrySet().stream()
				.filter(entry -> entry.getValue().getLastRequest() == requestId)
				.findFirst()
				.orElse(null);
	}

	int generatePersonalFilePrint(Sha1Sum hash, int bias, boolean symmetrical)
	{
		var buf = hash.toString() + ownLocation.toString();
		int result = bias;
		var decal = 0;

		for (var i = 0; i < buf.length(); i++)
		{
			result += (int) (7 * buf.charAt(i) + Integer.toUnsignedLong(decal));

			if (symmetrical)
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

		var itemSerializedSize = ItemUtils.getItemSerializedSize(item, this);

		turtleStatisticsBuffer.addToSearchRequestsDownload(itemSerializedSize);

		if (itemSerializedSize > MAX_SEARCH_REQUEST_ACCEPTED_SERIAL_SIZE)
		{
			log.warn("Got an arbitrary large size from {} of size {} and depth {}. Dropping", sender, itemSerializedSize, item.getDepth());
			return;
		}

		if (searchRequestsOrigins.size() > MAX_SEARCH_REQUEST_IN_CACHE)
		{
			log.debug("Request cache is full. Check if a peer is flooding.");
			return;
		}

		var searchResults = performLocalSearch(item, MAX_SEARCH_HITS);
		searchResults.forEach(turtleSearchResultItem -> {
			turtleSearchResultItem.setRequestId(item.getRequestId());
			peerConnectionManager.writeItem(sender, turtleSearchResultItem, this);
		});

		var searchRequest = new SearchRequest(sender.getLocation(),
				item.getDepth(),
				item.getKeywords(),
				searchResults.size(),
				MAX_SEARCH_HITS);

		if (searchRequestsOrigins.putIfAbsent(item.getRequestId(), searchRequest) != null)
		{
			log.debug("Request {} already in cache", item.getRequestId());
			return;
		}

		// Do not search further if enough has been sent back already.
		if (searchRequest.isFull())
		{
			return;
		}

		if (tunnelProbability.isForwardable(item))
		{
			peerConnectionManager.doForAllPeersExceptSender(peerConnection -> {
						var itemToSend = item.clone();
						tunnelProbability.incrementDepth(itemToSend);
						turtleStatisticsBuffer.addToSearchRequestsUpload(itemSerializedSize);
						peerConnectionManager.writeItem(peerConnection, itemToSend, this);
					},
					sender,
					this);
		}
	}

	@Override
	public int turtleSearch(String search, TurtleRsClient client) // XXX: put a size limit there in the search string length...
	{
		var id = SecureRandomUtils.nextInt();

		TurtleFileSearchRequestItem item;

		// "foobar" -> exact search
		if (search.startsWith("\"") && search.endsWith("\""))
		{
			search = search.substring(1, search.length() - 1);
			item = new TurtleStringSearchRequestItem(search);
		}
		else if (search.contains(" ")) // The Stuff -> search all terms, in this case "Stuff, The" is a match
		{
			var nameExpression = new NameExpression(StringExpression.Operator.CONTAINS_ALL, search, false);
			item = ExpressionMapper.toItem(List.of(nameExpression));
		}
		else // One word is just a string search
		{
			item = new TurtleStringSearchRequestItem(search);
		}

		item.setRequestId(id);

		var request = new SearchRequest(client, ownLocation, 0, search, 0, MAX_SEARCH_HITS);
		searchRequestsOrigins.put(id, request);

		var itemSerializedSize = ItemUtils.getItemSerializedSize(item, this);

		peerConnectionManager.doForAllPeers(peerConnection -> {
			var itemToSend = item.clone();
			turtleStatisticsBuffer.addToSearchRequestsUpload(itemSerializedSize);
			peerConnectionManager.writeItem(peerConnection, itemToSend, this);
		}, this);

		return id;
	}

	private List<TurtleSearchResultItem> performLocalSearch(TurtleSearchRequestItem item, int maxHits)
	{
		List<TurtleSearchResultItem> results = new ArrayList<>();

		if (item instanceof TurtleFileSearchRequestItem fileSearchItem)
		{
			log.debug("Received file search: {}, subclass: {}", fileSearchItem.getKeywords(), fileSearchItem.getClass().getSimpleName());
			return mapResults(searchFiles(fileSearchItem).stream()
					.filter(this::isSearchable)
					.limit(maxHits)
					.sorted(Comparator.comparing(File::getModified).reversed()) // Get the most recents first
					.map(file -> new TurtleFileInfo(file.getName(), file.getHash(), file.getSize()))
					.toList());
		}
		else if (item instanceof TurtleGenericSearchRequestItem genericSearchRequestItem)
		{
			log.debug("Received generic search: {}", genericSearchRequestItem.getKeywords());
			// XXX: generic search
		}
		return results;
	}

	private List<File> searchFiles(TurtleFileSearchRequestItem turtleFileSearchRequestItem)
	{
		return switch (turtleFileSearchRequestItem)
		{
			case TurtleStringSearchRequestItem item -> fileService.searchFiles(item.getKeywords());
			case TurtleRegExpSearchRequestItem item -> fileService.searchFiles(item.getExpressions());
			default -> throw new IllegalStateException("Unexpected value: " + turtleFileSearchRequestItem);
		};
	}

	private boolean isSearchable(File file)
	{
		if (file.getType() == FileType.DIRECTORY)
		{
			return false;
		}

		var share = fileService.findShareForFile(file).orElseThrow(() -> new IllegalStateException("File " + file + " is not in any share. Shouldn't happen."));
		return share.isSearchable();
	}

	private static List<TurtleSearchResultItem> mapResults(List<TurtleFileInfo> fileInfos)
	{
		List<TurtleSearchResultItem> results = new ArrayList<>();
		TurtleFileSearchResultItem item = null;
		var fileInfoSize = 0;

		for (TurtleFileInfo fileInfo : fileInfos)
		{
			if (item == null)
			{
				item = new TurtleFileSearchResultItem();
				results.add(item);
				fileInfoSize = 0;
			}

			item.addFileInfo(fileInfo);
			fileInfoSize += fileInfo.getSize();

			if (fileInfoSize > MAX_SEARCH_RESPONSE_SERIAL_SIZE)
			{
				item = null;
			}
		}
		return results;
	}

	private void handleSearchResult(PeerConnection sender, TurtleSearchResultItem item)
	{
		log.debug("Received search result from peer {}: {}", sender, item);

		if (item instanceof TurtleFileSearchResultItem turtleFileSearchResultItem)
		{
			// XXX: remove all the isBanned() files from the result set
		}

		var searchRequest = searchRequestsOrigins.get(item.getRequestId());
		if (searchRequest == null)
		{
			log.warn("Search result for request {} doesn't exist in the cache", item);
			return;
		}

		if (Duration.between(searchRequest.getLastUsed(), Instant.now()).compareTo(SEARCH_REQUEST_TIMEOUT) > 0) // XXX: put that timeout somewhere in the UI (like a spinning, or something that indicates the search is ongoing
		{
			log.debug("Search result arrived too late, dropping...");
			return;
		}

		if (searchRequest.getSource().equals(ownLocation))
		{
			log.debug("Search result is for us, forwarding to right service...");
			searchRequest.addResultCount(item.getCount());
			searchRequest.getClient().receiveSearchResult(item.getRequestId(), item);
			return;
		}

		if (searchRequest.isFull())
		{
			log.warn("Exceeded turtle search result to forward. Request {} already forwarded: {}, max allowed: {}, dropping item with {} elements...",
					item.getRequestId(),
					searchRequest.getResultCount(),
					searchRequest.getHitLimit(),
					item.getCount());
			return;
		}

		// Update the count and make sure we don't exceed the limit before forwarding
		searchRequest.addResultCount(item.getCount());
		if (searchRequest.isFull())
		{
			item.trim(searchRequest.getHitLimit());
		}

		// Forward the item to origin
		peerConnectionManager.writeItem(searchRequest.getSource(), item.clone(), this);
	}

	private static boolean isBanned(Sha1Sum hash)
	{
		return false; // TODO: implement
	}

	private void manageAll()
	{
		manageTunnels();
		computeTrafficInformation();
		cleanTunnelsIfNeeded();
		estimateSpeedIfNeeded();
	}

	private void manageTunnels()
	{
		var now = Instant.now();

		incomingHashes.entrySet().stream()
				.filter(entry -> {
					var hashInfo = entry.getValue();
					var totalSpeed = hashInfo.getTunnels().stream()
							.mapToDouble(tunnelId -> localTunnels.get(tunnelId).getSpeedBps())
							.sum();

					var tunnelKeepingFactor = (Math.max(1.0, totalSpeed / (50 * 1024)) - 1.0) + 1.0;

					return ((!hashInfo.hasTunnels() && Duration.between(hashInfo.getLastDiggTime(), now).compareTo(EMPTY_TUNNELS_DIGGING_TIME) > 0) ||
							(hashInfo.isAggressiveMode() && Duration.between(hashInfo.getLastDiggTime(), now).compareTo(Duration.ofSeconds((long) (REGULAR_TUNNELS_DIGGING_TIME.toSeconds() * tunnelKeepingFactor))) > 0));
				})
				.sorted(Comparator.comparing(entry -> entry.getValue().getLastDiggTime()))
				.map(Map.Entry::getKey)
				.findFirst()
				.ifPresent(this::diggTunnel);
	}

	private void computeTrafficInformation()
	{
		turtleStatistics = turtleStatistics.multiply(0.9f).add(turtleStatisticsBuffer.multiply(0.1f / TUNNEL_MANAGEMENT_DELAY.toSeconds()));
		turtleStatisticsBuffer.reset();
	}

	private void diggTunnel(Sha1Sum hash)
	{
		var requestId = SecureRandomUtils.nextInt();
		log.debug("Digging tunnel for hash {}, requestId: {}", hash, requestId);

		var hashInfo = incomingHashes.get(hash);

		hashInfo.setLastRequest(requestId);
		hashInfo.setLastDiggTime(Instant.now());

		var item = new TurtleTunnelRequestItem(hash, requestId, generatePersonalFilePrint(hash, tunnelProbability.getBias(), true));

		tunnelRequestsOrigins.put(item.getRequestId(), new TunnelRequest(ownLocation, item.getDepth()));

		peerConnectionManager.doForAllPeers(peerConnection -> {
					var itemToSend = item.clone();
					turtleStatisticsBuffer.addToTunnelRequestsUpload(SerializerSizeCache.getItemSize(itemToSend, this));
					peerConnectionManager.writeItem(peerConnection, itemToSend, this);
				},
				this);
	}

	private void cleanTunnelsIfNeeded()
	{
		var now = Instant.now();
		if (Duration.between(lastTunnelCleanup, now).compareTo(TUNNEL_CLEANING_TIME) <= 0)
		{
			return;
		}
		lastTunnelCleanup = now;

		var virtualPeersToRemove = new HashMap<TurtleRsClient, AbstractMap.SimpleEntry<Sha1Sum, Location>>();

		// Hashes marked for removal
		hashesToRemove.stream()
				.map(incomingHashes::remove)
				.filter(Objects::nonNull)
				.map(HashInfo::getTunnels)
				.forEach(tunnelId -> tunnelId.forEach(id -> closeTunnel(id, virtualPeersToRemove)));

		hashesToRemove.clear();

		// Search requests
		searchRequestsOrigins.entrySet().removeIf(entry ->
				Duration.between(now, entry.getValue().getLastUsed()).compareTo(SEARCH_REQUEST_LIFETIME) > 0);

		// Tunnel requests
		tunnelRequestsOrigins.entrySet().removeIf(entry ->
				Duration.between(now, entry.getValue().getLastUsed()).compareTo(TUNNEL_REQUEST_LIFETIME) > 0);

		// Tunnels
		localTunnels.entrySet().stream()
				.filter(entry -> Duration.between(now, entry.getValue().getLastUsed()).compareTo(MAX_TUNNEL_IDLE_TIME) > 0)
				.forEach(entry -> closeTunnel(entry.getKey(), virtualPeersToRemove));

		// Remove all the virtual peer ids from the clients
		virtualPeersToRemove.forEach((client, entry) -> client.removeVirtualPeer(entry.getKey(), entry.getValue()));
	}

	private void closeTunnel(int id, Map<TurtleRsClient, AbstractMap.SimpleEntry<Sha1Sum, Location>> sourcesToRemove)
	{
		log.debug("Closing tunnel {}", id);
		var tunnel = localTunnels.get(id);

		if (tunnel == null)
		{
			log.error("Cannot close tunnel {} because it doesn't exist", id);
			return;
		}

		if (tunnel.getSource().equals(ownLocation))
		{
			// This is a starting tunnel.

			// Remove the virtual peer from the virtual peers list
			virtualPeers.remove(tunnel.getVirtualLocation().getLocationId());

			// Remove the tunnel id from the file hash
			Optional.ofNullable(incomingHashes.get(tunnel.getHash())).ifPresent(hashInfo -> {
				hashInfo.removeTunnel(id);
				sourcesToRemove.put(hashInfo.getClient(), new AbstractMap.SimpleEntry<>(tunnel.getHash(), tunnel.getVirtualLocation()));
			});
		}
		else if (tunnel.getDestination().equals(ownLocation))
		{
			// This is an ending tunnel.

			var client = outgoingTunnelClients.remove(id);
			if (client != null)
			{
				sourcesToRemove.put(client, new AbstractMap.SimpleEntry<>(tunnel.getHash(), tunnel.getVirtualLocation()));

				// Remove associated virtual peers
				virtualPeers.entrySet().removeIf(entry -> entry.getValue().equals(id));
			}
		}
		localTunnels.remove(id);
	}

	private void estimateSpeedIfNeeded()
	{
		var now = Instant.now();
		if (Duration.between(lastSpeedEstimation, now).compareTo(SPEED_ESTIMATE_TIME) <= 0)
		{
			return;
		}
		lastSpeedEstimation = now;

		localTunnels.forEach((id, tunnel) -> {
			var speedEstimate = tunnel.getTransferredBytes() / (double) SPEED_ESTIMATE_TIME.toSeconds();
			tunnel.setSpeedBps(0.75 * tunnel.getSpeedBps() + 0.25 * speedEstimate);
			tunnel.clearTransferredBytes();
		});
	}

	public TurtleStatistics getStatistics()
	{
		return turtleStatistics.getStatistics();
	}
}
