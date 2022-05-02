/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.turtle.item.*;
import io.xeres.common.id.Sha1Sum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.xeres.app.xrs.service.RsServiceType.TURTLE;

@Component
public class TurtleRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(TurtleRsService.class);

	public static final int MAX_TUNNEL_DEPTH = 6;

	private final SearchRequestCache searchRequestCache = new SearchRequestCache();

	private final TunnelRequestCache tunnelRequestCache = new TunnelRequestCache();

	private final TunnelProbability tunnelProbability = new TunnelProbability();

	private final PeerConnectionManager peerConnectionManager;

	protected TurtleRsService(Environment environment, PeerConnectionManager peerConnectionManager)
	{
		super(environment);
		this.peerConnectionManager = peerConnectionManager;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return TURTLE;
	}

	@Override
	public Map<Class<? extends Item>, Integer> getSupportedItems()
	{
		return Map.of(
				TurtleStringSearchRequestItem.class, 1,
				TurtleTunnelRequestItem.class, 3,
				TurtleTunnelResultItem.class, 4,
				TurtleRegExpSearchRequestItem.class, 9,
				TurtleGenericSearchRequestItem.class, 11,
				TurtleGenericSearchResultItem.class, 12
		);
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
			log.debug("Got turtle search result item {} from peer {}", turtleSearchResultItem, sender);
		}
	}

	private void handleTunnelRequest(PeerConnection sender, TurtleTunnelRequestItem item)
	{
		log.debug("Received tunnel request from peer {}: {}", sender, item);

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

		// XXX: if it's not for us, perform a local search and send result back if found (otherwise forward)

		if (tunnelProbability.isForwardable(item)) // XXX: this is different there! needs the number of peers and speed...
		{
			peerConnectionManager.doForAllPeers(peerConnection -> {
						var itemToSend = item.clone();
						tunnelProbability.incrementDepth(itemToSend);
						peerConnectionManager.writeItem(peerConnection, itemToSend, this);
					},
					sender,
					this);
		}
	}

	private void handleSearchRequest(PeerConnection sender, TurtleSearchRequestItem item)
	{
		log.debug("Received search request from peer {}: {}", sender, item);

		// XXX: check maximum size

		// XXX: check maximum search request in cache

		if (searchRequestCache.exists(item.getRequestId()))
		{
			log.debug("Request {} already in cache", item.getRequestId());
			return;
		}

		// XXX: forward if not for us, etc...

		if (tunnelProbability.isForwardable(item))
		{
			peerConnectionManager.doForAllPeers(peerConnection -> {
						var itemToSend = item.clone();
						tunnelProbability.incrementDepth(itemToSend);
						peerConnectionManager.writeItem(peerConnection, itemToSend, this);
					},
					sender,
					this);
		}
	}

	private boolean isBanned(Sha1Sum fileHash)
	{
		return false; // TODO: implement
	}
}
