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

package io.xeres.ui.model.location;

import io.xeres.common.dto.location.LocationDTO;
import io.xeres.common.id.LocationId;
import io.xeres.ui.model.connection.ConnectionMapper;

@SuppressWarnings("DuplicatedCode")
public final class LocationMapper
{
	private LocationMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Location fromDTO(LocationDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var location = new Location();
		location.setId(dto.id());
		location.setName(dto.name());
		location.setLocationId(new LocationId(dto.locationIdentifier()));
		location.setConnected(dto.connected());
		location.setLastConnected(dto.lastConnected());

		return location;
	}

	public static Location fromDeepDTO(LocationDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var location = fromDTO(dto);

		location.getConnections().addAll(dto.connections().stream()
				.map(ConnectionMapper::fromDTO)
				.toList());

		return location;
	}
}
