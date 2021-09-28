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

package io.xeres.app.net.upnp;

import java.util.Objects;

class PortMapping
{
	final int port;
	final Protocol protocol;

	PortMapping(int port, Protocol protocol)
	{
		this.port = port;
		this.protocol = protocol;
	}

	public int getPort()
	{
		return port;
	}

	public Protocol getProtocol()
	{
		return protocol;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PortMapping that = (PortMapping) o;
		return port == that.port &&
				protocol == that.protocol;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(port, protocol);
	}
}
