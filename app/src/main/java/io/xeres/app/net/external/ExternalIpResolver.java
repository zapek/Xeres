/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.app.net.external;

import io.xeres.common.protocol.dns.DNS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * A service to find the external IP address. Currently, uses the DNS protocol.
 * <p>Note: this is only used if UPNPService fails to find it itself.
 */
@Service
public class ExternalIpResolver
{
	private static final Logger log = LoggerFactory.getLogger(ExternalIpResolver.class);

	private static final String OPENDNS_OWN_IP_HOST = "myip.opendns.com";
	private static final String AKAMAI_OWN_IP_HOST = "whoami.akamai.net";

	private static final Map<String, String> RESOLVERS = Map.of(
			"208.67.222.222", OPENDNS_OWN_IP_HOST, // resolver1.opendns.com
			"208.67.220.220", OPENDNS_OWN_IP_HOST, // resolver2.opendns.com
			"208.67.222.220", OPENDNS_OWN_IP_HOST, // resolver3.opendns.com
			"208.67.220.222", OPENDNS_OWN_IP_HOST, // resolver4.opendns.com
			"193.108.88.1", AKAMAI_OWN_IP_HOST // ns1-1.akamaitech.net
	);

	/**
	 * Finds the external IP address.
	 *
	 * @return the IP address or null if not found
	 */
	public String find()
	{
		return findExternalIpAddressUsingDns();
	}

	private String findExternalIpAddressUsingDns()
	{
		var keys = new ArrayList<>(RESOLVERS.keySet());
		Collections.shuffle(keys);

		InetAddress externalIpAddress = null;

		for (String nameServer : keys)
		{
			try
			{
				externalIpAddress = DNS.resolve(RESOLVERS.get(nameServer), nameServer);
			}
			catch (IOException e)
			{
				// Log the error and try the next server
				log.error("Failed to resolve own IP using server {}: {}", nameServer, e.getMessage());
			}
		}

		if (externalIpAddress == null)
		{
			return null;
		}
		return externalIpAddress.getHostAddress();
	}
}
