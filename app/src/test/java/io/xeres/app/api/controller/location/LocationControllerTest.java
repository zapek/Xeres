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

package io.xeres.app.api.controller.location;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.service.LocationService;
import io.xeres.common.rsid.Type;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static io.xeres.common.rest.PathConfig.LOCATIONS_PATH;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
class LocationControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = LOCATIONS_PATH;

	@MockBean
	private LocationService locationService;

	@Autowired
	public MockMvc mvc;

	@Test
	void LocationController_FindLocationById_OK() throws Exception
	{
		var location = LocationFakes.createLocation();

		when(locationService.findLocationById(location.getId())).thenReturn(Optional.of(location));

		mvc.perform(getJson(BASE_URL + "/" + location.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(is(location.getId()), Long.class));

		verify(locationService).findLocationById(location.getId());
	}

	@Test
	void LocationController_getRSIdOfLocation_OK() throws Exception
	{
		var location = LocationFakes.createLocation();

		when(locationService.findLocationById(location.getId())).thenReturn(Optional.of(location));

		mvc.perform(getJson(BASE_URL + "/" + location.getId() + "/rsId"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is(location.getProfile().getName())))
				.andExpect(jsonPath("$.location", is(location.getName())))
				.andExpect(jsonPath("$.rsId", is(location.getRsId(Type.ANY).getArmored())));

		verify(locationService).findLocationById(location.getId());
	}
}
