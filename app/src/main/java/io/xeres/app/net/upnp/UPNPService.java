/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.net.upnp;

import io.xeres.app.application.events.UpnpEvent;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.net.external.ExternalIpResolver;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.notification.status.StatusNotificationService;
import io.xeres.common.rest.notification.status.NatStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.time.Duration;

@Service
public class UPNPService implements Runnable
{
	private static final Logger log = LoggerFactory.getLogger(UPNPService.class);

	private static final String MULTICAST_IP = "239.255.255.250";
	private static final int MULTICAST_PORT = 1900;
	private static final int MULTICAST_BUFFER_SEND_SIZE_MAX = 512; // this is the maximum size used by MiniUPNPd so better not use more
	private static final int MULTICAST_BUFFER_RECV_SIZE = 1024; // also used by MiniUPNPd

	private static final int MULTICAST_MAX_WAIT_TIME = (int) Duration.ofSeconds(3).toMillis(); // time to wait for a router reply
	private static final int MULTICAST_MAX_WAIT_SNOOZE = (int) Duration.ofMinutes(5).toMillis(); // time to wait if nothing has answered all requests
	private static final int MULTICAST_DELAY_HINT = (int) Duration.ofSeconds(1).toSeconds(); // how long a router can delay its reply

	private static final int PORT_DURATION = (int) Duration.ofHours(1).toMillis(); // how long does a port mapping lasts
	private static final int PORT_DURATION_ANTICIPATION = (int) Duration.ofMinutes(1).toMillis(); // when to kick in the refresh before it expires

	private static final Duration SERVICE_RETRY_DURATION = Duration.ofMinutes(5); // time to retry the service when getting an error

	private static final String[] DEVICES = {
			// IGD 1
			"urn:schemas-upnp-org:device:InternetGatewayDevice:1",
			"urn:schemas-upnp-org:service:WANIPConnection:1",
			"urn:schemas-upnp-org:device:WANDevice:1",
			"urn:schemas-upnp-org:device:WANConnectionDevice:1",
			"urn:schemas-upnp-org:service:WANPPPConnection:1",
			// IGD 2
			"urn:schemas-upnp-org:device:InternetGatewayDevice:2",
			"urn:schemas-upnp-org:device:WANDevice:2",
			"urn:schemas-upnp-org:device:WANConnectionDevice:2",
			"urn:schemas-upnp-org:service:WANIPConnection:2",
			// Most routers will respond to all entries
	};

	private enum State
	{
		SNOOZING,
		BROADCASTING,
		WAITING,
		CONNECTING,
		CONNECTED,
		INTERRUPTED
	}

	private final LocationService locationService;
	private final ApplicationEventPublisher publisher;
	private final StatusNotificationService statusNotificationService;
	private final DatabaseSessionManager databaseSessionManager;
	private final ExternalIpResolver externalIpResolver;

	private int deviceIndex;

	private String localIpAddress;
	private int localPort;
	private int controlPort;
	private Thread thread;

	private SocketAddress multicastAddress;
	private ByteBuffer sendBuffer;
	private ByteBuffer receiveBuffer;
	private State state;
	private Device device;
	private boolean externalIpAddressFound;

	public UPNPService(LocationService locationService, ApplicationEventPublisher publisher, StatusNotificationService statusNotificationService, DatabaseSessionManager databaseSessionManager, ExternalIpResolver externalIpResolver)
	{
		this.locationService = locationService;
		this.publisher = publisher;
		this.statusNotificationService = statusNotificationService;
		this.databaseSessionManager = databaseSessionManager;
		this.externalIpResolver = externalIpResolver;
	}

	public void start(String localIpAddress, int localPort, int controlPort)
	{
		log.info("Starting UPNP service...");
		this.localIpAddress = localIpAddress;
		this.localPort = localPort;
		this.controlPort = controlPort;

		statusNotificationService.setNatStatus(NatStatus.UNKNOWN);

		thread = Thread.ofVirtual()
				.name("UPNP Service")
				.start(this);
	}

	public void stop()
	{
		if (thread != null)
		{
			log.info("Stopping UPNP...");
			thread.interrupt();
		}

		statusNotificationService.setNatStatus(NatStatus.UNKNOWN);
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
				log.info("Waiting for UPNP service to terminate...");
				thread.join();
				log.debug("UPNP service terminated");
			}
			catch (InterruptedException e)
			{
				log.error("Failed to wait for termination: {}", e.getMessage(), e);
				Thread.currentThread().interrupt();
			}
		}
	}

	private static String getMSearch(String device)
	{
		return "M-SEARCH * HTTP/1.1\r\nHost: " + MULTICAST_IP + ":" + MULTICAST_PORT + "\r\nST: " + device + "\r\nMan: \"ssdp:discover\"\r\nMX: " + MULTICAST_DELAY_HINT + "\r\n\r\n";
	}

	private void getUpnpDeviceSearch(SelectionKey selectionKey)
	{
		sendBuffer = ByteBuffer.wrap(getMSearch(DEVICES[deviceIndex % DEVICES.length]).getBytes());
		if (sendBuffer.limit() > MULTICAST_BUFFER_SEND_SIZE_MAX)
		{
			throw new IllegalArgumentException("Send buffer bigger than " + MULTICAST_BUFFER_SEND_SIZE_MAX + " (" + sendBuffer.limit() + ")");
		}
		deviceIndex++;
		if (deviceIndex > 0 && deviceIndex % DEVICES.length == 0)
		{
			setState(State.SNOOZING, selectionKey);
		}
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				upnpLoop();
				break;
			}
			catch (BindException e)
			{
				log.warn("Binding failed: {}, trying again in 5 minutes", e.getMessage());
				try
				{
					Thread.sleep(SERVICE_RETRY_DURATION);
				}
				catch (InterruptedException _)
				{
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
	}

	private void upnpLoop() throws BindException
	{
		multicastAddress = new InetSocketAddress(MULTICAST_IP, MULTICAST_PORT);
		receiveBuffer = ByteBuffer.allocate(MULTICAST_BUFFER_RECV_SIZE);

		try (var selector = Selector.open();
		     var channel = DatagramChannel.open(StandardProtocolFamily.INET)
				     .bind(new InetSocketAddress(InetAddress.getByName(localIpAddress), 0))
		)
		{
			channel.configureBlocking(false);
			var registerSelectionKeys = channel.register(selector, SelectionKey.OP_WRITE);
			state = State.BROADCASTING;

			while (true)
			{
				if (state == State.BROADCASTING)
				{
					getUpnpDeviceSearch(registerSelectionKeys);
				}

				if (state == State.SNOOZING)
				{
					attemptFindExternalAddressUsingDnsIfNeeded();
				}

				selector.select(getSelectorTimeout());
				if (Thread.interrupted())
				{
					setState(State.INTERRUPTED, registerSelectionKeys);
					break;
				}
				if (state == State.CONNECTED)
				{
					var refreshed = refreshPorts();
					if (refreshed)
					{
						statusNotificationService.setNatStatus(NatStatus.UPNP);
					}
					else
					{
						log.error("UPNP port refresh failed, starting again...");
						statusNotificationService.setNatStatus(NatStatus.FIREWALLED);
						setState(State.BROADCASTING, registerSelectionKeys);
						continue;
					}
				}
				handleSelection(selector, registerSelectionKeys);
			}
			cleanupDevice();
		}
		catch (ClosedByInterruptException _)
		{
			log.debug("Interrupted, bailing out...");
		}
		catch (BindException e)
		{
			throw e;
		}
		catch (IOException e)
		{
			log.error("Error: ", e);
		}
	}

	private void handleSelection(Selector selector, SelectionKey registerSelectionKeys) throws BindException
	{
		var selectedKeys = selector.selectedKeys().iterator();
		if (!selectedKeys.hasNext() && state != State.CONNECTED)
		{
			setState(State.BROADCASTING, registerSelectionKeys);
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
				else if (key.isWritable())
				{
					write(key);
				}
			}
			catch (BindException e)
			{
				throw e;
			}
			catch (IOException e)
			{
				log.warn("Glitch, continuing...", e);
			}
		}
	}

	private int getSelectorTimeout()
	{
		return switch (state)
		{
			case WAITING -> MULTICAST_MAX_WAIT_TIME;
			case SNOOZING -> MULTICAST_MAX_WAIT_SNOOZE;
			case CONNECTED -> PORT_DURATION - PORT_DURATION_ANTICIPATION;
			default -> 0;
		};
	}

	private void setState(State newState, SelectionKey key)
	{
		state = newState;

		switch (state)
		{
			case BROADCASTING -> key.interestOps(SelectionKey.OP_WRITE);
			case WAITING -> key.interestOps(SelectionKey.OP_READ);
			case CONNECTING, CONNECTED, SNOOZING -> key.interestOps(0);
			case INTERRUPTED -> log.debug("Interrupted");
		}
	}

	private void read(SelectionKey key) throws IOException
	{
		assert state == State.WAITING;

		@SuppressWarnings("resource") var channel = (DatagramChannel) key.channel();
		var routerAddress = channel.receive(receiveBuffer); // XXX: handle multiple responses if there's several routers. use 'rootdevice' to test
		device = Device.from(routerAddress, receiveBuffer);
		if (device.isValid())
		{
			setState(State.CONNECTING, key);
			device.addControlPoint();

			if (device.hasControlPoint())
			{
				setState(State.CONNECTED, key);
				var portsAdded = refreshPorts();
				var externalAddressFound = findExternalIpAddressUsingUpnp();
				if (!externalAddressFound)
				{
					externalAddressFound = findExternalIpAddressUsingDns();
				}

				publisher.publishEvent(new UpnpEvent(localPort, portsAdded, externalAddressFound));
				statusNotificationService.setNatStatus(portsAdded ? NatStatus.UPNP : NatStatus.FIREWALLED);
			}
			else
			{
				// Device has no control point, or it's unreachable; keep searching
				setState(State.WAITING, key);
			}
		}
		else
		{
			// Device has no location or address, keep searching
			setState(State.WAITING, key);
		}

		// XXX: a device must be blacklisted for a while if the above 2 steps fail for it, otherwise we'll run into it again if the user has a broken router on the same LAN
		receiveBuffer.clear(); // ready to read again
	}

	private void write(SelectionKey key) throws IOException
	{
		assert state == State.BROADCASTING;

		@SuppressWarnings("resource") var channel = (DatagramChannel) key.channel();
		channel.send(sendBuffer, multicastAddress);
		setState(State.WAITING, key);
		sendBuffer.clear();
	}

	private boolean refreshPorts()
	{
		// XXX: add a mechanism if the localport is already taken on the router?
		var refreshed = device.addPortMapping(localIpAddress, localPort, localPort, PORT_DURATION / 1000, Protocol.TCP);
		refreshed &= device.addPortMapping(localIpAddress, localPort, localPort, PORT_DURATION / 1000, Protocol.UDP);
		if (controlPort != 0)
		{
			refreshed &= device.addPortMapping(localIpAddress, controlPort, controlPort, PORT_DURATION / 1000, Protocol.TCP);
		}

		if (refreshed)
		{
			log.info("UPNP ports added/refreshed successfully.");
		}
		else
		{
			log.warn("Failed to add/refresh UPNP ports. Incoming connections won't be accepted.");
		}

		return refreshed;
	}

	private void cleanupDevice()
	{
		if (device != null && device.hasControlPoint())
		{
			device.removeAllPortMapping();
		}
	}

	private void attemptFindExternalAddressUsingDnsIfNeeded()
	{
		// If no UPNP seems available after the first try, attempt
		// to find the external address using OpenDNS then keep trying
		// with UPNP (even though it's unlikely to work). This allows at least
		// to have the IP in the ShortInvite, which is important for reachability.
		if (!externalIpAddressFound && findExternalIpAddressUsingDns())
		{
			externalIpAddressFound = true;
			publisher.publishEvent(new UpnpEvent(localPort, false, true));
			statusNotificationService.setNatStatus(NatStatus.FIREWALLED);
		}
	}

	private boolean findExternalIpAddressUsingUpnp()
	{
		return updateExternalIpAddress(device.getExternalIpAddress());
	}

	private boolean findExternalIpAddressUsingDns()
	{
		var externalIpAddress = externalIpResolver.find();
		if (externalIpAddress == null)
		{
			return false;
		}
		return updateExternalIpAddress(externalIpAddress);
	}

	private boolean updateExternalIpAddress(String externalIpAddress)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			var peerAddress = PeerAddress.from(externalIpAddress, localPort);
			if (peerAddress.isInvalid())
			{
				log.warn("External IP is invalid: {}", externalIpAddress);
				return false;
			}
			if (!peerAddress.isExternal())
			{
				log.warn("External IP is not external: {}", externalIpAddress);
				return false;
			}
			locationService.updateConnection(locationService.findOwnLocation().orElseThrow(), peerAddress);
			return true;
		}
	}
}
