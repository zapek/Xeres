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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.database.model.profile.ProfileFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class LocationRepositoryTest
{
	@Autowired
	private LocationRepository locationRepository;

	@Test
	void LocationRepository_CRUD_OK()
	{
		Profile profile = ProfileFakes.createProfile("test", 1);

		Location location1 = LocationFakes.createLocation("test1", profile);
		Location location2 = LocationFakes.createLocation("test2", profile);
		Location location3 = LocationFakes.createLocation("test3", profile);

		Location savedLocation1 = locationRepository.save(location1);
		locationRepository.save(location2);
		locationRepository.save(location3);

		List<Location> locations = locationRepository.findAll();
		assertNotNull(locations);
		assertEquals(3, locations.size());

		Location first = locationRepository.findById(locations.get(0).getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedLocation1.getId(), first.getId());
		assertEquals(savedLocation1.getName(), first.getName());

		first.setConnected(true);

		Location updatedLocation = locationRepository.save(first);

		assertNotNull(updatedLocation);
		assertEquals(first.getId(), updatedLocation.getId());
		assertTrue(updatedLocation.isConnected());

		locationRepository.deleteById(first.getId());

		Optional<Location> deleted = locationRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}
}
