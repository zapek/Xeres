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

package io.xeres.app.api.controller.connection;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.service.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static io.xeres.common.rest.PathConfig.CONNECTIONS_PATH;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConnectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConnectionControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = CONNECTIONS_PATH;

	@MockBean
	private LocationService locationService;

	@Autowired
	public MockMvc mvc;

	@Test
	void GetConnectedProfiles_Success() throws Exception
	{
		var location = LocationFakes.createLocation();
		var locations = List.of(LocationFakes.createOwnLocation(),
				location);
		when(locationService.getConnectedLocations()).thenReturn(locations);

		mvc.perform(getJson(BASE_URL + "/profiles"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(is(1)))
				.andExpect(jsonPath("$.[0].id").value(is(location.getProfile().getId()), Long.class));

		verify(locationService).getConnectedLocations();
	}
}
