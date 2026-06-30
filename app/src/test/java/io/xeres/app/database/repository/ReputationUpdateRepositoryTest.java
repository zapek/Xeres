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

import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.database.model.reputation.ReputationUpdateFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ReputationUpdateRepositoryTest
{
	@Autowired
	private ReputationUpdateRepository reputationUpdateRepository;

	@Autowired
	private LocationRepository locationRepository;

	@Autowired
	private ProfileRepository profileRepository;

	@Test
	void CRUD_Success()
	{
		var profile1 = ProfileFakes.createFreshProfile("test1", 1);
		var profile2 = ProfileFakes.createFreshProfile("test2", 2);
		var profile3 = ProfileFakes.createFreshProfile("test3", 3);

		profileRepository.save(profile1);
		profileRepository.save(profile2);
		profileRepository.save(profile3);

		var location1 = LocationFakes.createFreshLocation("loc1", profile1);
		var location2 = LocationFakes.createFreshLocation("loc2", profile2);
		var location3 = LocationFakes.createFreshLocation("loc3", profile3);

		locationRepository.save(location1);
		locationRepository.save(location2);
		locationRepository.save(location3);

		var update1 = ReputationUpdateFakes.createFreshReputationUpdate(location1);
		var update2 = ReputationUpdateFakes.createFreshReputationUpdate(location2);
		var update3 = ReputationUpdateFakes.createFreshReputationUpdate(location3);

		var savedUpdate = reputationUpdateRepository.save(update1);
		reputationUpdateRepository.save(update2);
		reputationUpdateRepository.save(update3);

		var updates = reputationUpdateRepository.findAll();
		assertNotNull(updates);
		assertEquals(3, updates.size());

		var found = reputationUpdateRepository.findByLocation(location1).orElse(null);

		assertNotNull(found);
		assertEquals(savedUpdate.getLastUpdated(), found.getLastUpdated());

		var newTime = Instant.now();
		found.setLastUpdated(newTime);

		var updatedUpdate = reputationUpdateRepository.save(found);

		assertNotNull(updatedUpdate);
		assertEquals(newTime, updatedUpdate.getLastUpdated());

		reputationUpdateRepository.delete(found);

		var deleted = reputationUpdateRepository.findByLocation(location1);
		assertTrue(deleted.isEmpty());
	}

	@Test
	void findByLocation_Success()
	{
		var profile = ProfileFakes.createFreshProfile("test", 1);
		profileRepository.save(profile);

		var location = LocationFakes.createFreshLocation("test", profile);
		locationRepository.save(location);

		var update = ReputationUpdateFakes.createFreshReputationUpdate(location);
		reputationUpdateRepository.save(update);

		var found = reputationUpdateRepository.findByLocation(location);

		assertTrue(found.isPresent());
		assertEquals(update.getLastUpdated(), found.get().getLastUpdated());
	}

	@Test
	void findByLocation_NotFound()
	{
		var profile = ProfileFakes.createFreshProfile("test", 1);
		profileRepository.save(profile);

		var location = LocationFakes.createFreshLocation("test", profile);
		locationRepository.save(location);

		// Create a different location that shouldn't have a reputation update
		var location2 = LocationFakes.createFreshLocation("test2", profile);
		locationRepository.save(location2);

		var found = reputationUpdateRepository.findByLocation(location2);

		assertTrue(found.isEmpty());
	}
}
