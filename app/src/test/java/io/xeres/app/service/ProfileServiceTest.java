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

package io.xeres.app.service;

import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.database.repository.ProfileRepository;
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
	private PrefsService prefsService;

	@Mock
	private ProfileRepository profileRepository;

	@InjectMocks
	private ProfileService profileService;

	@BeforeAll
	static void setup()
	{
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	void ProfileService_GenerateProfileKeys_OK()
	{
		String NAME = "test";

		when(prefsService.getSecretProfileKey()).thenReturn(null);

		assertTrue(profileService.generateProfileKeys(NAME));

		verify(prefsService).getSecretProfileKey();

		ArgumentCaptor<Profile> profile = ArgumentCaptor.forClass(Profile.class);
		verify(profileRepository).save(profile.capture());
		assertTrue(profile.getValue().getName().startsWith(NAME));
		verify(prefsService).saveSecretProfileKey(any(byte[].class));
	}

	@Test
	void ProfileService_GenerateProfileKeys_AlreadyExists_Fail()
	{
		String NAME = "test";

		when(prefsService.getSecretProfileKey()).thenReturn(new byte[]{1});

		assertThatThrownBy(() -> profileService.generateProfileKeys(NAME))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already exists");

		verify(prefsService).getSecretProfileKey();
		verify(profileRepository, times(0)).save(any(Profile.class));
		verify(prefsService, times(0)).saveSecretProfileKey(any(byte[].class));
	}

	@Test
	void ProfileService_GenerateProfileKeys_KeyIdTooShort_Fail()
	{
		String NAME = "";

		when(prefsService.getSecretProfileKey()).thenReturn(null);

		assertThatThrownBy(() -> profileService.generateProfileKeys(NAME))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("too short");

		verify(prefsService).getSecretProfileKey();
		verify(profileRepository, times(0)).save(any(Profile.class));
		verify(prefsService, times(0)).saveSecretProfileKey(any(byte[].class));
	}

	@Test
	void ProfileService_GenerateProfileKeys_KeyIdTooLong_Fail()
	{
		String NAME = "12345678900987654321123456789098765432120987676543432123456798765";

		when(prefsService.getSecretProfileKey()).thenReturn(null);

		assertThatThrownBy(() -> profileService.generateProfileKeys(NAME))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("too long");

		verify(prefsService).getSecretProfileKey();
		verify(profileRepository, times(0)).save(any(Profile.class));
		verify(prefsService, times(0)).saveSecretProfileKey(any(byte[].class));
	}

	@Test
	void ProfileService_CreateOrUpdateProfile_Update_OK()
	{
		Profile first = ProfileFakes.createProfile("first", 1);
		first.addLocation(LocationFakes.createLocation("first location", first));

		Profile second = ProfileFakes.createProfile("first", 1);
		second.addLocation(LocationFakes.createLocation("second location", second));

		when(profileRepository.findByProfileFingerprint(any(ProfileFingerprint.class))).thenReturn(Optional.of(first));
		when(profileRepository.save(any(Profile.class))).thenAnswer(mock -> mock.getArguments()[0]);

		Profile updated = profileService.createOrUpdateProfile(second).orElseThrow();

		assertEquals(2, updated.getLocations().size());

		// XXX: add the case where we "update" an existing location, not just add
	}
}
