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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.ProfileFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class LocationRepositoryTest
{
	@Autowired
	private ProfileRepository profileRepository;
	@Autowired
	private LocationRepository locationRepository;

	@Test
	void CRUD_Success()
	{
		var profile = ProfileFakes.createFreshProfile("test", 1);

		profile = profileRepository.save(profile);

		var location1 = LocationFakes.createFreshLocation("test1", profile);
		var location2 = LocationFakes.createFreshLocation("test2", profile);
		var location3 = LocationFakes.createFreshLocation("test3", profile);

		profile.addLocation(location1);
		profile.addLocation(location2);
		profile.addLocation(location3);

		profileRepository.save(profile);

		var locations = locationRepository.findAll();
		assertNotNull(locations);
		assertEquals(3, locations.size());

		var first = locationRepository.findById(locations.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(locations.getFirst().getId(), first.getId());
		assertEquals(locations.getFirst().getName(), first.getName());

		first.setConnected(true);

		var updatedLocation = locationRepository.save(first);

		assertNotNull(updatedLocation);
		assertEquals(first.getId(), updatedLocation.getId());
		assertTrue(updatedLocation.isConnected());

		locationRepository.deleteById(first.getId());

		var deleted = locationRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());

		profileRepository.deleteById(profile.getId());
		deleted = locationRepository.findById(location2.getId());
		assertTrue(deleted.isEmpty());
	}
}
