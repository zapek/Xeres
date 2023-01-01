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

package io.xeres.app.net.peer;

public enum ConnectionType
{
	TCP_INCOMING("incoming"),
	TCP_OUTGOING("outgoing"),
	TOR_OUTGOING("Tor"),
	I2P_OUTGOING("I2P");

	private final String description;

	ConnectionType(String description)
	{
		this.description = description;
	}

	public String getDescription()
	{
		return description;
	}
}
