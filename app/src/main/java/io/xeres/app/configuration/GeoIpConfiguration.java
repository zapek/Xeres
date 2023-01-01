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

package io.xeres.app.configuration;

import com.maxmind.geoip2.DatabaseReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Objects;

@Configuration
public class GeoIpConfiguration
{
	private static final Logger log = LoggerFactory.getLogger(GeoIpConfiguration.class);

	@Bean
	public DatabaseReader getDatabaseReader()
	{
		var database = Objects.requireNonNull(getClass().getResourceAsStream("/GeoLite2-Country.mmdb"));
		try
		{
			return new DatabaseReader.Builder(database).build();
		}
		catch (IOException e)
		{
			log.error("Couldn't setup GeoIP: {}", e.getMessage(), e);
			return null;
		}
	}
}
