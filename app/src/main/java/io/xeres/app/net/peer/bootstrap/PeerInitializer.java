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

package io.xeres.app.net.peer.bootstrap;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.net.peer.ConnectionType;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.net.peer.pipeline.*;
import io.xeres.app.net.peer.ssl.SSL;
import io.xeres.app.properties.NetworkProperties;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.SettingsService;
import io.xeres.app.xrs.service.serviceinfo.ServiceInfoRsService;
import io.xeres.ui.support.tray.TrayService;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;

import static io.xeres.app.net.peer.ConnectionType.I2P_OUTGOING;
import static io.xeres.app.net.peer.ConnectionType.TOR_OUTGOING;

public class PeerInitializer extends ChannelInitializer<SocketChannel>
{
	public static final Duration PEER_IDLE_TIMEOUT = Duration.ofMinutes(2); /* peers not responding during that time are considered dead */
	public static final Duration ACTIVITY_PROD = Duration.ofMinutes(1); /* if idle, sends a prod activity after that time */

	private final SslContext sslContext;
	private final ConnectionType connectionType;
	private final SettingsService settingsService;
	private final NetworkProperties networkProperties;
	private final LocationService locationService;
	private final PeerConnectionManager peerConnectionManager;
	private final DatabaseSessionManager databaseSessionManager;
	private final ServiceInfoRsService serviceInfoRsService;
	private final TrayService trayService;

	private static final ChannelHandler SIMPLE_PACKET_ENCODER = new SimplePacketEncoder();
	private static final ChannelHandler ITEM_ENCODER = new ItemEncoder();
	private static final ChannelHandler IDLE_EVENT_HANDLER = new IdleEventHandler(PEER_IDLE_TIMEOUT);

	public PeerInitializer(PeerConnectionManager peerConnectionManager, DatabaseSessionManager databaseSessionManager, LocationService locationService, SettingsService settingsService, NetworkProperties networkProperties, ServiceInfoRsService serviceInfoRsService, ConnectionType connectionType, TrayService trayService)
	{
		this.settingsService = settingsService;
		try
		{
			this.sslContext = SSL.createSslContext(settingsService.getLocationPrivateKeyData(), settingsService.getLocationCertificate(), connectionType);
		}
		catch (SSLException | NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			throw new IllegalStateException("Error setting up PeerClient: " + e.getMessage(), e);
		}
		this.networkProperties = networkProperties;
		this.serviceInfoRsService = serviceInfoRsService;
		this.trayService = trayService;
		this.locationService = locationService;
		this.peerConnectionManager = peerConnectionManager;
		this.databaseSessionManager = databaseSessionManager;
		this.connectionType = connectionType;
	}

	@Override
	protected void initChannel(SocketChannel channel)
	{
		var pipeline = channel.pipeline();

		// Build the pipeline in order.
		// Inbound
		// vvvvvvv

		// add SOCKS5 connection if Tor or I2P
		if (connectionType == TOR_OUTGOING && settingsService.hasTorSocksConfigured())
		{
			var hostPort = settingsService.getTorSocksHostPort();
			pipeline.addLast(new Socks5ProxyHandler(new InetSocketAddress(hostPort.host(), hostPort.port())));
		}
		else if (connectionType == I2P_OUTGOING && settingsService.hasI2pSocksConfigured())
		{
			var hostPort = settingsService.getI2pSocksHostPort();
			pipeline.addLast(new Socks5ProxyHandler(new InetSocketAddress(hostPort.host(), hostPort.port())));
		}

		// add SSL to encrypt and decrypt everything
		pipeline.addLast(sslContext.newHandler(channel.alloc()));

		// decoder (inbound)
		pipeline.addLast(new PacketDecoder());
		pipeline.addLast(new ItemDecoder());

		// encoder (outbound)
		pipeline.addLast(networkProperties.isPacketSlicing() ? new MultiPacketEncoder() : SIMPLE_PACKET_ENCODER);
		pipeline.addLast(ITEM_ENCODER);

		// business logic
		pipeline.addLast(new IdleStateHandler((int) PEER_IDLE_TIMEOUT.toSeconds(), (int) ACTIVITY_PROD.toSeconds(), 0));
		pipeline.addLast(IDLE_EVENT_HANDLER);

		// ^^^^^^^^
		// Outbound

		pipeline.addLast(new PeerHandler(locationService, peerConnectionManager, databaseSessionManager, serviceInfoRsService, connectionType, trayService));
	}
}
