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
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.net.peer.ConnectionDirection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.net.peer.pipeline.*;
import io.xeres.app.net.peer.ssl.SSL;
import io.xeres.app.properties.NetworkProperties;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.PrefsService;
import io.xeres.app.xrs.service.serviceinfo.ServiceInfoService;

import javax.net.ssl.SSLException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;

public class PeerInitializer extends ChannelInitializer<SocketChannel>
{
	public static final Duration PEER_IDLE_TIMEOUT = Duration.ofMinutes(2); /* peers not responding during that time are considered dead */
	public static final Duration ACTIVITY_PROD = Duration.ofMinutes(1); /* if idle, sends a prod activity after that time */

	private final SslContext sslContext;
	private final ConnectionDirection direction;

	private final EventExecutorGroup sslExecutorGroup;
	private final EventExecutorGroup eventExecutorGroup;

	private final NetworkProperties networkProperties;
	private final LocationService locationService;
	private final PeerConnectionManager peerConnectionManager;
	private final DatabaseSessionManager databaseSessionManager;
	private final ServiceInfoService serviceInfoService;

	private static final ChannelHandler SIMPLE_PACKET_ENCODER = new SimplePacketEncoder();
	private static final ChannelHandler ITEM_ENCODER = new ItemEncoder();
	private static final ChannelHandler IDLE_EVENT_HANDLER = new IdleEventHandler(PEER_IDLE_TIMEOUT);

	public PeerInitializer(PeerConnectionManager peerConnectionManager, DatabaseSessionManager databaseSessionManager, LocationService locationService, PrefsService prefsService, EventExecutorGroup sslExecutorGroup, EventExecutorGroup eventExecutorGroup, NetworkProperties networkProperties, ServiceInfoService serviceInfoService, ConnectionDirection direction) throws InvalidKeySpecException, NoSuchAlgorithmException, SSLException
	{
		this.serviceInfoService = serviceInfoService;
		this.sslContext = SSL.createSslContext(prefsService.getLocationPrivateKeyData(), prefsService.getLocationCertificate(), direction);
		this.sslExecutorGroup = sslExecutorGroup;
		this.eventExecutorGroup = eventExecutorGroup;
		this.networkProperties = networkProperties;
		this.locationService = locationService;
		this.peerConnectionManager = peerConnectionManager;
		this.databaseSessionManager = databaseSessionManager;
		this.direction = direction;
	}

	@Override
	protected void initChannel(SocketChannel channel)
	{
		ChannelPipeline pipeline = channel.pipeline();

		// Build the pipeline in order.
		// Inbound
		// vvvvvvv

		// add SSL to encrypt and decrypt everything
		pipeline.addLast(sslExecutorGroup, sslContext.newHandler(channel.alloc()));

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

		pipeline.addLast(eventExecutorGroup, new PeerHandler(locationService, peerConnectionManager, databaseSessionManager, serviceInfoService, direction));
	}
}
