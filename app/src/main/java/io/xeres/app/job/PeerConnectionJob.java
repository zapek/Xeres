/*
 * Copyright (c) 2019-2020 by David Gerber - https://zapek.com
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

package io.xeres.app.job;

import io.xeres.app.XeresApplication;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.net.peer.bootstrap.PeerClient;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.service.LocationService;
import io.xeres.common.properties.StartupProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PeerConnectionJob
{
	private static final Logger log = LoggerFactory.getLogger(PeerConnectionJob.class);

	private final LocationService locationService;
	private final PeerClient peerClient;

	public PeerConnectionJob(LocationService locationService, PeerClient peerClient)
	{
		this.locationService = locationService;
		this.peerClient = peerClient;
	}

	@Scheduled(initialDelay = 5 * 1000, fixedDelay = 60 * 1000)
	protected void checkConnections()
	{
		// XXX: don't try to connect when we're shutting down... I wonder if this is a spring boot bug... maybe ask them?
		if (!StartupProperties.getBoolean(StartupProperties.Property.SERVER_ONLY, false) && !XeresApplication.isRemoteUiClient())
		{
			connectToPeers();
		}
	}

	private void connectToPeers()
	{
		// XXX: check if the network is UP before attempting
		List<Connection> connections = locationService.getConnectionsToConnectTo();

		for (Connection connection : connections)
		{
			log.debug("Attempting to connect to {} ...", connection.getAddress());
			var peerAddress = PeerAddress.fromAddress(connection.getAddress());
			if (peerAddress.isValid())
			{
				peerClient.connect(peerAddress);
			}
			else
			{
				log.error("Automatic connection: invalid address for {}", connection.getAddress());
			}
		}
	}
}
