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

package io.xeres.app.database.model.connection;

import io.xeres.common.dto.connection.ConnectionDTO;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectionMapperTest
{
	@Test
	void ConnectionMapper_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ConnectionMapper.class);
	}

	@Test
	void ConnectionMapper_toDTO_OK()
	{
		var connection = ConnectionFakes.createConnection();
		var connectionDTO = ConnectionMapper.toDTO(connection);

		assertEquals(connection.getId(), connectionDTO.id());
		assertEquals(connection.getAddress(), connectionDTO.address());
		assertEquals(connection.getLastConnected(), connectionDTO.lastConnected());
		assertEquals(connection.isExternal(), connectionDTO.external());
	}

	@Test
	void ConnectionMapper_fromDTO_OK()
	{
		var connectionDTO = new ConnectionDTO(
				1L,
				"85.11.11.12",
				Instant.now(),
				true
		);

		var connection = ConnectionMapper.fromDTO(connectionDTO);

		assertEquals(connectionDTO.id(), connection.getId());
		assertEquals(connectionDTO.address(), connection.getAddress());
		assertEquals(connectionDTO.external(), connection.isExternal());
		assertEquals(connectionDTO.lastConnected(), connection.getLastConnected());
	}
}
