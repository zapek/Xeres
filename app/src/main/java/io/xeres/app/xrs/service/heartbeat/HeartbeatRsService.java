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

package io.xeres.app.xrs.service.heartbeat;

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.heartbeat.item.HeartbeatItem;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static io.xeres.app.xrs.service.RsServiceType.HEARTBEAT;

@Component
public class HeartbeatRsService extends RsService
{
	private final PeerConnectionManager peerConnectionManager;

	public HeartbeatRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager)
	{
		super(rsServiceRegistry);
		this.peerConnectionManager = peerConnectionManager;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return HEARTBEAT;
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.NORMAL;
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		peerConnection.scheduleAtFixedRate(() -> peerConnectionManager.writeItem(peerConnection, new HeartbeatItem(), this),
				5,
				5,
				TimeUnit.SECONDS);
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		// do nothing
	}
}
