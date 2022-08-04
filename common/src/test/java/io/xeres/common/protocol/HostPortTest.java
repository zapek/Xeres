/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.common.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HostPortTest
{
	@Test
	void HostPort_Parse_OK()
	{
		var host = "hey.foobar.com";
		var port = 1234;

		var hostPort = HostPort.parse(host + ":" + port);

		assertEquals(host, hostPort.host());
		assertEquals(port, hostPort.port());
	}

	@Test
	void HostPort_Parse_WrongFormat()
	{
		var host = "hey.foobar.com";

		assertThrows(IllegalArgumentException.class, () -> HostPort.parse(host), "Input is not in \"host:port\" format: hey.foobar.com");
	}

	@Test
	void HostPort_Parse_MissingHost()
	{
		var host = "";

		assertThrows(IllegalArgumentException.class, () -> HostPort.parse(host), "Host is missing");
	}

	@Test
	void HostPort_Parse_MissingPort()
	{
		var host = "hey.foobar.com";

		assertThrows(IllegalArgumentException.class, () -> HostPort.parse(host + ":"), "Port is not a number: ");
	}

	@Test
	void HostPort_Parse_PortNotANumber()
	{
		var host = "hey.foobar.com";
		var port = "plop";

		assertThrows(IllegalArgumentException.class, () -> HostPort.parse(host + ":" + port), "Port is not a number: " + port);
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 65536})
	void HostPort_Parse_PortOutOfRange(int port)
	{
		var host = "hey.foobar.com";

		assertThrows(IllegalArgumentException.class, () -> HostPort.parse(host + ":" + port), "Port is out of range: " + port);
	}
}
