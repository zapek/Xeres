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

package io.xeres.app.database.model.location;

import io.xeres.app.database.model.connection.ConnectionMapper;
import io.xeres.common.dto.location.LocationDTO;
import io.xeres.common.id.LocationId;

import java.util.ArrayList;

@SuppressWarnings("DuplicatedCode")
public final class LocationMapper
{
	private LocationMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static LocationDTO toDTO(Location location)
	{
		if (location == null)
		{
			return null;
		}

		return new LocationDTO(
				location.getId(),
				location.getName(),
				location.getLocationId().getBytes(),
				null,
				new ArrayList<>(),
				location.isConnected(),
				location.getLastConnected(),
				location.getAvailability()
		);
	}

	public static LocationDTO toDeepDTO(Location location)
	{
		if (location == null)
		{
			return null;
		}
		var locationDTO = toDTO(location);

		locationDTO.connections().addAll(location.getConnections().stream()
				.map(ConnectionMapper::toDTO)
				.toList());

		return locationDTO;
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
		location.setAvailability(dto.availability());
		return location;
	}
}
