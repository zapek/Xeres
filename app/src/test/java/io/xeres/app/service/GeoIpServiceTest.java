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

package io.xeres.app.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class GeoIpServiceTest
{
	@Mock
	private DatabaseReader databaseReader;

	@InjectMocks
	private GeoIpService geoIpService;

	@Test
	void GetCountry_Success() throws IOException, GeoIp2Exception
	{
		var address = "1.1.1.1";
		var inetAddress = InetAddress.getByName(address);

		when(databaseReader.country(inetAddress)).thenReturn(new CountryResponse(null, new Country(null, null, null, null, "CH", null), null, null, null, null));

		var country = geoIpService.getCountry(address);

		assertEquals(io.xeres.common.geoip.Country.CH, country);

		verify(databaseReader).country(inetAddress);
	}

	@Test
	void GetCountry_Failure() throws IOException, GeoIp2Exception
	{
		var address = "1.1.1.1";
		var inetAddress = InetAddress.getByName(address);

		when(databaseReader.country(inetAddress)).thenThrow(new GeoIp2Exception("Country not found"));

		var country = geoIpService.getCountry(address);

		assertNull(country);

		verify(databaseReader).country(inetAddress);
	}
}
