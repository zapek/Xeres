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

package io.xeres.app.net.bdisc;

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This service periodically sends an UDP broadcast packet to find out
 * if other Retroshare clients are on the LAN. It implements more or
 * less the same protocol as found in https://github.com/truvorskameikin/udp-discovery-cpp
 * (which is what Retroshare uses).
 */
@Service
public class BroadcastDiscoveryService implements Runnable
{
	private static final Logger log = LoggerFactory.getLogger(BroadcastDiscoveryService.class);

	private static final int PORT = 36405;
	private static final int APP_ID = 904571;
	private static final int BROADCAST_BUFFER_SEND_SIZE_MAX = 512;
	private static final int BROADCAST_BUFFER_RECV_SIZE = 512;

	private static final Duration BROADCAST_MAX_WAIT_TIME = Duration.ofSeconds(5);

	private enum State
	{
		BROADCASTING,
		WAITING,
		INTERRUPTED
	}

	private final DatabaseSessionManager databaseSessionManager;
	private final LocationService locationService;

	private InetSocketAddress localAddress;
	private InetSocketAddress sendAddress;
	private Thread thread;

	private SocketAddress broadcastAddress;
	private ByteBuffer sendBuffer;
	private ByteBuffer receiveBuffer;
	private State state;
	private Instant sent = Instant.EPOCH;
	private int ownPeerId;
	private final int counter = 1;
	private final Map<Integer, UdpDiscoveryPeer> peers = new HashMap<>();

	public BroadcastDiscoveryService(DatabaseSessionManager databaseSessionManager, LocationService locationService)
	{
		this.databaseSessionManager = databaseSessionManager;
		this.locationService = locationService;
	}

	public void start(String localIpAddress, int localPort)
	{
		log.info("Starting Broadcast Discovery...");
		this.localAddress = new InetSocketAddress(localIpAddress, localPort);
		thread = new Thread(this, "Broadcast Discovery Service");
		thread.start();
	}

	public void stop()
	{
		if (thread != null)
		{
			log.info("Stopping Broadcast Discovery...");
			thread.interrupt();
		}
	}

	public boolean isRunning()
	{
		return thread.isAlive();
	}

	public void waitForTermination()
	{
		if (thread != null)
		{
			try
			{
				log.info("Waiting for Broadcast Discovery to terminate...");
				thread.join();
			}
			catch (InterruptedException e)
			{
				log.error("Failed to wait for termination: {}", e.getMessage(), e);
				Thread.currentThread().interrupt();
			}
		}
	}

	private String getBroadcastAddress(String ipAddress)
	{
		List<InetAddress> broadcastList = new ArrayList<>();

		Iterator<NetworkInterface> interfaces;
		try
		{
			interfaces = NetworkInterface.getNetworkInterfaces().asIterator();
			while (interfaces.hasNext())
			{
				var networkInterface = interfaces.next();
				if (networkInterface.isUp() && !networkInterface.isLoopback())
				{
					networkInterface.getInterfaceAddresses().stream()
							.filter(interfaceAddress -> interfaceAddress.getAddress().getHostAddress().equals(ipAddress))
							.map(InterfaceAddress::getBroadcast)
							.filter(Objects::nonNull)
							.forEach(broadcastList::add);
				}
			}
		}
		catch (SocketException e)
		{
			throw new IllegalStateException("Couldn't find broadcast address: " + e.getMessage(), e);
		}
		return broadcastList.stream().findFirst().orElseThrow(() -> new IllegalStateException("No broadcast address found")).getHostAddress();
	}

	private void setupOwnInfo()
	{
		try (var session = new DatabaseSession(databaseSessionManager))
		{
			var ownLocation = locationService.findOwnLocation().orElseThrow();

			ownPeerId = ThreadLocalRandom.current().nextInt();
			sendBuffer = UdpDiscoveryProtocol.createPacket(
					BROADCAST_BUFFER_SEND_SIZE_MAX,
					UdpDiscoveryPeer.Status.PRESENT,
					APP_ID,
					ownPeerId,
					counter,
					ownLocation.getProfile().getProfileFingerprint(),
					ownLocation.getLocationId(),
					localAddress.getPort(),
					ownLocation.getProfile().getName());
			sendBuffer.flip();
		}
	}

	private void updateOwnInfo()
	{
		// For now we do nothing but we could implement something better if for
		// example there's a change of IP or port. Don't forget to increase the
		// counter for each update otherwise it won't be taken into account.
	}

	@Override
	public void run()
	{
		broadcastAddress = new InetSocketAddress(getBroadcastAddress(localAddress.getHostString()), PORT);
		receiveBuffer = ByteBuffer.allocate(BROADCAST_BUFFER_RECV_SIZE);

		setupOwnInfo();

		try (var selector = Selector.open();
		     DatagramChannel receiveChannel = DatagramChannel.open(StandardProtocolFamily.INET)
				     .setOption(StandardSocketOptions.SO_REUSEADDR, true)
				     .bind(new InetSocketAddress(localAddress.getHostString(), PORT));
		     DatagramChannel sendChannel = DatagramChannel.open(StandardProtocolFamily.INET)
				     .setOption(StandardSocketOptions.SO_BROADCAST, true)
				     .bind(new InetSocketAddress(localAddress.getHostString(), 0))
		)
		{
			sendAddress = (InetSocketAddress) sendChannel.getLocalAddress();
			receiveChannel.configureBlocking(false);
			receiveChannel.register(selector, SelectionKey.OP_READ);
			state = State.BROADCASTING;

			while (true)
			{
				if (state == State.BROADCASTING)
				{
					updateOwnInfo();
					sendBroadcast(sendChannel);
				}

				selector.select(getSelectorTimeout());
				if (Thread.interrupted())
				{
					setState(State.INTERRUPTED);
					break;
				}
				handleSelection(selector);
			}
		}
		catch (ClosedByInterruptException e)
		{
			log.debug("Interrupted, bailing out...");
		}
		catch (IOException e)
		{
			log.error("Error: ", e);
		}
	}

	private void handleSelection(Selector selector)
	{
		Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();

		if (!selectedKeys.hasNext())
		{
			// This was a timeout
			setState(State.BROADCASTING);
			return;
		}

		while (selectedKeys.hasNext())
		{
			try
			{
				SelectionKey key = selectedKeys.next();
				selectedKeys.remove();

				if (!key.isValid())
				{
					continue;
				}

				if (key.isReadable())
				{
					read(key);
				}
			}
			catch (IOException e)
			{
				log.error("Glitch, continuing...", e); // XXX: I think I should keep that part in case there's a transient network error. need experimenting
			}
		}

		// We're past the timeout so send again
		if (Duration.between(sent, Instant.now()).compareTo(BROADCAST_MAX_WAIT_TIME) > 0)
		{
			setState(State.BROADCASTING);
		}
	}

	private long getSelectorTimeout()
	{
		return switch (state)
				{
					case WAITING -> Math.max(BROADCAST_MAX_WAIT_TIME.toMillis() - Duration.between(sent, Instant.now()).toMillis(), 0L);
					default -> 0L;
				};
	}

	private void setState(State newState)
	{
		state = newState;
	}

	private void read(SelectionKey key) throws IOException
	{
		assert state == State.WAITING;

		DatagramChannel channel = (DatagramChannel) key.channel();
		InetSocketAddress peerAddress = (InetSocketAddress) channel.receive(receiveBuffer);
		receiveBuffer.flip();

		if (!peerAddress.equals(sendAddress)) // ignore our own packets
		{
			try
			{
				UdpDiscoveryPeer peer = UdpDiscoveryProtocol.parsePacket(receiveBuffer, peerAddress);
				log.debug("Got peer: {}", peer);

				if (isValidPeer(peer))
				{
					if (!peers.containsKey(peer.getPeerId())) // XXX: removed this because the original version always increments the packetIndex()... || peers.get(peer.getPeerId()).getPacketIndex() != peer.getPacketIndex()) // We use != so that it works with the 32-bit version
					{
						// Add or update
						peers.put(peer.getPeerId(), peer);
						try (var session = new DatabaseSession(databaseSessionManager))
						{
							log.debug("Trying to update friend's IP");

							locationService.findLocationById(peer.getLocationId()).ifPresent(location -> {
								if (!location.isConnected())
								{
									var lanConnection = Connection.from(PeerAddress.from(peer.getIpAddress(), peer.getLocalPort()));
									location.getConnections().stream()
											.filter(connection -> connection.equals(lanConnection))
											.findFirst()
													.ifPresentOrElse(connection -> {
													}, () -> {
														log.debug("Updating friend {} with ip {}", location, lanConnection);
														location.addConnection(lanConnection);
													});
										}
									}
							);
						}
					}
					// XXX: and if a client is missing for 10 broadcasts or so, remove it
				}
			}
			catch (RuntimeException e)
			{
				log.debug("Failed to parse packet: {}", e.getMessage());
			}
		}
		receiveBuffer.clear();
	}

	private void sendBroadcast(DatagramChannel channel) throws IOException
	{
		assert state == State.BROADCASTING;

		channel.send(sendBuffer, broadcastAddress);
		sent = Instant.now();
		setState(State.WAITING);
		sendBuffer.rewind();
	}

	private boolean isValidPeer(UdpDiscoveryPeer peer)
	{
		return peer.getAppId() == APP_ID && peer.getStatus() == UdpDiscoveryPeer.Status.PRESENT;
	}
}
