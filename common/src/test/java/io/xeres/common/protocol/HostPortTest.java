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

package io.xeres.common.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HostPortTest
{
	@Test
	void Parse_Success()
	{
		var host = "hey.foobar.com";
		var port = 1234;

		var hostPort = HostPort.parse(host + ":" + port);

		assertEquals(host, hostPort.host());
		assertEquals(port, hostPort.port());
	}

	@Test
	void Parse_WrongFormat_ThrowsException()
	{
		var host = "hey.foobar.com";

		assertThrows(IllegalArgumentException.class, () -> HostPort.parse(host), "Input is not in \"host:port\" format: hey.foobar.com");
	}

	@Test
	void Parse_MissingHost_ThrowsException()
	{
		var host = "";

		assertThrows(IllegalArgumentException.class, () -> HostPort.parse(host), "Host is missing");
	}

	@Test
	void Parse_MissingPort_ThrowsException()
	{
		var host = "hey.foobar.com";

		assertThrows(IllegalArgumentException.class, () -> HostPort.parse(host + ":"), "Port is not a number: ");
	}

	@Test
	void Parse_PortNotANumber_ThrowsException()
	{
		var host = "hey.foobar.com";
		var port = "plop";

		assertThrows(IllegalArgumentException.class, () -> HostPort.parse(host + ":" + port), "Port is not a number: " + port);
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 65536})
	void Parse_PortOutOfRange_ThrowsException(int port)
	{
		var host = "hey.foobar.com";

		assertThrows(IllegalArgumentException.class, () -> HostPort.parse(host + ":" + port), "Port is out of range: " + port);
	}
}
