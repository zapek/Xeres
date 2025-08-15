/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.service;

import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.location.Availability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest
{
	@Mock
	private ProfileService profileService;

	@Mock
	private IdentityService identityService;

	@InjectMocks
	private ContactService contactService;

	@Test
	void getContacts_ShouldReturnCombinedList()
	{
		var profile = ProfileFakes.createProfile("Test Profile", 1L);
		profile.setAccepted(true);

		var identity = new IdentityGroupItem();
		identity.setId(2L);
		identity.setName("Test Identity");
		identity.setProfile(profile);

		when(profileService.getAllProfiles()).thenReturn(List.of(profile));
		when(identityService.getAll()).thenReturn(List.of(identity));

		var result = contactService.getContacts();

		assertEquals(2, result.size());
		assertTrue(result.stream().anyMatch(c -> c.name().equals("Test Profile")));
		assertTrue(result.stream().anyMatch(c -> c.name().equals("Test Identity")));
	}

	@Test
	void toContacts_WithIdentityList_ShouldConvertCorrectly()
	{
		var profile = ProfileFakes.createOwnProfile();
		profile.setAccepted(true);

		var identity = new IdentityGroupItem();
		identity.setId(2L);
		identity.setName("Test Identity");
		identity.setProfile(profile);

		var result = contactService.toContacts(List.of(identity));

		assertEquals(1, result.size());
		assertEquals("Test Identity", result.getFirst().name());
		assertEquals(1L, result.getFirst().profileId());
		assertEquals(2L, result.getFirst().identityId());
		assertTrue(result.getFirst().accepted());
	}

	@Test
	void toContact_WithProfile_ShouldConvertCorrectly()
	{
		var profile = ProfileFakes.createOwnProfile();
		profile.setAccepted(true);

		var result = contactService.toContact(profile);

		assertEquals(profile.getName(), result.name());
		assertEquals(1L, result.profileId());
		assertEquals(0L, result.identityId());
		assertTrue(result.accepted());
	}

	@Test
	void getAvailability_WithNullProfile_ShouldReturnOffline()
	{
		var identity = new IdentityGroupItem();
		identity.setName("Test Identity");

		var result = contactService.toContacts(List.of(identity));

		assertEquals(Availability.OFFLINE, result.getFirst().availability());
	}
}