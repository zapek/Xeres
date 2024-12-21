/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

import io.xeres.common.dto.location.LocationDTOFakes;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

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
	void FromDTO_Success()
	{
		var dto = LocationDTOFakes.create();

		var location = LocationMapper.fromDTO(dto);

		assertEquals(dto.id(), location.getId());
		assertArrayEquals(dto.locationIdentifier(), location.getLocationIdentifier().getBytes());
		assertEquals(dto.name(), location.getName());
		assertEquals(dto.connected(), location.isConnected());
		assertEquals(dto.lastConnected(), location.getLastConnected());
	}

	@Test
	void FromDeepDTO_Success()
	{
		var dto = LocationDTOFakes.create();

		var location = LocationMapper.fromDeepDTO(dto);

		assertEquals(dto.id(), location.getId());
		assertArrayEquals(dto.locationIdentifier(), location.getLocationIdentifier().getBytes());
		assertEquals(dto.name(), location.getName());
		//assertEquals(dto.hostname(), location.getHostname()); XXX
		assertEquals(dto.connected(), location.isConnected());
		assertEquals(dto.lastConnected(), location.getLastConnected());
		assertEquals(dto.connections().size(), location.getConnections().size());
		assertEquals(dto.connections().getFirst().id(), location.getConnections().getFirst().getId());
	}
}
