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

package io.xeres.app.database.model.profile;

import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.common.dto.profile.ProfileDTO;
import io.xeres.common.pgp.Trust;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileMapperTest
{
	@Test
	void ProfileMapper_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ProfileMapper.class);
	}

	@Test
	void ProfileMapper_toDTO_OK()
	{
		var profile = ProfileFakes.createProfile("test", 1);
		var profileDTO = ProfileMapper.toDTO(profile);

		assertEquals(profile.getId(), profileDTO.id());
		assertEquals(profile.getName(), profileDTO.name());
		assertEquals(profile.getPgpIdentifier(), Long.parseLong(profileDTO.pgpIdentifier()));
		assertArrayEquals(profile.getProfileFingerprint().getBytes(), profileDTO.pgpFingerprint());
		assertArrayEquals(profile.getPgpPublicKeyData(), profileDTO.pgpPublicKeyData());
		assertEquals(profile.isAccepted(), profileDTO.accepted());
		assertEquals(profile.getTrust(), profileDTO.trust());
	}

	@Test
	void ProfileMapper_toDeepDTO_OK()
	{
		var profile = ProfileFakes.createProfile("test", 1);
		profile.addLocation(LocationFakes.createLocation("foo", profile));

		var profileDTO = ProfileMapper.toDeepDTO(profile);

		assertEquals(profile.getId(), profileDTO.id());
		assertEquals(profile.getLocations().getFirst().getId(), profileDTO.locations().getFirst().id());
	}

	@Test
	void ProfileMapper_fromDTO_OK()
	{
		var profileDTO = new ProfileDTO(
				1L,
				"prout",
				"2",
				new byte[20],
				new byte[4],
				true,
				Trust.ULTIMATE,
				null
		);

		var profile = ProfileMapper.fromDTO(profileDTO);

		assertEquals(profileDTO.id(), profile.getId());
		assertEquals(profileDTO.name(), profile.getName());
		assertEquals(profileDTO.pgpIdentifier(), String.valueOf(profile.getPgpIdentifier()));
		assertArrayEquals(profileDTO.pgpFingerprint(), profile.getProfileFingerprint().getBytes());
		assertArrayEquals(profileDTO.pgpPublicKeyData(), profile.getPgpPublicKeyData());
		assertEquals(profileDTO.accepted(), profile.isAccepted());
		assertEquals(profileDTO.trust(), profile.getTrust());
	}
}
