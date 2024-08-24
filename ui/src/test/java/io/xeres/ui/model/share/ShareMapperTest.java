/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.ui.model.share;

import io.xeres.common.dto.share.ShareDTOFakes;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShareMapperTest
{
	@Test
	void ShareMapper_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ShareMapper.class);
	}

	@Test
	void ShareMapper_fromDTO_OK()
	{
		var dto = ShareDTOFakes.createShareDTO();

		var share = ShareMapper.fromDTO(dto);

		assertEquals(dto.id(), share.getId());
		assertEquals(dto.name(), share.getName());
		assertEquals(dto.path(), share.getPath());
		assertEquals(dto.searchable(), share.isSearchable());
		assertEquals(dto.browsable(), share.getBrowsable());
		assertEquals(dto.lastScanned(), share.getLastScanned());
	}
}
