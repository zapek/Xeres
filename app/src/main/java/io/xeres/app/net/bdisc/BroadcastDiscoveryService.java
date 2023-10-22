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

package io.xeres.app.net.bdisc;

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.service.LocationService;
import io.xeres.ui.support.tray.TrayService;
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

/**
 * This service periodically sends a UDP broadcast packet to find out
 * if other Retroshare clients are on the LAN. It implements more or
 * less the same protocol as found in the project <a href="https://github.com/truvorskameikin/udp-discovery-cpp">udp-discovery-cpp</a>
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

	private static final Duration LAST_SEEN_TIMEOUT = Duration.ofMinutes(1);

	private static final Duration BROADCAST_MAX_WAIT_TIME = Duration.ofSeconds(5);

	private enum State
	{
		BROADCASTING,
		WAITING,
		INTERRUPTED
	}

	private final DatabaseSessionManager databaseSessionManager;
	private final LocationService locationService;
	private final TrayService trayService;

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

	public BroadcastDiscoveryService(DatabaseSessionManager databaseSessionManager, LocationService locationService, TrayService trayService)
	{
		this.databaseSessionManager = databaseSessionManager;
		this.locationService = locationService;
		this.trayService = trayService;
	}

	public void start(String localIpAddress, int localPort)
	{
		log.info("Starting Broadcast Discovery service...");
		localAddress = new InetSocketAddress(localIpAddress, localPort);
		thread = Thread.ofVirtual()
				.name("Broadcast Discovery Service")
				.start(this);
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
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			var ownLocation = locationService.findOwnLocation().orElseThrow();

			ownPeerId = ownLocation.getLocationId().hashCode();
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
		// For now, we do nothing; but we could implement something better if for
		// example there's a change of IP or port. Don't forget to increase the
		// counter for each update otherwise it won't be taken into account.
		// but see https://github.com/truvorskameikin/udp-discovery-cpp/issues/18
	}

	@Override
	public void run()
	{
		broadcastAddress = new InetSocketAddress(getBroadcastAddress(localAddress.getHostString()), PORT);
		receiveBuffer = ByteBuffer.allocate(BROADCAST_BUFFER_RECV_SIZE);

		setupOwnInfo();

		try (var selector = Selector.open();
		     var receiveChannel = DatagramChannel.open(StandardProtocolFamily.INET)
				     .setOption(StandardSocketOptions.SO_REUSEADDR, true)
				     .bind(new InetSocketAddress(localAddress.getHostString(), PORT));
		     var sendChannel = DatagramChannel.open(StandardProtocolFamily.INET)
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
		var selectedKeys = selector.selectedKeys().iterator();

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
				var key = selectedKeys.next();
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
				log.warn("Glitch, continuing...", e);
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

		@SuppressWarnings("resource") var channel = (DatagramChannel) key.channel();
		var peerAddress = (InetSocketAddress) channel.receive(receiveBuffer);
		receiveBuffer.flip();

		if (!peerAddress.equals(sendAddress)) // ignore our own packets
		{
			try
			{
				var peer = UdpDiscoveryProtocol.parsePacket(receiveBuffer, peerAddress);
				log.debug("Got peer: {}", peer);
				var now = Instant.now();

				if (isValidPeer(peer))
				{
					var lastSeenPeer = peers.get(peer.getPeerId());
					if (lastSeenPeer != null)
					{
						// If a client is missing for a minute, remove it
						if (lastSeenPeer.getLastSeen().plus(LAST_SEEN_TIMEOUT).isBefore(now))
						{
							log.debug("Removing peer {} because it hasn't been seen for more than a minute", peer);
							peers.remove(peer.getPeerId());
						}
						else
						{
							lastSeenPeer.setLastSeen(now);
						}
					}
					else
					{
						// Add. Currently, the protocol's packet index is always incremented so there can't be an optimization to see if there's new data, so we can't update.
						peers.put(peer.getPeerId(), peer);
						peer.setLastSeen(now);
						try (var ignored = new DatabaseSession(databaseSessionManager))
						{
							log.debug("Trying to update friend's IP");

							locationService.findLocationByLocationId(peer.getLocationId()).ifPresentOrElse(location -> {
								if (!location.isConnected())
								{
									var lanConnection = Connection.from(PeerAddress.from(peer.getIpAddress(), peer.getLocalPort()));

									log.debug("Updating friend {} with ip {}", location, lanConnection);
									location.addConnection(lanConnection);
								}
							}, () -> trayService.showNotification("Detected client on LAN: " + peer.getProfileName() + " at " + peer.getIpAddress()));
						}
					}
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
		return peer != null && peer.getAppId() == APP_ID && peer.getStatus() == UdpDiscoveryPeer.Status.PRESENT && peer.getPeerId() != ownPeerId;
	}
}
