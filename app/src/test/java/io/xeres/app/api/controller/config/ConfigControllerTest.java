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

package io.xeres.app.api.controller.config;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.service.CapabilityService;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.ProfileService;
import io.xeres.app.service.backup.BackupService;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.common.rest.config.IpAddressRequest;
import io.xeres.common.rest.config.OwnIdentityRequest;
import io.xeres.common.rest.config.OwnLocationRequest;
import io.xeres.common.rest.config.OwnProfileRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.cert.CertificateException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static io.xeres.common.rest.PathConfig.*;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConfigController.class)
class ConfigControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = CONFIG_PATH;

	@MockBean
	private ProfileService profileService;

	@MockBean
	private LocationService locationService;

	@MockBean
	private IdentityRsService identityRsService;

	@MockBean
	private CapabilityService capabilityService;

	@MockBean
	private BackupService backupService;

	@Autowired
	public MockMvc mvc;

	@Test
	void ConfigController_CreateProfile_OK() throws Exception
	{
		var profileRequest = new OwnProfileRequest("test node");

		when(profileService.generateProfileKeys(profileRequest.name())).thenReturn(true);

		mvc.perform(postJson(BASE_URL + "/profile", profileRequest))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + PROFILES_PATH + "/" + 1L));

		verify(profileService).generateProfileKeys(profileRequest.name());
	}

	@Test
	void ConfigController_CreateProfile_Fail() throws Exception
	{
		var ownProfileRequest = new OwnProfileRequest("test node");

		when(profileService.generateProfileKeys(ownProfileRequest.name())).thenReturn(false);

		mvc.perform(postJson(BASE_URL + "/profile", ownProfileRequest))
				.andExpect(status().isInternalServerError());

		verify(profileService).generateProfileKeys(ownProfileRequest.name());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"This name is way too long and there's no chance it ever gets created as a profile"
	})
	void ConfigController_CreateProfile_BadName(String name) throws Exception
	{
		var ownProfileRequest = new OwnProfileRequest(name);

		mvc.perform(postJson(BASE_URL + "/profile", ownProfileRequest))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(profileService);
	}

	@Test
	void ConfigController_CreateLocation_OK() throws Exception
	{
		var ownLocationRequest = new OwnLocationRequest("test location");

		mvc.perform(postJson(BASE_URL + "/location", ownLocationRequest))
				.andExpect(status().isCreated());

		verify(locationService).generateOwnLocation(anyString());
	}

	@Test
	void ConfigController_CreateLocation_Fail() throws Exception
	{
		var ownLocationRequest = new OwnLocationRequest("test location");

		doThrow(CertificateException.class).when(locationService).generateOwnLocation(anyString());

		mvc.perform(postJson(BASE_URL + "/location", ownLocationRequest))
				.andExpect(status().isInternalServerError());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"This name is way too long and there's no chance it ever gets created as a location"
	})
	void ConfigController_CreateLocation_BadName(String name) throws Exception
	{
		var ownLocationRequest = new OwnLocationRequest(name);

		mvc.perform(postJson(BASE_URL + "/location", ownLocationRequest))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(locationService);
	}

	@Test
	void ConfigController_UpdateExternalIpAddress_Create_OK() throws Exception
	{
		var IP = "1.1.1.1";
		var PORT = 6667;

		when(locationService.findOwnLocation()).thenReturn(Optional.of(Location.createLocation("foo")));
		when(locationService.updateConnection(any(Location.class), any(PeerAddress.class))).thenReturn(LocationService.UpdateConnectionStatus.ADDED);

		var request = new IpAddressRequest(IP, PORT);

		mvc.perform(putJson(BASE_URL + "/externalIp", request))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + CONFIG_PATH + "/externalIp"));

		verify(locationService).updateConnection(any(Location.class), any(PeerAddress.class));
	}

	@Test
	void ConfigController_UpdateExternalIpAddress_Update_OK() throws Exception
	{
		var IP = "1.1.1.1";
		var PORT = 6667;

		when(locationService.findOwnLocation()).thenReturn(Optional.of(Location.createLocation("foo")));
		when(locationService.updateConnection(any(Location.class), any(PeerAddress.class))).thenReturn(LocationService.UpdateConnectionStatus.UPDATED);

		var request = new IpAddressRequest(IP, PORT);


		mvc.perform(putJson(BASE_URL + "/externalIp", request))
				.andExpect(status().isNoContent());

		verify(locationService).updateConnection(any(Location.class), any(PeerAddress.class));
	}

	@Test
	void ConfigController_UpdateExternalIpAddress_Update_NoConnection_Fail() throws Exception
	{
		var IP = "1.1.1.1";
		var PORT = 6667;

		when(locationService.findOwnLocation()).thenReturn(Optional.of(Location.createLocation("foo")));
		when(locationService.updateConnection(any(Location.class), any(PeerAddress.class))).thenThrow(NoSuchElementException.class);

		var request = new IpAddressRequest(IP, PORT);

		mvc.perform(putJson(BASE_URL + "/externalIp", request))
				.andExpect(status().isNotFound());

		verify(locationService).updateConnection(any(Location.class), any(PeerAddress.class));
	}

	@Test
	void ConfigController_UpdateExternalIpAddress_Update_WrongIp_Fail() throws Exception
	{
		var IP = "1.1.1.1.1";
		var PORT = 6667;

		when(locationService.updateConnection(any(Location.class), any(PeerAddress.class))).thenThrow(NoSuchElementException.class);

		var request = new IpAddressRequest(IP, PORT);

		mvc.perform(putJson(BASE_URL + "/externalIp", request))
				.andExpect(status().isBadRequest());
	}

	@Test
	void ConfigController_UpdateExternalIpAddress_Update_InternalIp_Fail() throws Exception
	{
		var IP = "192.168.1.38";
		var PORT = 6667;

		when(locationService.updateConnection(any(Location.class), any(PeerAddress.class))).thenThrow(NoSuchElementException.class);

		var request = new IpAddressRequest(IP, PORT);

		mvc.perform(putJson(BASE_URL + "/externalIp", request))
				.andExpect(status().isBadRequest());
	}

	@Test
	void ConfigController_GetExternalIpAddress_OK() throws Exception
	{
		var IP = "1.1.1.1";
		var PORT = 6667;

		var location = Location.createLocation("test");
		var connection = Connection.from(PeerAddress.from(IP, PORT));
		location.addConnection(connection);

		when(locationService.findOwnLocation()).thenReturn(Optional.of(location));

		mvc.perform(getJson(BASE_URL + "/externalIp"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ip", is(IP)))
				.andExpect(jsonPath("$.port", is(PORT)));
	}

	@Test
	void ConfigController_GetExternalIpAddress_NoLocationOrIpAddress_OK() throws Exception
	{
		when(locationService.findOwnLocation()).thenReturn(Optional.empty());

		mvc.perform(getJson(BASE_URL + "/externalIp"))
				.andExpect(status().isNotFound());
	}

	@Test
	void ConfigController_GetInternalIpAddress_OK() throws Exception
	{
		var IP = "192.168.1.25";
		var PORT = 1234;

		var location = Location.createLocation("test");
		var connection = Connection.from(PeerAddress.from(IP, PORT));
		location.addConnection(connection);

		when(locationService.findOwnLocation()).thenReturn(Optional.of(location));

		mvc.perform(getJson(BASE_URL + "/internalIp"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ip", is(IP)))
				.andExpect(jsonPath("$.port", is(PORT)));
	}

	@Test
	void ConfigController_GetInternalIpAddress_NoLocationOrIpAddress_OK() throws Exception
	{
		when(locationService.findOwnLocation()).thenReturn(Optional.empty());

		mvc.perform(getJson(BASE_URL + "/internalIp"))
				.andExpect(status().isNotFound());
	}

	@Test
	void ConfigController_GetHostname_OK() throws Exception
	{
		var HOSTNAME = "foo.bar.com";

		when(locationService.getHostname()).thenReturn(HOSTNAME);

		mvc.perform(getJson(BASE_URL + "/hostname"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hostname", is(HOSTNAME)));
	}

	@Test
	void ConfigController_GetUsername_OK() throws Exception
	{
		var USERNAME = "foobar";
		when(locationService.getUsername()).thenReturn(USERNAME);

		mvc.perform(getJson(BASE_URL + "/username"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username", is(USERNAME)));
	}

	@Test
	void ConfigController_CreateIdentity_Signed_OK() throws Exception
	{
		var identity = IdentityFakes.createOwn();
		var identityRequest = new OwnIdentityRequest(identity.getName(), false);

		when(identityRsService.generateOwnIdentity(identityRequest.name(), true)).thenReturn(identity.getId());

		mvc.perform(postJson(BASE_URL + "/identity", identityRequest))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + IDENTITIES_PATH + "/" + identity.getId()));

		verify(identityRsService).generateOwnIdentity(identityRequest.name(), true);
	}

	@Test
	void ConfigController_CreateIdentity_Anonymous_OK() throws Exception
	{
		var identity = IdentityFakes.createOwn();
		var identityRequest = new OwnIdentityRequest(identity.getName(), true);

		when(identityRsService.generateOwnIdentity(identityRequest.name(), false)).thenReturn(identity.getId());

		mvc.perform(postJson(BASE_URL + "/identity", identityRequest))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + IDENTITIES_PATH + "/" + identity.getId()));

		verify(identityRsService).generateOwnIdentity(identityRequest.name(), false);
	}

	@Test
	void ConfigController_GetCapabilities_OK() throws Exception
	{
		var capability = "autostart";
		when(capabilityService.getCapabilities()).thenReturn(Set.of(capability));

		mvc.perform(getJson(BASE_URL + "/capabilities"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0]", is(capability)));

		verify(capabilityService).getCapabilities();
	}
}
