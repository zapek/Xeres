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

package io.xeres.app.net.protocol;

import io.xeres.common.protocol.HostPort;
import io.xeres.common.protocol.i2p.I2pAddress;
import io.xeres.common.protocol.ip.IP;
import io.xeres.common.protocol.tor.OnionAddress;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

import static io.xeres.app.net.protocol.PeerAddress.Type.*;
import static io.xeres.common.protocol.ip.IP.isInvalidPort;
import static java.util.function.Predicate.not;

/**
 * A class that can contain any peer address.
 * <p>
 * Vocabulary:
 * <ul>
 * <li>url: a Retroshare URL (ipv4://192.168.1.1:80, etc...)</li>
 * <li>address: a string that can be an ipv4 socket or tor address (192.168.1.1:80, foobar.onion, ...)</li>
 * <li>ipAndPort: 192.168.1.1:80</li>
 * <li>socket address: an ip socket address, directly usable with java functions</li>
 * </ul>
 * <p>Creating a PeerAddress always succeed. Its validity can be checked with isValid().
 */
public final class PeerAddress
{
	public enum Type
	{
		INVALID(""),
		IPV4("ipv4://"),
		IPV6("ipv6://"),
		TOR(""),
		HOSTNAME(""),
		I2P("");

		private final String scheme;

		Type(String scheme)
		{
			this.scheme = scheme;
		}

		public String scheme()
		{
			return scheme;
		}
	}

	private static final Pattern HOSTNAME_OK_PATTERN = Pattern.compile("^(?:\\p{Alnum}(?>[\\p{Alnum}-]{0,61}\\p{Alnum})?\\.)+(\\p{Alpha}(?>[\\p{Alnum}-]{0,61}\\p{Alnum})?)\\.?$");

	private SocketAddress socketAddress;
	private final Type type;

	/**
	 * Creates a PeerAddress from a URL (ipv4://, etc...).
	 *
	 * @param url the URL
	 * @return a PeerAddress
	 */
	public static PeerAddress fromUrl(String url)
	{
		if (url == null)
		{
			return fromInvalid();
		}

		if (url.startsWith(IPV4.scheme()))
		{
			return fromIpAndPort(url.substring(IPV4.scheme().length()));
		}
		return fromInvalid();
	}

	/**
	 * Creates a PeerAddress from an address (eg. juiejkslajfsk.onion, 85.12.33.11:8081, ...).
	 *
	 * @param address the address
	 * @return a PeerAddress
	 */
	public static PeerAddress fromAddress(String address)
	{
		if (address == null)
		{
			return fromInvalid();
		}
		return tryFromHidden(address).orElse(tryFromIpAndPort(address).orElse(fromHostnameAndPort(address)));
	}

	/**
	 * Creates a PeerAddress from a hidden address (Tor/I2P)
	 *
	 * @param address the address
	 * @return a PeerAddress
	 */
	public static PeerAddress fromHidden(String address)
	{
		if (address == null)
		{
			return fromInvalid();
		}
		return tryFromHidden(address).orElse(fromInvalid());
	}

	private static Optional<PeerAddress> tryFromHidden(String address)
	{
		var peerAddress = tryFromOnion(address);
		if (peerAddress.isEmpty())
		{
			peerAddress = tryFromI2p(address);
		}
		return peerAddress;
	}

	private static Optional<PeerAddress> tryFromIpAndPort(String address)
	{
		var peerAddress = fromIpAndPort(address);
		if (peerAddress.isValid())
		{
			return Optional.of(peerAddress);
		}
		return Optional.empty();
	}

	/**
	 * Creates a PeerAddress from an IP and a port.
	 *
	 * @param ip   the IP address
	 * @param port the port
	 * @return a PeerAddress
	 */
	public static PeerAddress from(String ip, int port)
	{
		if (isInvalidPort(port))
		{
			return fromInvalid();
		}
		if (isInvalidIpAddress(ip))
		{
			return fromInvalid();
		}
		try
		{
			return new PeerAddress(new InetSocketAddress(InetAddress.getByName(ip), port), IPV4);
		}
		catch (UnknownHostException e)
		{
			return fromInvalid(); // Won't happen anyway
		}
	}

	/**
	 * Creates a PeerAddress from an "ip:port" string.
	 *
	 * @param ipAndPort a string in the form "ip:port"; for example, "192.168.1.2:8002"
	 * @return a PeerAddress
	 */
	public static PeerAddress fromIpAndPort(String ipAndPort)
	{
		try
		{
			var hostPort = HostPort.parse(ipAndPort);
			return from(hostPort);
		}
		catch (IllegalArgumentException e)
		{
			return fromInvalid();
		}
	}

	public static PeerAddress from(HostPort hostPort)
	{
		return from(hostPort.host(), hostPort.port());
	}

	/**
	 * Creates a PeerAddress from a RsCertificate byte array.
	 *
	 * @param data a byte array which is made of the 4 bytes of the IP and the 2 bytes of the port (big endian).
	 * @return a PeerAddress
	 */
	public static PeerAddress fromByteArray(byte[] data)
	{
		if (data == null || data.length != 6)
		{
			return fromInvalid();
		}

		var ip = String.format("%d.%d.%d.%d", Byte.toUnsignedInt(data[0]), Byte.toUnsignedInt(data[1]), Byte.toUnsignedInt(data[2]), Byte.toUnsignedInt(data[3]));

		if (isInvalidIpAddress(ip))
		{
			return fromInvalid();
		}

		var port = Byte.toUnsignedInt(data[4]) << 8 | Byte.toUnsignedInt(data[5]);
		if (isInvalidPort(port))
		{
			return fromInvalid();
		}
		return from(ip, port);
	}

	public static PeerAddress fromHostname(String hostname)
	{
		if (isInvalidHostname(hostname))
		{
			return fromInvalid();
		}
		return new PeerAddress(DomainNameSocketAddress.of(hostname), HOSTNAME);
	}

	public static PeerAddress fromHostname(String hostname, int port)
	{
		if (isInvalidHostname(hostname) || isInvalidPort(port))
		{
			return fromInvalid();
		}
		return new PeerAddress(InetSocketAddress.createUnresolved(hostname, port), HOSTNAME);
	}

	public static PeerAddress fromHostnameAndPort(String hostnameAndPort)
	{
		try
		{
			var hostPort = HostPort.parse(hostnameAndPort);
			if (isInvalidHostname(hostPort.host()))
			{
				return fromInvalid();
			}
			return fromHostname(hostPort.host(), hostPort.port());
		}
		catch (IllegalArgumentException e)
		{
			return fromInvalid();
		}
	}

	public static PeerAddress fromSocketAddress(SocketAddress socketAddress)
	{
		return new PeerAddress(socketAddress, Type.IPV4);
	}

	/**
	 * Creates a PeerAddress from an onion address (ie. "jskljfksdjk.onion")
	 *
	 * @param onion the onion address
	 * @return a PeerAddress
	 */
	public static PeerAddress fromOnion(String onion)
	{
		return tryFromOnion(onion).orElse(fromInvalid());
	}

	private static Optional<PeerAddress> tryFromOnion(String onion)
	{
		if (OnionAddress.isValidAddress(onion))
		{
			var hostPort = HostPort.parse(onion);
			return Optional.of(new PeerAddress(InetSocketAddress.createUnresolved(hostPort.host(), hostPort.port()), TOR));
		}
		return Optional.empty();
	}

	public static PeerAddress fromI2p(String i2p)
	{
		return tryFromI2p(i2p).orElse(fromInvalid());
	}

	private static Optional<PeerAddress> tryFromI2p(String i2p)
	{
		if (I2pAddress.isValidAddress(i2p))
		{
			var hostPort = HostPort.parse(i2p);
			return Optional.of(new PeerAddress(InetSocketAddress.createUnresolved(hostPort.host(), hostPort.port()), I2P));
		}
		return Optional.empty();
	}

	/**
	 * Creates an invalid PeerAddress.
	 *
	 * @return a PeerAddress
	 */
	public static PeerAddress fromInvalid()
	{
		return new PeerAddress(INVALID);
	}

	private PeerAddress(Type type)
	{
		this.type = type;
	}

	private PeerAddress(SocketAddress socketAddress, Type type)
	{
		this.type = type;
		this.socketAddress = socketAddress;
	}

	/**
	 * Gets the SocketAddress of the PeerAddress (if the protocol allows it).
	 *
	 * @return a SocketAddress
	 */
	public SocketAddress getSocketAddress()
	{
		return socketAddress;
	}

	/**
	 * Gets the address and port of the PeerAddress (if the protocol allows it), or any other suitable format.
	 *
	 * @return the IP address and port in the following format: "ip:port" or any other suitable format
	 */
	public Optional<String> getAddress()
	{
		if (socketAddress instanceof InetSocketAddress inetSocketAddress)
		{
			return Optional.of(inetSocketAddress.getHostString() + ":" + inetSocketAddress.getPort());
		}
		else if (socketAddress instanceof DomainNameSocketAddress domainNameSocketAddress)
		{
			return Optional.of(domainNameSocketAddress.getName());
		}
		return Optional.empty();
	}

	/**
	 * Gets the IP address and port in an array of bytes.
	 *
	 * @return the IP address in the 4 first bytes and the port in the 2 last ones (big endian).
	 */
	public Optional<byte[]> getAddressAsBytes()
	{
		if (socketAddress instanceof InetSocketAddress inetSocketAddress)
		{
			var port = inetSocketAddress.getPort();

			switch (type)
			{
				case HOSTNAME ->
				{
					var hostname = inetSocketAddress.getHostName().getBytes(StandardCharsets.US_ASCII);
					var bytes = new byte[hostname.length + 2];
					System.arraycopy(hostname, 0, bytes, 0, hostname.length);
					bytes[bytes.length - 2] = (byte) (port >> 8);
					bytes[bytes.length - 1] = (byte) (port & 0xff);
					return Optional.of(bytes);
				}
				case IPV4 ->
				{
					var bytes = new byte[6];
					System.arraycopy(inetSocketAddress.getAddress().getAddress(), 0, bytes, 0, 4);
					bytes[4] = (byte) (port >> 8);
					bytes[5] = (byte) (port & 0xff);
					return Optional.of(bytes);
				}
				case null, default -> throw new UnsupportedOperationException("Can't get address for type " + type);
			}
		}
		else if (socketAddress instanceof DomainNameSocketAddress)
		{
			throw new IllegalStateException("Can't get the address of a DomainNameSocketAddress as it requires a port");
		}
		return Optional.empty();
	}

	/**
	 * Gets the type of the PeerAddress.
	 *
	 * @return the type of the PeerAddress
	 */
	public Type getType()
	{
		return type;
	}

	public String getUrl()
	{
		return type.scheme() + getAddress().orElseThrow();
	}

	/**
	 * Checks if the PeerAddress is invalid.
	 *
	 * @return true if invalid
	 */
	public boolean isInvalid()
	{
		return type == INVALID;
	}

	/**
	 * Checks if the PeerAddress is valid.
	 *
	 * @return true if valid
	 */
	public boolean isValid()
	{
		return type != INVALID;
	}

	/**
	 * Checks if the PeerAddress is a hidden address (Tor/I2P)
	 *
	 * @return true if the address is a hidden address
	 */
	public boolean isHidden()
	{
		return type == TOR || type == I2P;
	}

	/**
	 * Checks if the PeerAddress is an external address (that is, something that can be connected to from outside a LAN).
	 *
	 * @return true if external address
	 */
	public boolean isExternal()
	{
		return type == TOR || type == I2P ||
				(type == IPV4 && IP.isPublicIp(((InetSocketAddress) socketAddress).getHostString()));
	}

	public boolean isLAN()
	{
		return type == IPV4 && IP.isLanIp(((InetSocketAddress) socketAddress).getHostString());
	}

	public boolean isHostname()
	{
		return type == HOSTNAME;
	}

	private static boolean isInvalidIpAddress(String address)
	{
		if (address == null)
		{
			return true;
		}

		var octets = address.split("\\.");

		if (octets.length != 4)
		{
			return true;
		}

		try
		{
			return Arrays.stream(octets)
					.filter(not(s -> s.length() > 1 && s.startsWith("0")))
					.map(Integer::parseInt)
					.filter(i -> (i >= 0 && i <= 255))
					.count() != 4 || !IP.isRoutableIp(address);
		}
		catch (NumberFormatException e)
		{
			return true;
		}
	}

	private static boolean isInvalidHostname(String hostname)
	{
		return !(hostname != null && hostname.length() <= 253 && HOSTNAME_OK_PATTERN.matcher(hostname).matches());
	}

	@Override
	public String toString()
	{
		return "PeerAddress{" +
				"socketAddress=" + socketAddress +
				", type=" + type +
				'}';
	}
}
