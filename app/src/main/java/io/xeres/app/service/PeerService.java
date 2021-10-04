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

package io.xeres.app.service;

import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.xeres.app.net.peer.bootstrap.PeerClient;
import io.xeres.app.net.peer.bootstrap.PeerServer;
import io.xeres.common.properties.StartupProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PeerService
{
	private static final Logger log = LoggerFactory.getLogger(PeerService.class);

	private static final int SSL_THREADS = 4; // XXX: is this ok? we probably don't need many since it's only for the first SSL handshake
	private static final int HANDLER_THREADS = 8; // XXX: ponder how much we should have for the most common setup

	private final PeerClient peerClient;
	private final PeerServer peerServer;

	private EventExecutorGroup sslExecutorGroup;
	private EventExecutorGroup handlerExecutorGroup;

	private final AtomicBoolean running = new AtomicBoolean();

	public PeerService(PeerClient peerClient, PeerServer peerServer)
	{
		this.peerClient = peerClient;
		this.peerServer = peerServer;
	}

	public void start()
	{
		running.lazySet(true);
		sslExecutorGroup = new DefaultEventExecutorGroup(SSL_THREADS);
		handlerExecutorGroup = new DefaultEventExecutorGroup(HANDLER_THREADS);

		peerServer.start(sslExecutorGroup, handlerExecutorGroup);
		if (!StartupProperties.getBoolean(StartupProperties.Property.SERVER_ONLY, false))
		{
			peerClient.start(sslExecutorGroup, handlerExecutorGroup);
		}
	}

	public void stop()
	{
		running.set(false);
		peerServer.stop();
		peerClient.stop();
		try
		{
			if (sslExecutorGroup != null)
			{
				sslExecutorGroup.shutdownGracefully().sync();
			}

			if (handlerExecutorGroup != null)
			{
				handlerExecutorGroup.shutdownGracefully().sync();
			}
		}
		catch (InterruptedException e)
		{
			log.error("Error while shutting down executor group: {}", e.getMessage());
			Thread.currentThread().interrupt();
		}
	}

	public boolean isRunning()
	{
		return running.get();
	}
}
