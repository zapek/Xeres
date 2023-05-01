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

import io.xeres.app.database.model.gxs.GxsClientUpdateFakes;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.testutils.IdFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GxsClientUpdateRepositoryTest
{
	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private GxsClientUpdateRepository gxsClientUpdateRepository;

	@Test
	void GxsClientUpdateRepository_CRUD_OK()
	{
		var profile = ProfileFakes.createProfile("profile1", 1);
		profile = profileRepository.save(profile);
		var location = LocationFakes.createLocation("location1", profile);

		profile.addLocation(location);
		profile = profileRepository.save(profile);

		var gxsClientUpdate1 = GxsClientUpdateFakes.createGxsClientUpdate(profile.getLocations().get(0), 200);
		var gxsClientUpdate2 = GxsClientUpdateFakes.createGxsClientUpdate(profile.getLocations().get(0), 201);
		var gxsClientUpdate3 = GxsClientUpdateFakes.createGxsClientUpdate(profile.getLocations().get(0), 202);

		var savedGxsClientUpdate1 = gxsClientUpdateRepository.save(gxsClientUpdate1);
		var savedGxsClientUpdate2 = gxsClientUpdateRepository.save(gxsClientUpdate2);
		gxsClientUpdateRepository.save(gxsClientUpdate3);

		var gxsClientUpdates = gxsClientUpdateRepository.findAll();
		assertNotNull(gxsClientUpdates);
		assertEquals(3, gxsClientUpdates.size());

		var first = gxsClientUpdateRepository.findById(gxsClientUpdates.get(0).getId()).orElse(null);
		assertNotNull(first);
		assertEquals(savedGxsClientUpdate1.getId(), first.getId());
		assertEquals(savedGxsClientUpdate1.getServiceType(), first.getServiceType());

		var second = gxsClientUpdateRepository.findByLocationAndServiceType(gxsClientUpdate2.getLocation(), gxsClientUpdate2.getServiceType()).orElse(null);
		assertNotNull(second);
		assertEquals(savedGxsClientUpdate2.getId(), second.getId());
		assertEquals(savedGxsClientUpdate2.getServiceType(), second.getServiceType());

		first.setServiceType(300);

		var updatedGxsClientUpdate = gxsClientUpdateRepository.save(first);

		assertNotNull(updatedGxsClientUpdate);
		assertEquals(first.getId(), updatedGxsClientUpdate.getId());
		assertEquals(300, updatedGxsClientUpdate.getServiceType());

		gxsClientUpdateRepository.deleteById(first.getId());

		var deleted = gxsClientUpdateRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void GxsClientUpdateRepository_CRUD_Messages_OK()
	{
		var profile = ProfileFakes.createProfile("profile1", 1);
		profile = profileRepository.save(profile);
		var location = LocationFakes.createLocation("location1", profile);

		profile.addLocation(location);
		profile = profileRepository.save(profile);

		var groupId1 = IdFakes.createGxsId();
		var time1 = "2007-12-03T10:15:30.00Z";
		var update1 = Instant.parse(time1);
		var groupId2 = IdFakes.createGxsId();
		var time2 = "2014-11-05T09:28:35.00Z";
		var update2 = Instant.parse(time2);
		var groupId3 = IdFakes.createGxsId();
		var time3 = "2021-01-01T14:45:00.00Z";
		var update3 = Instant.parse(time3);

		var gxsClientUpdate = GxsClientUpdateFakes.createGxsClientUpdateWithMessages(profile.getLocations().get(0), groupId1, update1, 200);
		gxsClientUpdate.addMessageUpdate(groupId2, update2);
		gxsClientUpdate.addMessageUpdate(groupId3, update3);

		var savedGxsClientUpdate = gxsClientUpdateRepository.save(gxsClientUpdate);

		var gxsClientUpdates = gxsClientUpdateRepository.findAll();
		assertNotNull(gxsClientUpdates);
		assertEquals(1, gxsClientUpdates.size());

		var first = gxsClientUpdateRepository.findById(gxsClientUpdates.get(0).getId()).orElse(null);
		assertNotNull(first);
		assertEquals(savedGxsClientUpdate.getId(), first.getId());
		assertEquals(savedGxsClientUpdate.getServiceType(), first.getServiceType());
		assertEquals(update1, first.getMessageUpdate(groupId1));
		assertEquals(update2, first.getMessageUpdate(groupId2));
		assertEquals(update3, first.getMessageUpdate(groupId3));

		first.removeMessageUpdate(groupId3);

		var updatedGxsClientUpdate = gxsClientUpdateRepository.save(first);

		assertNotNull(updatedGxsClientUpdate);

		assertNull(first.getMessageUpdate(groupId3));
		assertEquals(update1, first.getMessageUpdate(groupId1));
		assertEquals(update2, first.getMessageUpdate(groupId2));

		gxsClientUpdateRepository.deleteById(first.getId());

		var deleted = gxsClientUpdateRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}
}
