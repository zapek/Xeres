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
