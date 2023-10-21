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

package io.xeres.app.service;

import io.xeres.app.net.peer.bootstrap.PeerI2pClient;
import io.xeres.app.net.peer.bootstrap.PeerTcpClient;
import io.xeres.app.net.peer.bootstrap.PeerTcpServer;
import io.xeres.app.net.peer.bootstrap.PeerTorClient;
import io.xeres.common.properties.StartupProperties;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.xeres.common.properties.StartupProperties.Property.SERVER_ADDRESS;
import static io.xeres.common.properties.StartupProperties.Property.SERVER_ONLY;

@Service
public class PeerService
{
	private final PeerTcpClient peerTcpClient;
	private final PeerTorClient peerTorClient;
	private final PeerI2pClient peerI2pClient;
	private final PeerTcpServer peerTcpServer;
	private final SettingsService settingsService;

	private final AtomicBoolean running = new AtomicBoolean();

	public PeerService(PeerTcpClient peerTcpClient, PeerTorClient peerTorClient, PeerI2pClient peerI2pClient, PeerTcpServer peerTcpServer, SettingsService settingsService)
	{
		this.peerTcpClient = peerTcpClient;
		this.peerTorClient = peerTorClient;
		this.peerI2pClient = peerI2pClient;
		this.peerTcpServer = peerTcpServer;
		this.settingsService = settingsService;
	}

	public void start(int localPort)
	{
		running.lazySet(true);

		peerTcpServer.start(StartupProperties.getString(SERVER_ADDRESS), localPort);
		if (!StartupProperties.getBoolean(SERVER_ONLY, false))
		{
			peerTcpClient.start();
		}
		startTor();
		startI2p();
	}

	public void stop()
	{
		running.set(false);
		peerTcpServer.stop();
		peerTcpClient.stop();
		peerTorClient.stop();
		peerI2pClient.stop();
	}

	public void startTor()
	{
		if (settingsService.hasTorSocksConfigured())
		{
			peerTorClient.start();
		}
	}

	public void stopTor()
	{
		peerTorClient.stop();
	}

	public void restartTor()
	{
		stopTor();
		startTor();
	}

	public void startI2p()
	{
		if (settingsService.hasI2pSocksConfigured())
		{
			peerI2pClient.start();
		}
	}

	public void stopI2p()
	{
		peerI2pClient.stop();
	}

	public void restartI2p()
	{
		stopI2p();
		startI2p();
	}

	public boolean isRunning()
	{
		return running.get();
	}
}
