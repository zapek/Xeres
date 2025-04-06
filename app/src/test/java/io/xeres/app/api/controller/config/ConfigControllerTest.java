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

package io.xeres.app.api.controller.config;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.service.*;
import io.xeres.app.service.backup.BackupService;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.app.xrs.service.status.StatusRsService;
import io.xeres.common.rest.config.OwnIdentityRequest;
import io.xeres.common.rest.config.OwnLocationRequest;
import io.xeres.common.rest.config.OwnProfileRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static io.xeres.common.rest.PathConfig.*;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConfigControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = CONFIG_PATH;

	@MockitoBean
	private ProfileService profileService;

	@MockitoBean
	private LocationService locationService;

	@MockitoBean
	private IdentityRsService identityRsService;

	@MockitoBean
	private CapabilityService capabilityService;

	@MockitoBean
	private BackupService backupService;

	@MockitoBean
	private NetworkService networkService;

	@MockitoBean
	private StatusRsService statusRsService;

	@Autowired
	public MockMvc mvc;

	@Test
	void CreateProfile_Success() throws Exception
	{
		var profileRequest = new OwnProfileRequest("test node");

		when(profileService.generateProfileKeys(profileRequest.name())).thenReturn(ResourceCreationState.CREATED);

		mvc.perform(postJson(BASE_URL + "/profile", profileRequest))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + PROFILES_PATH + "/" + 1L));

		verify(profileService).generateProfileKeys(profileRequest.name());
	}

	@Test
	void CreateProfile_Failure() throws Exception
	{
		var ownProfileRequest = new OwnProfileRequest("test node");

		when(profileService.generateProfileKeys(ownProfileRequest.name())).thenReturn(ResourceCreationState.FAILED);

		mvc.perform(postJson(BASE_URL + "/profile", ownProfileRequest))
				.andExpect(status().isInternalServerError());

		verify(profileService).generateProfileKeys(ownProfileRequest.name());
	}

	@Test
	void CreateProfile_AlreadyExists_Failure() throws Exception
	{
		var profileRequest = new OwnProfileRequest("test node");

		when(profileService.generateProfileKeys(profileRequest.name())).thenReturn(ResourceCreationState.ALREADY_EXISTS);

		mvc.perform(postJson(BASE_URL + "/profile", profileRequest))
				.andExpect(status().isOk());

		verify(profileService).generateProfileKeys(profileRequest.name());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"This name is way too long and there's no chance it ever gets created as a profile"
	})
	void CreateProfile_BadName_Failure(String name) throws Exception
	{
		var ownProfileRequest = new OwnProfileRequest(name);

		mvc.perform(postJson(BASE_URL + "/profile", ownProfileRequest))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(profileService);
	}

	@Test
	void CreateLocation_Success() throws Exception
	{
		var ownLocationRequest = new OwnLocationRequest("test location");

		mvc.perform(postJson(BASE_URL + "/location", ownLocationRequest))
				.andExpect(status().isCreated());

		verify(locationService).generateOwnLocation(anyString());
	}

	@Test
	void CreateLocation_Failure() throws Exception
	{
		var ownLocationRequest = new OwnLocationRequest("test location");

		when(locationService.generateOwnLocation(anyString())).thenReturn(ResourceCreationState.FAILED);

		mvc.perform(postJson(BASE_URL + "/location", ownLocationRequest))
				.andExpect(status().isInternalServerError());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"This name is way too long and there's no chance it ever gets created as a location"
	})
	void CreateLocation_BadName_Failure(String name) throws Exception
	{
		var ownLocationRequest = new OwnLocationRequest(name);

		mvc.perform(postJson(BASE_URL + "/location", ownLocationRequest))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(locationService);
	}

	@Test
	void GetExternalIpAddress_Success() throws Exception
	{
		var ip = "1.1.1.1";
		var port = 6667;

		var location = Location.createLocation("test");
		var connection = Connection.from(PeerAddress.from(ip, port));
		location.addConnection(connection);

		when(locationService.findOwnLocation()).thenReturn(Optional.of(location));

		mvc.perform(getJson(BASE_URL + "/external-ip"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ip", is(ip)))
				.andExpect(jsonPath("$.port", is(port)));
	}

	@Test
	void GetExternalIpAddress_NoLocationOrIpAddress_Success() throws Exception
	{
		when(locationService.findOwnLocation()).thenReturn(Optional.empty());

		mvc.perform(getJson(BASE_URL + "/external-ip"))
				.andExpect(status().isNotFound());
	}

	@Test
	void GetInternalIpAddress_Success() throws Exception
	{
		var ip = "192.168.1.25";
		var port = 1234;

		var location = Location.createLocation("test");
		var connection = Connection.from(PeerAddress.from(ip, port));
		location.addConnection(connection);

		when(networkService.getLocalIpAddress()).thenReturn(ip);
		when(networkService.getPort()).thenReturn(port);

		mvc.perform(getJson(BASE_URL + "/internal-ip"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ip", is(ip)))
				.andExpect(jsonPath("$.port", is(port)));
	}

	@Test
	void GetInternalIpAddress_NoLocationOrIpAddress_Success() throws Exception
	{
		when(locationService.findOwnLocation()).thenReturn(Optional.empty());

		mvc.perform(getJson(BASE_URL + "/internalIp"))
				.andExpect(status().isNotFound());
	}

	@Test
	void GetHostname_Success() throws Exception
	{
		var hostname = "foo.bar.com";

		when(locationService.getHostname()).thenReturn(hostname);

		mvc.perform(getJson(BASE_URL + "/hostname"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hostname", is(hostname)));
	}

	@Test
	void GetUsername_Success() throws Exception
	{
		var username = "foobar";
		when(locationService.getUsername()).thenReturn(username);

		mvc.perform(getJson(BASE_URL + "/username"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username", is(username)));
	}

	@Test
	void CreateIdentity_Signed_Success() throws Exception
	{
		var identity = IdentityFakes.createOwn();
		var identityRequest = new OwnIdentityRequest(identity.getName(), false);

		when(identityRsService.generateOwnIdentity(identityRequest.name(), true)).thenReturn(ResourceCreationState.CREATED);

		mvc.perform(postJson(BASE_URL + "/identity", identityRequest))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + IDENTITIES_PATH + "/" + identity.getId()));

		verify(identityRsService).generateOwnIdentity(identityRequest.name(), true);
	}

	@Test
	void CreateIdentity_Anonymous_Success() throws Exception
	{
		var identity = IdentityFakes.createOwn();
		var identityRequest = new OwnIdentityRequest(identity.getName(), true);

		when(identityRsService.generateOwnIdentity(identityRequest.name(), false)).thenReturn(ResourceCreationState.CREATED);

		mvc.perform(postJson(BASE_URL + "/identity", identityRequest))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + IDENTITIES_PATH + "/" + identity.getId()));

		verify(identityRsService).generateOwnIdentity(identityRequest.name(), false);
	}

	@Test
	void GetCapabilities_Success() throws Exception
	{
		var capability = "autostart";
		when(capabilityService.getCapabilities()).thenReturn(Set.of(capability));

		mvc.perform(getJson(BASE_URL + "/capabilities"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0]", is(capability)));

		verify(capabilityService).getCapabilities();
	}
}
