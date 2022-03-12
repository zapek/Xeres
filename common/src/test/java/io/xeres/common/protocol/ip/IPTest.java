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

package io.xeres.common.protocol.ip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IPTest
{
	@Test
	void IP_GetFreeLocalPort_OK()
	{
		var port = IP.getFreeLocalPort();

		assertTrue(port >= 1025 && port <= 32766);
	}

	@Test
	void IP_GetLocalIPAddress_OK()
	{
		var ip = IP.getLocalIpAddress();

		assertNotNull(ip);
	}

	@Test
	void IP_IsLanIP_OK()
	{
		assertTrue(IP.isLanIp("10.0.0.0"));
		assertTrue(IP.isLanIp("10.255.255.255"));

		assertTrue(IP.isLanIp("172.16.0.0"));
		assertTrue(IP.isLanIp("172.31.255.255"));

		assertTrue(IP.isLanIp("192.168.0.0"));
		assertTrue(IP.isLanIp("192.168.255.255"));

		assertTrue(IP.isLanIp("192.168.1.5"));
		assertTrue(IP.isLanIp("172.16.0.5"));
		assertTrue(IP.isLanIp("10.0.0.5"));
	}

	@Test
	void IP_IsLanIP_Fail()
	{
		assertFalse(IP.isLanIp("85.1.2.78"));
	}

	@Test
	void IP_IsLanIP_Empty_Fail()
	{
		assertFalse(IP.isLanIp(""));
	}

	@Test
	void IP_IsLanIP_Null_Fail()
	{
		assertFalse(IP.isLanIp(null));
	}

	@Test
	void IP_IsPublicIP_OK()
	{
		assertTrue(IP.isPublicIp("85.1.2.78"));
	}

	@Test
	void IP_IsPublicIP_Fail()
	{
		assertFalse(IP.isPublicIp("192.168.1.5"));
	}

	@Test
	void IP_IsPublicIP_Empty_Fail()
	{
		assertFalse(IP.isPublicIp(""));
	}

	@Test
	void IP_IsPublicIP_Null_Fail()
	{
		assertFalse(IP.isPublicIp(null));
	}
}
