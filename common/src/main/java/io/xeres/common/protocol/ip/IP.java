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

package io.xeres.common.protocol.ip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * IP handling utility class.
 */
public final class IP
{
	private static final Logger log = LoggerFactory.getLogger(IP.class);

	/**
	 * List of port to avoid picking up as default because of their popularity in a NAT setup.
	 * Xeres uses a range from 1025 to 32767.
	 * Note that some ports aren't really popular, but they're scanned by default by some anti-viruses.
	 */
	private static final Set<Integer> reservedPorts = Set.of(
			1080,  // Socks proxy
			1194,  // Open VPN
			1433,  // MS SQL
			1701,  // L2TP
			1723,  // PPTP VPN
			1900,  // SSDP
			2021,  // FTP ALG
			2041,  // Mail.ru
			2086,  // GNUnet
			2375,  // Docker
			2376,  // Docker (SSL)
			3074,  // XBox Live
			3128,  // Default proxy
			3306,  // MySQL
			3389,  // Remote Desktop Protocol
			4242,  // Quassel
			4444,  // I2P Proxy
			4500,  // IPSec
			5000,  // Yahoo!
			5001,  // Yahoo!
			5050,  // Yahoo!
			5101,  // Yahoo!
			5190,  // ICQ
			5060,  // Asterisk
			5061,  // Asterisk (SSL)
			5222,  // Jabber
			5223,  // Jabber
			5269,  // Jabber
			6667,  // IRC
			6697,  // IRCS
			6881,  // Bittorrent
			6882,  // Bittorrent
			6883,  // Bittorrent
			6884,  // Bittorrent
			6885,  // Bittorrent
			6886,  // Bittorrent
			6887,  // Bittorrent
			6888,  // Bittorrent
			6889,  // Bittorrent
			7652,  // I2P
			7653,  // I2P
			7654,  // I2P
			7900,  // Many local tests
			8000,  // Many local tests
			8080,  // Many local tests
			8088,  // Many local tests
			8888,  // Many local tests
			9001,  // Tor
			9030,  // Tor
			9050,  // Tor
			9051,  // Tor
			9080,  // Logitech's LGHUB
			11523  // No idea why Kaspersky scans this
	);

	private static final int BINDING_ATTEMPTS_MAX = 100; // After that many failed attempts, there must be something wrong

	private IP()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Finds a free local port to bind to. There's a built-in blacklist of commonly used ports which are avoided.
	 *
	 * @return a free local port
	 */
	public static int getFreeLocalPort()
	{
		int port;
		var bindErrorDetector = 0;

		while (true)
		{
			// Avoid Ephemeral ports, see https://en.wikipedia.org/wiki/Ephemeral_port
			port = ThreadLocalRandom.current().nextInt(1025, 32767);

			if (reservedPorts.contains(port))
			{
				continue;
			}

			try (var socket = new Socket())
			{
				socket.bind(new InetSocketAddress("0.0.0.0", port));
				return port;
			}
			catch (IOException e)
			{
				if (bindErrorDetector > BINDING_ATTEMPTS_MAX)
				{
					throw new IllegalStateException("Failure to bind a local port. Check your network setup.");
				}
				bindErrorDetector++;
			}
		}
	}

	/**
	 * Tries its best to get the local IP address, without requiring an external
	 * server. Should work at all times unless the host has no Internet access.
	 *
	 * @return the local IP address or null
	 */
	public static String getLocalIpAddress()
	{
		String ip;

		try (var socket = new DatagramSocket())
		{
			socket.connect(InetAddress.getByName("1.1.1.1"), 10000);
			ip = socket.getLocalAddress().getHostAddress();
			if (isRoutableIp(ip))
			{
				log.debug("Own IP found using routing system: {}", ip);
				return ip;
			}

			// The above is reported to not work on MacOS, if so, just scan all interfaces manually.
			ip = findIpFromInterfaces();
			if (isRoutableIp(ip))
			{
				log.debug("Own IP found by walking down interfaces: {}", ip);
				return ip;
			}
		}
		catch (IOException | UncheckedIOException e)
		{
			ip = null;
		}
		return ip;
	}

	/**
	 * Checks if the IP address can be bound to (that is, a server can run on it).
	 *
	 * @param ip the IP address to check
	 * @return true if it's bindable
	 */
	public static boolean isBindableIp(String ip)
	{
		return isLanIp(ip) || isPublicIp(ip) || isLocalIp(ip);
	}

	/**
	 * Checks if the IP address is routable, which means it's either a valid LAN address (for example, 192.168.1.4) or a public IP address.
	 *
	 * @param ip the IP address to check
	 * @return true if it's routable
	 */
	public static boolean isRoutableIp(String ip)
	{
		return isLanIp(ip) || isPublicIp(ip);
	}

	/**
	 * Checks if the IP address if from a LAN (that is, a privately routable IP address; for example, 192.168.1.4 or 10.0.0.5).
	 *
	 * @param ip the IP address to check
	 * @return true if it's a LAN address
	 */
	public static boolean isLanIp(String ip)
	{
		try
		{
			return isLanAddress(InetAddress.getByName(ip));
		}
		catch (UnknownHostException e)
		{
			return false;
		}
	}

	/**
	 * Checks if the IP address is a publicly routable IP address (that is, an IP that an Internet router will forward).
	 *
	 * @param ip the IP address to check
	 * @return true if it's a public IP address
	 */
	public static boolean isPublicIp(String ip)
	{
		try
		{
			return isPublicAddress(InetAddress.getByName(ip));
		}
		catch (UnknownHostException e)
		{
			return false;
		}
	}

	/**
	 * Checks if the IP address is a local IP (localhost or link local).
	 *
	 * @param ip the IP address to check
	 * @return true if it's a local IP address
	 */
	public static boolean isLocalIp(String ip)
	{
		try
		{
			return isLocalAddress(InetAddress.getByName(ip));
		}
		catch (UnknownHostException e)
		{
			return false;
		}
	}

	/**
	 * Try to find the local IP by iterating all interfaces.<br>
	 * Note: this doesn't work in all cases (for example, if docker has some address like 10.0.75.1 then it might be
	 * picked up before the proper interface).
	 *
	 * @return the IP address if found, otherwise null
	 * @throws SocketException if there's a failure to get the interfaces
	 */
	private static String findIpFromInterfaces() throws SocketException
	{
		var interfaces = NetworkInterface.getNetworkInterfaces().asIterator();
		while (interfaces.hasNext())
		{
			var networkInterface = interfaces.next();
			if (networkInterface.isUp())
			{
				var addresses = networkInterface.getInetAddresses().asIterator();

				while (addresses.hasNext())
				{
					var address = addresses.next();

					if (isRoutableAddress(address))
					{
						log.debug("IP found using interface enumeration system: {}", address.getHostAddress());
						return address.getHostAddress();
					}
				}
			}
		}
		return null;
	}

	private static boolean isRoutableAddress(InetAddress address)
	{
		return isLanAddress(address) || isPublicAddress(address);
	}

	private static boolean isLanAddress(InetAddress address)
	{
		return address.isSiteLocalAddress();
	}

	private static boolean isPublicAddress(InetAddress address)
	{
		return !(isSpecifiedHostOnThisNetwork(address) || // 0.0.0.0 - 0.255.255.255
				address.isLoopbackAddress() || // 127.0.0.0 - 127.255.255.255
				address.isSiteLocalAddress() || // 10.0.0.0 - 10.255.255.255 | 172.16.0.0 - 172.31.255.255 | 192.0.0.0 - 192.0.0.255
				isSharedAddressSpace(address) || // 100.64.0.0 - 100.127.255.255
				address.isLinkLocalAddress() || // 169.254.0.0 - 169.254.255.255
				address.isMulticastAddress() || // 224.0.0.0 - 239.255.255.255
				isLimitedBroadcastAddress(address)); // 255.255.255.255
	}

	private static boolean isLocalAddress(InetAddress address)
	{
		return address.isLinkLocalAddress() ||
				address.isLoopbackAddress();
	}

	private static boolean isLimitedBroadcastAddress(InetAddress address)
	{
		// 255.255.255.255, see rfc6890
		return IntStream.of(3, 2, 1, 0).allMatch(i -> address.getAddress()[i] == -1);
	}

	/**
	 * Check if an address is a <i>current network</i> (0.0.0.0/8). It must not be sent except as a source
	 * address as part of an initialization procedure by which the host learns its full IP address.<br>
	 * Note: 0.0.0.0 (bind to any interface) is included as well. If you only need it, use {@link InetAddress#isAnyLocalAddress()} instead.
	 *
	 * @param address the address to test
	 * @return true if the address represents a <i>current network</i>
	 * @see <a href="https://tools.ietf.org/html/rfc6890">rfc6890</a> and <a href="https://tools.ietf.org/html/rfc1122#page-29">rfc1122 (section 3.2.1.3)</a>
	 */
	private static boolean isSpecifiedHostOnThisNetwork(InetAddress address)
	{
		return address.getAddress()[0] == 0;
	}

	/**
	 * Check if an address is in a shared address space (100.64.0.0/10), which is used when the ISP
	 * is using a carrier-grade NAT. This address cannot be reached from the public Internet directly.
	 *
	 * @param address the address to test
	 * @return true if in a shared address space
	 * @see <a href="https://tools.ietf.org/html/rfc6598">rfc6598</a>
	 */
	private static boolean isSharedAddressSpace(InetAddress address)
	{
		return address.getAddress()[0] == 100 && Byte.toUnsignedInt(address.getAddress()[1]) >= 64 && Byte.toUnsignedInt(address.getAddress()[1]) < 128;
	}

	/**
	 * Checks if a port is invalid (that is, cannot be bound to or sent to).
	 *
	 * @param port the port to check
	 * @return true if valid
	 */
	public static boolean isInvalidPort(int port)
	{
		return port <= 0 || port >= 65536;
	}
}
