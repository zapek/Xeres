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

package io.xeres.app.job;

import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.bootstrap.PeerI2pClient;
import io.xeres.app.net.peer.bootstrap.PeerTcpClient;
import io.xeres.app.net.peer.bootstrap.PeerTorClient;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.PeerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * Handles automatic outgoing connections to peers.
 */
@Component
public class PeerConnectionJob
{
	private static final Logger log = LoggerFactory.getLogger(PeerConnectionJob.class);

	private static final int SIMULTANEOUS_CONNECTIONS = 10; // number of locations to connect at once

	private final LocationService locationService;
	private final PeerTcpClient peerTcpClient;
	private final PeerTorClient peerTorClient;
	private final PeerI2pClient peerI2pClient;
	private final PeerService peerService;

	public PeerConnectionJob(LocationService locationService, PeerTcpClient peerTcpClient, PeerTorClient peerTorClient, PeerI2pClient peerI2pClient, PeerService peerService)
	{
		this.locationService = locationService;
		this.peerTcpClient = peerTcpClient;
		this.peerTorClient = peerTorClient;
		this.peerI2pClient = peerI2pClient;
		this.peerService = peerService;
	}

	@Scheduled(initialDelay = 5, fixedDelay = 60, timeUnit = TimeUnit.SECONDS)
	void checkConnections()
	{
		connectToPeers();
	}

	private void connectToPeers()
	{
		if (!JobUtils.canRun(peerService))
		{
			return;
		}
		synchronized (PeerConnectionJob.class)
		{
			var connections = locationService.getConnectionsToConnectTo(SIMULTANEOUS_CONNECTIONS);

			for (var connection : connections)
			{
				connect(connection);
			}
		}
	}

	public void connectImmediately(Location location, int connectionIndex)
	{
		if (!JobUtils.canRun(peerService))
		{
			return;
		}
		synchronized (PeerConnectionJob.class)
		{
			var connections = location.getConnections().stream()
					.sorted(Comparator.comparing(Connection::isExternal).reversed())
					.toList();

			connect(connections.get(connectionIndex));
		}
	}

	private void connect(Connection connection)
	{
		log.debug("Attempting to connect to {} ...", connection.getAddress());
		var peerAddress = PeerAddress.fromAddress(connection.getAddress());
		if (peerAddress.isValid())
		{
			if (peerAddress.isHidden())
			{
				switch (peerAddress.getType())
				{
					case TOR -> peerTorClient.connect(peerAddress);
					case I2P -> peerI2pClient.connect(peerAddress);
					default -> throw new IllegalArgumentException("Wrong type " + peerAddress.getType() + " for hidden address");
				}
			}
			else
			{
				peerTcpClient.connect(peerAddress);
			}
		}
		else
		{
			log.error("Automatic connection: invalid address for {}", connection.getAddress());
		}
	}
}
