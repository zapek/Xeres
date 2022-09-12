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
	void GeoIpService_GetCountry_OK() throws IOException, GeoIp2Exception
	{
		var address = "1.1.1.1";
		var inetAddress = InetAddress.getByName(address);

		when(databaseReader.country(inetAddress)).thenReturn(new CountryResponse(null, new Country(null, null, null, null, "CH", null), null, null, null, null));

		var country = geoIpService.getCountry(address);

		assertEquals(io.xeres.common.geoip.Country.CH, country);

		verify(databaseReader).country(inetAddress);
	}

	@Test
	void GeoIpService_GetCountry_Fail() throws IOException, GeoIp2Exception
	{
		var address = "1.1.1.1";
		var inetAddress = InetAddress.getByName(address);

		when(databaseReader.country(inetAddress)).thenThrow(new GeoIp2Exception("Country not found"));

		var country = geoIpService.getCountry(address);

		assertNull(country);

		verify(databaseReader).country(inetAddress);
	}
}
