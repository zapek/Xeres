/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

import static org.apache.commons.lang3.StringUtils.isBlank;

public record HostPort(String host, int port)
{
	public static HostPort parse(String hostPort)
	{
		var tokens = hostPort.split(":");
		int port;
		if (tokens.length != 2)
		{
			throw new IllegalArgumentException("Input is not in \"host:port\" format: " + hostPort);
		}

		try
		{
			port = Integer.parseInt(tokens[1]);
		}
		catch (NumberFormatException _)
		{
			throw new IllegalArgumentException("Port is not a number: " + tokens[1]);
		}

		if (port < 0 || port > 65535)
		{
			throw new IllegalArgumentException("Port is out of range: " + port);
		}

		if (isBlank(tokens[0]))
		{
			throw new IllegalArgumentException("Host is missing");
		}

		return new HostPort(tokens[0], port);
	}
}
