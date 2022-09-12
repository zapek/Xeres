package io.xeres.app.api.controller.geoip;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.service.GeoIpService;
import io.xeres.common.geoip.Country;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static io.xeres.common.rest.PathConfig.GEOIP_PATH;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeoIpController.class)
class GeoIpControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = GEOIP_PATH;

	@MockBean
	private GeoIpService geoIpService;

	@Autowired
	public MockMvc mvc;

	@Test
	void GeoIpController_GetIsoCountry_OK() throws Exception
	{
		var address = "1.1.1.1";

		when(geoIpService.getCountry(address)).thenReturn(Country.CH);

		mvc.perform(getJson(BASE_URL + "/" + address))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isoCountry").value(Country.CH.name().toLowerCase(Locale.ROOT)));

		verify(geoIpService).getCountry(address);
	}

	@Test
	void GeoIpController_GetIsoCountry_Fail() throws Exception
	{
		var address = "1.1.1.1";

		when(geoIpService.getCountry(address)).thenReturn(null);

		mvc.perform(getJson(BASE_URL + "/" + address))
				.andExpect(status().isNotFound());

		verify(geoIpService).getCountry(address);
	}
}
