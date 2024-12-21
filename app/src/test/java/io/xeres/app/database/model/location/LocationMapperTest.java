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

import io.xeres.app.database.model.connection.ConnectionFakes;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.common.dto.location.LocationDTO;
import io.xeres.common.location.Availability;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationMapperTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(LocationMapper.class);
	}

	@Test
	void toDTO_Success()
	{
		var location = LocationFakes.createLocation("test", ProfileFakes.createProfile("test", 1));
		var locationDTO = LocationMapper.toDTO(location);

		assertEquals(location.getId(), locationDTO.id());
		assertEquals(location.getName(), locationDTO.name());
		assertArrayEquals(location.getLocationIdentifier().getBytes(), locationDTO.locationIdentifier());
		assertEquals(location.isConnected(), locationDTO.connected());
		assertEquals(location.getLastConnected(), locationDTO.lastConnected());
	}

	@Test
	void toDeepDTO_Success()
	{
		var location = LocationFakes.createLocation("test", ProfileFakes.createProfile("test", 1));
		location.addConnection(ConnectionFakes.createConnection());

		var locationDTO = LocationMapper.toDeepDTO(location);

		assertEquals(location.getId(), locationDTO.id());
		assertEquals(location.getConnections().getFirst().getAddress(), locationDTO.connections().getFirst().address());
	}

	@Test
	void fromDTO_Success()
	{
		var locationDTO = new LocationDTO(
				1L,
				"test",
				new byte[16],
				"foo",
				null,
				true,
				Instant.now(),
				Availability.AVAILABLE,
				"Xeres 2.3.2"
		);

		var location = LocationMapper.fromDTO(locationDTO);

		assertEquals(locationDTO.id(), location.getId());
		assertEquals(locationDTO.name(), location.getName());
		assertArrayEquals(locationDTO.locationIdentifier(), location.getLocationIdentifier().getBytes());
		assertEquals(locationDTO.connected(), location.isConnected());
		assertEquals(locationDTO.lastConnected(), location.getLastConnected());
	}
}
