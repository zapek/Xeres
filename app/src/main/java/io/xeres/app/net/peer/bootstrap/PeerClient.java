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

package io.xeres.app.net.peer.bootstrap;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.properties.NetworkProperties;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.PrefsService;
import io.xeres.app.xrs.service.serviceinfo.ServiceInfoService;
import io.xeres.common.properties.StartupProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

import static io.xeres.app.net.peer.ConnectionDirection.OUTGOING;

@Component
public class PeerClient
{
	private static final Logger log = LoggerFactory.getLogger(PeerClient.class);

	private final PrefsService prefsService;
	private final NetworkProperties networkProperties;
	private final LocationService locationService;
	private final PeerConnectionManager peerConnectionManager;
	private final DatabaseSessionManager databaseSessionManager;
	private final ServiceInfoService serviceInfoService;

	private Bootstrap bootstrap;
	private EventLoopGroup group;

	public PeerClient(PrefsService prefsService, NetworkProperties networkProperties, LocationService locationService, PeerConnectionManager peerConnectionManager, DatabaseSessionManager databaseSessionManager, ServiceInfoService serviceInfoService)
	{
		this.prefsService = prefsService;
		this.networkProperties = networkProperties;
		this.locationService = locationService;
		this.peerConnectionManager = peerConnectionManager;
		this.databaseSessionManager = databaseSessionManager;
		this.serviceInfoService = serviceInfoService;
	}

	public void start(EventExecutorGroup sslExecutorGroup, EventExecutorGroup eventExecutorGroup)
	{
		group = new NioEventLoopGroup();

		try
		{
			bootstrap = new Bootstrap();
			bootstrap.group(group)
					.channel(NioSocketChannel.class)
					.handler(new PeerInitializer(peerConnectionManager, databaseSessionManager, locationService, prefsService, sslExecutorGroup, eventExecutorGroup, networkProperties, serviceInfoService, OUTGOING));
		}
		catch (SSLException | NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			log.error("Error setting up PeerClient: {}", e.getMessage());
		}
	}

	public void stop()
	{
		if (group == null)
		{
			return;
		}

		if (StartupProperties.getBoolean(StartupProperties.Property.FAST_SHUTDOWN, false))
		{
			log.debug("Shutting down netty client (fast)...");
			group.shutdownGracefully();
		}
		else
		{
			log.info("Shutting down netty client...");
			try
			{
				group.shutdownGracefully().sync();
			}
			catch (InterruptedException e)
			{
				log.error("Error while shutting down netty client: {}", e.getMessage());
				Thread.currentThread().interrupt();
			}
		}
	}

	public ChannelFuture connect(PeerAddress peerAddress)
	{
		Objects.requireNonNull(group);
		return bootstrap.connect(peerAddress.getSocketAddress());
	}
}
