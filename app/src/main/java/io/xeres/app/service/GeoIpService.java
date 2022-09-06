package io.xeres.app.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import io.xeres.common.geoip.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;

@Service
public class GeoIpService
{
	private static final Logger log = LoggerFactory.getLogger(GeoIpService.class);
	private final DatabaseReader databaseReader;

	public GeoIpService(DatabaseReader databaseReader)
	{
		this.databaseReader = databaseReader;
	}

	public Country getCountry(String ipAddress)
	{
		try
		{
			final CountryResponse country = databaseReader.country(InetAddress.getByName(ipAddress));
			return Country.valueOf(country.getCountry().getIsoCode());
		}
		catch (IOException | GeoIp2Exception e)
		{
			log.error("No country found for IP {}: {}", ipAddress, e.getMessage());
			return null;
		}
	}
}
