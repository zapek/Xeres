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

package io.xeres.ui.model.profile;

import io.xeres.common.dto.profile.ProfileDTOFakes;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileMapperTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ProfileMapper.class);
	}

	@Test
	void FromDTO_Success()
	{
		var dto = ProfileDTOFakes.create();

		var profile = ProfileMapper.fromDTO(dto);

		assertEquals(dto.id(), profile.getId());
		assertEquals(dto.name(), profile.getName());
		assertEquals(Long.parseLong(dto.pgpIdentifier()), profile.getPgpIdentifier());
		assertArrayEquals(dto.pgpFingerprint(), profile.getProfileFingerprint().getBytes());
		assertArrayEquals(dto.pgpPublicKeyData(), profile.getPgpPublicKeyData());
		assertEquals(dto.accepted(), profile.isAccepted());
		assertEquals(dto.trust(), profile.getTrust());
	}

	@Test
	void FromDeepDTO_Success()
	{
		var dto = ProfileDTOFakes.create();

		var profile = ProfileMapper.fromDeepDTO(dto);

		assertEquals(dto.id(), profile.getId());
		assertEquals(dto.name(), profile.getName());
		assertEquals(Long.parseLong(dto.pgpIdentifier()), profile.getPgpIdentifier());
		assertArrayEquals(dto.pgpFingerprint(), profile.getProfileFingerprint().getBytes());
		assertArrayEquals(dto.pgpPublicKeyData(), profile.getPgpPublicKeyData());
		assertEquals(dto.accepted(), profile.isAccepted());
		assertEquals(dto.trust(), profile.getTrust());
		assertEquals(dto.locations().size(), profile.getLocations().size());
		assertEquals(dto.locations().getFirst().id(), profile.getLocations().getFirst().getId());
	}
}
