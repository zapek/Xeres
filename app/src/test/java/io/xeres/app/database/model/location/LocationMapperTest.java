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

package io.xeres.app.database.model.location;

import io.xeres.app.database.model.connection.ConnectionFakes;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.common.dto.location.LocationDTO;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationMapperTest
{
	@Test
	void LocationMapper_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(LocationMapper.class);
	}

	@Test
	void LocationMapper_toDTO_OK()
	{
		var location = LocationFakes.createLocation("test", ProfileFakes.createProfile("test", 1));
		var locationDTO = LocationMapper.toDTO(location);

		assertEquals(location.getId(), locationDTO.id());
		assertEquals(location.getName(), locationDTO.name());
		assertArrayEquals(location.getLocationId().getBytes(), locationDTO.locationIdentifier());
		assertEquals(location.isConnected(), locationDTO.connected());
		assertEquals(location.getLastConnected(), locationDTO.lastConnected());
	}

	@Test
	void LocationMapper_toDeepDTO_OK()
	{
		var location = LocationFakes.createLocation("test", ProfileFakes.createProfile("test", 1));
		location.addConnection(ConnectionFakes.createConnection());

		var locationDTO = LocationMapper.toDeepDTO(location);

		assertEquals(location.getId(), locationDTO.id());
		assertEquals(location.getConnections().get(0).getAddress(), locationDTO.connections().get(0).address());
	}

	@Test
	void LocationMapper_fromDTO_OK()
	{
		var locationDTO = new LocationDTO(
				1L,
				"test",
				new byte[16],
				"foo",
				null,
				true,
				Instant.now()
		);

		var location = LocationMapper.fromDTO(locationDTO);

		assertEquals(locationDTO.id(), location.getId());
		assertEquals(locationDTO.name(), location.getName());
		assertArrayEquals(locationDTO.locationIdentifier(), location.getLocationId().getBytes());
		assertEquals(locationDTO.connected(), location.isConnected());
		assertEquals(locationDTO.lastConnected(), location.getLastConnected());
	}
}
