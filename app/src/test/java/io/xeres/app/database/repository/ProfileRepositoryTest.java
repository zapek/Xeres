/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.profile.ProfileFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProfileRepositoryTest
{
	@Autowired
	private ProfileRepository profileRepository;

	@Test
	void CRUD_Success()
	{
		var profile1 = ProfileFakes.createFreshProfile("test1", 1);
		var profile2 = ProfileFakes.createFreshProfile("test2", 2);
		var profile3 = ProfileFakes.createFreshProfile("test3", 3);

		var savedProfile = profileRepository.save(profile1);
		profileRepository.save(profile2);
		profileRepository.save(profile3);

		var profiles = profileRepository.findAll();
		assertNotNull(profiles);
		assertEquals(3, profiles.size());

		var first = profileRepository.findById(profiles.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedProfile.getId(), first.getId());
		assertEquals(savedProfile.getName(), first.getName());

		first.setAccepted(false);

		var updatedProfile = profileRepository.save(first);

		assertNotNull(updatedProfile);
		assertEquals(first.getId(), updatedProfile.getId());
		assertFalse(updatedProfile.isAccepted());

		profileRepository.deleteById(first.getId());

		var deleted = profileRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}
}
