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

package io.xeres.app.web.api.controller.profile;

import io.xeres.app.crypto.rsid.RSId;
import io.xeres.app.crypto.rsid.RSIdArmor;
import io.xeres.app.crypto.rsid.RSIdFakes;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.service.ProfileService;
import io.xeres.app.web.api.controller.AbstractControllerTest;
import io.xeres.common.rest.profile.RsIdRequest;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static io.xeres.common.rest.PathConfig.PROFILES_PATH;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = PROFILES_PATH;

	@MockBean
	private ProfileService profileService;

	@Test
	void ProfileController_FindProfileById_OK() throws Exception
	{
		var expected = ProfileFakes.createProfile("test", 1);

		when(profileService.findProfileById(expected.getId())).thenReturn(Optional.of(expected));

		mvc.perform(getJson(BASE_URL + "/" + expected.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(is(expected.getId()), Long.class))
				.andExpect(jsonPath("$.name", is(expected.getName())))
				.andExpect(jsonPath("$.pgpFingerprint", is(Base64.toBase64String(expected.getProfileFingerprint().getBytes()))))
				.andExpect(jsonPath("$.pgpPublicKeyData", is(Base64.toBase64String(expected.getPgpPublicKeyData()))))
				.andExpect(jsonPath("$.accepted").value(is(expected.isAccepted()), Boolean.class))
				.andExpect(jsonPath("$.trust", is(expected.getTrust().name())));

		verify(profileService).findProfileById(expected.getId());
	}

	@Test
	void ProfileController_FindProfileById_NotFound() throws Exception
	{
		long ID = 2L;

		when(profileService.findProfileById(ID)).thenReturn(Optional.empty());

		mvc.perform(getJson(BASE_URL + "/" + ID))
				.andExpect(status().isNotFound());

		verify(profileService).findProfileById(ID);
	}

	@Test
	void ProfileController_FindProfileByName_OK() throws Exception
	{
		var expected = ProfileFakes.createProfile("test", 1);

		when(profileService.findProfileByName(expected.getName())).thenReturn(Optional.of(expected));

		mvc.perform(getJson(BASE_URL + "?name=" + expected.getName()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].id").value(is(expected.getId()), Long.class))
				.andExpect(jsonPath("$.[0].name", is(expected.getName())));

		verify(profileService).findProfileByName(expected.getName());
	}

	@Test
	void ProfileController_FindProfileByName_NotFound() throws Exception
	{
		String NAME = "inexistant";

		when(profileService.findProfileByName(NAME)).thenReturn(Optional.empty());

		mvc.perform(getJson(BASE_URL + "?name=" + NAME))
				.andExpect(status().isOk())
				.andExpect(content().string("[]"));

		verify(profileService).findProfileByName(NAME);
	}

	@Test
	void ProfileController_FindProfiles_OK() throws Exception
	{
		var profile1 = ProfileFakes.createProfile("test1", 1);
		var profile2 = ProfileFakes.createProfile("test2", 2);
		var profiles = List.of(profile1, profile2);

		when(profileService.getAllProfiles()).thenReturn(profiles);

		mvc.perform(getJson(BASE_URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].id").value(is(profiles.get(0).getId()), Long.class));

		verify(profileService).getAllProfiles();
	}

	@Test
	void ProfileController_CreateProfile_OK() throws Exception
	{
		var expected = ProfileFakes.createProfile("test", 1);
		var profileRequest = new RsIdRequest(RSIdArmor.getArmored(RSIdFakes.createShortInvite()));

		when(profileService.getProfileFromRSId(any(RSId.class))).thenReturn(expected);
		when(profileService.createOrUpdateProfile(any(Profile.class))).thenReturn(Optional.of(expected));

		mvc.perform(postJson(BASE_URL, profileRequest))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + PROFILES_PATH + "/" + expected.getId()));

		verify(profileService).createOrUpdateProfile(any(Profile.class));
	}

	@Test
	void ProfileController_CreateProfile_MissingCertificate() throws Exception
	{
		var profileRequest = new RsIdRequest(null);

		mvc.perform(postJson(BASE_URL, profileRequest))
				.andExpect(status().isBadRequest());
	}

	@Test
	void ProfileController_CreateProfile_BrokenCertificate() throws Exception
	{
		var profileRequest = new RsIdRequest("foo");

		mvc.perform(postJson(BASE_URL, profileRequest))
				.andExpect(status().isBadRequest());
	}

	@Test
	void ProfileController_DeleteProfile_OK() throws Exception
	{
		long ID = 2;

		mvc.perform(delete(BASE_URL + "/" + ID))
				.andExpect(status().isNoContent());

		verify(profileService).deleteProfile(ID);
	}

	@Test
	void ProfileController_DeleteProfile_NotFound() throws Exception
	{
		long ID = 2;
		var profile = ProfileFakes.createProfile("test", ID);

		doThrow(NoSuchElementException.class).when(profileService).deleteProfile(ID);

		mvc.perform(delete(BASE_URL + "/" + ID))
				.andExpect(status().isNotFound());

		verify(profileService).deleteProfile(ID);
	}

	@Test
	void ProfileController_DeleteProfile_Own() throws Exception
	{
		long ID = 1;

		mvc.perform(delete(BASE_URL + "/" + ID))
				.andExpect(status().isUnprocessableEntity());
	}
}
