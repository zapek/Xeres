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

	protected TurtleRsService(Environment environment, PeerConnectionManager peerConnectionManager)
	{
		super(environment, peerConnectionManager);
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
				TurtleTunnelOpenItem.class, 3,
				TurtleTunnelOkItem.class, 4,
				TurtleRegExpSearchRequestItem.class, 9,
				TurtleGenericSearchRequestItem.class, 11,
				TurtleGenericSearchResultItem.class, 12
		);
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		if (item instanceof TurtleTunnelOpenItem turtleTunnelOpenItem)
		{
			log.debug("Got tunnel open item {} from peer {}", turtleTunnelOpenItem, sender);
		}
		else if (item instanceof TurtleTunnelOkItem turtleTunnelOkItem)
		{
			log.debug("Got tunnel OK item {} from peer {}", turtleTunnelOkItem, sender);
		}
		else if (item instanceof TurtleStringSearchRequestItem turtleStringSearchRequestItem)
		{
			log.debug("Got search request item {} from peer {}", turtleStringSearchRequestItem, sender);
			// XXX
		}
		else if (item instanceof TurtleRegExpSearchRequestItem turtleRegExpSearchRequestItem)
		{
			log.debug("Got regexp search request item {} from peer {}", turtleRegExpSearchRequestItem, sender);
		}
		else if (item instanceof TurtleGenericSearchRequestItem turtleGenericSearchRequestItem)
		{
			log.debug("Got generic search request item {} from peer {}", turtleGenericSearchRequestItem, sender);
		}
		else if (item instanceof TurtleGenericSearchResultItem turtleGenericSearchResultItem)
		{
			log.debug("Got generic search result item {} from peer {}", turtleGenericSearchResultItem, sender);
		}
	}
}
