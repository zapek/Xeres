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

package io.xeres.app.database.model.connection;

import io.xeres.common.dto.connection.ConnectionDTO;

@SuppressWarnings("DuplicatedCode")
public final class ConnectionMapper
{
	private ConnectionMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ConnectionDTO toDTO(Connection connection)
	{
		if (connection == null)
		{
			return null;
		}

		return new ConnectionDTO(
				connection.getId(),
				connection.getAddress(),
				connection.getLastConnected(),
				connection.isExternal());
	}

	public static Connection fromDTO(ConnectionDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var connection = new Connection();
		connection.setId(dto.id());
		connection.setAddress(dto.address());
		connection.setExternal(dto.external());
		connection.setLastConnected(dto.lastConnected());
		return connection;
	}
}
