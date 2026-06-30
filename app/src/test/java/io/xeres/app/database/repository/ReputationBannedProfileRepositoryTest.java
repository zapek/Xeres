/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.reputation.ReputationBannedProfileFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ReputationBannedProfileRepositoryTest
{
	@Autowired
	private ReputationBannedProfileRepository reputationBannedProfileRepository;

	@Test
	void CRUD_Success()
	{
		var bannedProfile1 = ReputationBannedProfileFakes.createReputationBannedProfile();
		var bannedProfile2 = ReputationBannedProfileFakes.createReputationBannedProfile();
		var bannedProfile3 = ReputationBannedProfileFakes.createReputationBannedProfile();

		var savedBannedProfile = reputationBannedProfileRepository.save(bannedProfile1);
		reputationBannedProfileRepository.save(bannedProfile2);
		reputationBannedProfileRepository.save(bannedProfile3);

		var bannedProfiles = reputationBannedProfileRepository.findAll();
		assertNotNull(bannedProfiles);
		assertEquals(3, bannedProfiles.size());

		var found = reputationBannedProfileRepository.findByPgpIdentifier(bannedProfile1.getPgpIdentifier()).orElse(null);

		assertNotNull(found);
		assertEquals(savedBannedProfile.getId(), found.getId());
		assertEquals(bannedProfile1.getPgpIdentifier(), found.getPgpIdentifier());

		var originalLastUsed = found.getLastUsed();
		found.updateLastUsed();

		var updatedBannedProfile = reputationBannedProfileRepository.save(found);

		assertNotNull(updatedBannedProfile);
		assertEquals(savedBannedProfile.getId(), updatedBannedProfile.getId());
		assertEquals(bannedProfile1.getPgpIdentifier(), updatedBannedProfile.getPgpIdentifier());
		assertTrue(updatedBannedProfile.getLastUsed().isAfter(originalLastUsed));

		reputationBannedProfileRepository.delete(found);

		var deleted = reputationBannedProfileRepository.findByPgpIdentifier(1L);
		assertTrue(deleted.isEmpty());
	}

	@Test
	void findByPgpIdentifier_Success()
	{
		var pgpId = 12345L;
		var bannedProfile = ReputationBannedProfileFakes.createReputationBannedProfile(pgpId);

		reputationBannedProfileRepository.save(bannedProfile);

		var found = reputationBannedProfileRepository.findByPgpIdentifier(pgpId);

		assertTrue(found.isPresent());
		assertEquals(pgpId, found.get().getPgpIdentifier());
	}

	@Test
	void findByPgpIdentifier_NotFound()
	{
		var pgpId = 99999L;

		var found = reputationBannedProfileRepository.findByPgpIdentifier(pgpId);

		assertTrue(found.isEmpty());
	}
}
