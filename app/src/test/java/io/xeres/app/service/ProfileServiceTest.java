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

package io.xeres.app.service;

import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.database.repository.ProfileRepository;
import io.xeres.app.service.notification.contact.ContactNotificationService;
import io.xeres.common.dto.profile.ProfileConstants;
import io.xeres.common.id.ProfileFingerprint;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Security;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ProfileServiceTest
{
	@Mock
	private SettingsService settingsService;

	@Mock
	private ProfileRepository profileRepository;

	@Mock
	private ContactNotificationService contactNotificationService;

	@InjectMocks
	private ProfileService profileService;

	@BeforeAll
	static void setup()
	{
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	void GenerateProfileKeys_Success()
	{
		var name = "test";

		assertEquals(ResourceCreationState.CREATED, profileService.generateProfileKeys(name));

		var profile = ArgumentCaptor.forClass(Profile.class);
		verify(profileRepository).save(profile.capture());
		assertTrue(profile.getValue().getName().startsWith(name));
		verify(settingsService).saveSecretProfileKey(any(byte[].class));
	}

	@Test
	void GenerateProfileKeys_AlreadyExists_Failure()
	{
		var name = "test";

		when(profileRepository.findById(ProfileConstants.OWN_PROFILE_ID)).thenReturn(Optional.of(ProfileFakes.createProfile()));

		assertEquals(ResourceCreationState.ALREADY_EXISTS, profileService.generateProfileKeys(name));

		verify(profileRepository, times(0)).save(any(Profile.class));
		verify(settingsService, times(0)).saveSecretProfileKey(any(byte[].class));
	}

	@Test
	void GenerateProfileKeys_KeyIdTooShort_Failure()
	{
		var name = "";

		assertThatThrownBy(() -> profileService.generateProfileKeys(name))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("too short");

		verify(profileRepository, times(0)).save(any(Profile.class));
		verify(settingsService, times(0)).saveSecretProfileKey(any(byte[].class));
	}

	@Test
	void GenerateProfileKeys_KeyIdTooLong_Failure()
	{
		var name = "12345678900987654321123456789098765432120987676543432123456798765";

		assertThatThrownBy(() -> profileService.generateProfileKeys(name))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("too long");

		verify(profileRepository, times(0)).save(any(Profile.class));
		verify(settingsService, times(0)).saveSecretProfileKey(any(byte[].class));
	}

	@Test
	void CreateOrUpdateProfile_Update_Success()
	{
		var first = ProfileFakes.createProfile("first", 1);
		first.addLocation(LocationFakes.createLocation("first location", first));

		var second = ProfileFakes.createProfile("first", 1);
		second.addLocation(LocationFakes.createLocation("second location", second));

		when(profileRepository.findByProfileFingerprint(any(ProfileFingerprint.class))).thenReturn(Optional.of(first));
		when(profileRepository.save(any(Profile.class))).thenAnswer(mock -> mock.getArguments()[0]);

		var updated = profileService.createOrUpdateProfile(second);

		assertEquals(2, updated.getLocations().size());

		// XXX: add the case where we "update" an existing location, not just add
	}
}
