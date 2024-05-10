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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DNSTest
{
	/**
	 * This test verifies that myip.opendns.com works for finding one's own IP when UPNP is not working.
	 * It also tests that akamai works, in case opendns is removed, and we need to fall back to something else.
	 * It only runs on my machine because of the chicken & egg problem on knowing one's own IP.
	 *
	 * @throws IOException
	 */
	@Test
	@EnabledIfEnvironmentVariable(named = "COMPUTERNAME", matches = "B650")
	void DNS_OK() throws IOException
	{
		var ip1 = DNS.resolve("myip.opendns.com", "208.67.222.222"); // resolver1.opendns.com
		var ip2 = DNS.resolve("myip.opendns.com", "208.67.220.220"); // resolver2.opendns.com
		var ip3 = DNS.resolve("myip.opendns.com", "208.67.222.220"); // resolver3.opendns.com
		var ip4 = DNS.resolve("myip.opendns.com", "208.67.220.222"); // resolver4.opendns.com
		var ip5 = DNS.resolve("whoami.akamai.net", "193.108.88.1"); // ns1-1.akamaitech.net
		var realIp = InetAddress.getByName("core.zapek.com");
		assertEquals(realIp, ip1);
		assertEquals(realIp, ip2);
		assertEquals(realIp, ip3);
		assertEquals(realIp, ip4);
		assertEquals(realIp, ip5);
	}
}
