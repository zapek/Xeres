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
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.resolver.AddressResolverGroup;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.properties.NetworkProperties;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.SettingsService;
import io.xeres.app.xrs.service.serviceinfo.ServiceInfoRsService;
import io.xeres.common.properties.StartupProperties;
import io.xeres.ui.support.tray.TrayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

import static io.xeres.common.properties.StartupProperties.Property.FAST_SHUTDOWN;

abstract class PeerClient
{
	@SuppressWarnings("NonConstantLogger")
	protected final Logger log = LoggerFactory.getLogger(getClass().getName());

	protected final SettingsService settingsService;
	protected final NetworkProperties networkProperties;
	protected final LocationService locationService;
	protected final PeerConnectionManager peerConnectionManager;
	protected final DatabaseSessionManager databaseSessionManager;
	protected final ServiceInfoRsService serviceInfoRsService;
	protected final TrayService trayService;

	private Bootstrap bootstrap;
	private EventLoopGroup group;

	public abstract PeerInitializer getPeerInitializer();

	public abstract AddressResolverGroup<? extends SocketAddress> getAddressResolverGroup();

	protected PeerClient(SettingsService settingsService, NetworkProperties networkProperties, LocationService locationService, PeerConnectionManager peerConnectionManager, DatabaseSessionManager databaseSessionManager, ServiceInfoRsService serviceInfoRsService, TrayService trayService)
	{
		this.settingsService = settingsService;
		this.networkProperties = networkProperties;
		this.locationService = locationService;
		this.peerConnectionManager = peerConnectionManager;
		this.databaseSessionManager = databaseSessionManager;
		this.serviceInfoRsService = serviceInfoRsService;
		this.trayService = trayService;
	}

	public void start()
	{
		log.info("Starting peer client...");
		group = new NioEventLoopGroup();

		bootstrap = new Bootstrap();
		setAddressResolver();
		bootstrap.group(group)
				.channel(NioSocketChannel.class)
				.handler(getPeerInitializer());
	}

	private void setAddressResolver()
	{
		var addressResolverGroup = getAddressResolverGroup();
		if (addressResolverGroup != null)
		{
			bootstrap.resolver(addressResolverGroup);
		}
	}

	public void stop()
	{
		if (group == null)
		{
			return;
		}

		if (StartupProperties.getBoolean(FAST_SHUTDOWN, false))
		{
			log.debug("Shutting down peer client (fast)...");
			group.shutdownGracefully();
		}
		else
		{
			log.info("Shutting down peer client...");
			try
			{
				group.shutdownGracefully().sync();
			}
			catch (InterruptedException e)
			{
				log.error("Error while shutting down peer client: {}", e.getMessage());
				Thread.currentThread().interrupt();
			}
		}
	}

	public void connect(PeerAddress peerAddress)
	{
		if (group != null)
		{
			bootstrap.connect(peerAddress.getSocketAddress());
		}
	}
}
