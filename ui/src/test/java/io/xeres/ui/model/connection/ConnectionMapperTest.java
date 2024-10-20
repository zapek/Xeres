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

package io.xeres.ui.model.connection;

import io.xeres.common.dto.connection.ConnectionDTOFakes;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectionMapperTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ConnectionMapper.class);
	}

	@Test
	void FromDTO_Success()
	{
		var dto = ConnectionDTOFakes.createConnectionDTO();

		var connection = ConnectionMapper.fromDTO(dto);

		assertEquals(dto.id(), connection.getId());
		assertEquals(dto.address(), connection.getAddress());
		assertEquals(dto.lastConnected(), connection.getLastConnected());
		assertEquals(dto.external(), connection.isExternal());
	}
}
