/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.common.protocol.dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;

public final class DNS
{
	private static final Duration TIMEOUT = Duration.ofSeconds(10);
	private static final int DNS_PORT = 53;

	private DNS()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * @param host      the host to resolve
	 * @param dnsServer the dns server to resolve against
	 * @return the IP address of the host
	 * @throws IOException failure to resolve
	 * @see <a href="https://stackoverflow.com/a/39375234/1811760">Stack Overflow</a>
	 */
	public static InetAddress resolve(String host, String dnsServer) throws IOException
	{
		var serverAddress = InetAddress.getByName(dnsServer);

		var request = new DnsRequest(host);

		var dnsFrame = request.toByteArray();

		try (var socket = new DatagramSocket())
		{
			socket.setSoTimeout((int) TIMEOUT.toMillis());

			var dnsReqPacket = new DatagramPacket(dnsFrame, dnsFrame.length, serverAddress, DNS_PORT);
			socket.send(dnsReqPacket);

			var buf = new byte[1024];
			var packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);

			var response = new DnsResponse(buf, request.getId());
			return response.getAddress();
		}
	}
}
