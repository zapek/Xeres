/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.status;

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.status.item.StatusItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.xeres.app.xrs.service.RsServiceType.STATUS;
import static io.xeres.app.xrs.service.status.item.StatusItem.Status.ONLINE;

@Component
public class StatusRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(StatusRsService.class);

	public StatusRsService(Environment environment, PeerConnectionManager peerConnectionManager)
	{
		super(environment, peerConnectionManager);
	}

	@Override
	public RsServiceType getServiceType()
	{
		return STATUS;
	}

	@Override
	public Map<Class<? extends Item>, Integer> getSupportedItems()
	{
		return Map.of(StatusItem.class, 1);
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.NORMAL;
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		peerConnection.schedule(
				() -> writeItem(peerConnection, new StatusItem(ONLINE)),
				0,
				TimeUnit.SECONDS);
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		// XXX: print peer's status (ideally refresh a list)
		if (item instanceof StatusItem statusItem)
		{
			log.debug("Got status {} from peer {}", statusItem.getStatus(), sender);
		}
	}
}
