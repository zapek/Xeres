package io.xeres.app.api.controller.connection;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.connection.ConnectionFakes;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.service.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static io.xeres.common.rest.PathConfig.CONNECTIONS_PATH;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConnectionController.class)
class ConnectionControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = CONNECTIONS_PATH;

	@MockBean
	private LocationService locationService;

	@Autowired
	public MockMvc mvc;

	@Test
	void ConnectionController_GetConnectedProfiles_OK() throws Exception
	{
		var locations = List.of(LocationFakes.createLocation(),
				LocationFakes.createOwnLocation());
		when(locationService.getConnectedLocations()).thenReturn(locations);

		mvc.perform(getJson(BASE_URL + "/profiles"))
				.andExpect(status().isOk());

		verify(locationService).getConnectedLocations();
	}
}
