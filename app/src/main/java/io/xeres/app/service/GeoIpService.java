/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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
			var country = databaseReader.country(InetAddress.getByName(ipAddress));
			return Country.valueOf(country.country().isoCode());
		}
		catch (IOException | GeoIp2Exception | IllegalArgumentException e)
		{
			log.error("No country found for IP {}: {}", ipAddress, e.getMessage());
			return null;
		}
	}
}
