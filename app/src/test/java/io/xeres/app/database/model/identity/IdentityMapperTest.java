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

package io.xeres.app.database.model.identity;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentityMapperTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(IdentityMapper.class);
	}

	@Test
	void toDTO_Success()
	{
		var identity = IdentityFakes.createOwn();
		var identityDTO = IdentityMapper.toDTO(identity);

		assertEquals(identity.getId(), identityDTO.id());
		assertEquals(identity.getName(), identityDTO.name());
		assertEquals(identity.getGxsId(), identityDTO.gxsId());
		assertEquals(identity.getPublished(), identityDTO.updated());
		assertEquals(identity.getType(), identityDTO.type());
		assertEquals(identity.hasImage(), identityDTO.hasImage());
	}
}
