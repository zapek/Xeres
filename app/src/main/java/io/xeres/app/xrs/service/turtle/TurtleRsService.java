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

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceMaster;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.app.xrs.service.turtle.item.*;
import io.xeres.common.id.Sha1Sum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static io.xeres.app.xrs.service.RsServiceType.TURTLE;

@Component
public class TurtleRsService extends RsService implements RsServiceMaster<TurtleRsClient>
{
	private static final Logger log = LoggerFactory.getLogger(TurtleRsService.class);

	public static final int MAX_TUNNEL_DEPTH = 6;

	private static final int MAX_SEARCH_REQUEST_IN_CACHE = 120;

	private static final int MAX_SEARCH_HITS = 100;

	private static final Duration SEARCH_REQUEST_TIMEOUT = Duration.ofSeconds(20);

	private final SearchRequestCache searchRequestCache = new SearchRequestCache(MAX_SEARCH_REQUEST_IN_CACHE);

	private final TunnelRequestCache tunnelRequestCache = new TunnelRequestCache();

	private final TunnelProbability tunnelProbability = new TunnelProbability();

	private final PeerConnectionManager peerConnectionManager;

	private final List<TurtleRsClient> turtleClients = new ArrayList<>();

	private final IdentityRsService identityRsService;

	private IdentityGroupItem ownIdentity;

	protected TurtleRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, IdentityRsService identityRsService)
	{
		super(rsServiceRegistry);
		this.peerConnectionManager = peerConnectionManager;
		this.identityRsService = identityRsService;
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
		ownIdentity = identityRsService.getOwnIdentity();
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
			log.debug("Got tunnel OK item {} from peer {}", turtleTunnelResultItem, sender);
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

		if (item.getFileHash() == null) // XXX: not sure what to do with those
		{
			log.debug("null filehash, dropping...");
			return;
		}

		if (isBanned(item.getFileHash()))
		{
			log.debug("Rejecting banned file hash {}", item.getFileHash());
			return;
		}

		// XXX: calculate forwarding probability
		if (tunnelRequestCache.exists(item.getRequestId(), () -> new TunnelRequest(sender.getLocation().getLocationId(), item.getDepth())))
		{
			log.debug("Requests {} already exists", item.getRequestId());
			return;
		}

		var client = turtleClients.stream()
				.filter(turtleRsClient -> turtleRsClient.handleTunnelRequest(sender, item.getFileHash()))
				.findFirst();

		if (client.isPresent())
		{
			var resultItem = new TurtleTunnelResultItem(item.getRequestId(), generateTunnelId(item, tunnelProbability.getBias(), false));
			peerConnectionManager.writeItem(sender, resultItem, this);
			// XXX: store the opened tunnel somewhere! also we added a distant peer, etc...
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

	int generateTunnelId(TurtleTunnelRequestItem item, int bias, boolean symetrical)
	{
		var buf = item.getFileHash().toString() + ownIdentity.getGxsId().toString();
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
		return item.getPartialTunnelId() ^ result;
	}

	private void handleSearchRequest(PeerConnection sender, TurtleSearchRequestItem item)
	{
		log.debug("Received search request from peer {}: {}", sender, item);

		// XXX: check maximum size

		if (searchRequestCache.isFull())
		{
			log.debug("Request cache is full. Check if a peer is flooding.");
			return;
		}

		// XXX: perform local search

		if (searchRequestCache.exists(item.getRequestId(),
				() -> new SearchRequest(sender.getLocation(),
						item.getDepth(),
						item.getKeywords(),
						0,
						MAX_SEARCH_HITS)))
		{
			log.debug("Request {} already in cache", item.getRequestId());
			return;
		}

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

		var searchRequest = searchRequestCache.get(item.getRequestId());
		if (searchRequest == null)
		{
			log.error("Search result for request {} doesn't exist in the cache", item);
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
}
