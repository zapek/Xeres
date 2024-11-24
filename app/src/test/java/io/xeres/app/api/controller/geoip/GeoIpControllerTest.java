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

package io.xeres.app.api.controller.geoip;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.service.GeoIpService;
import io.xeres.common.geoip.Country;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static io.xeres.common.rest.PathConfig.GEOIP_PATH;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeoIpController.class)
@AutoConfigureMockMvc(addFilters = false)
class GeoIpControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = GEOIP_PATH;

	@MockitoBean
	private GeoIpService geoIpService;

	@Autowired
	public MockMvc mvc;

	@Test
	void GetIsoCountry_Success() throws Exception
	{
		var address = "1.1.1.1";

		when(geoIpService.getCountry(address)).thenReturn(Country.CH);

		mvc.perform(getJson(BASE_URL + "/" + address))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isoCountry").value(Country.CH.name().toLowerCase(Locale.ROOT)));

		verify(geoIpService).getCountry(address);
	}

	@Test
	void GetIsoCountry_Failure() throws Exception
	{
		var address = "1.1.1.1";

		when(geoIpService.getCountry(address)).thenReturn(null);

		mvc.perform(getJson(BASE_URL + "/" + address))
				.andExpect(status().isNotFound());

		verify(geoIpService).getCountry(address);
	}
}
