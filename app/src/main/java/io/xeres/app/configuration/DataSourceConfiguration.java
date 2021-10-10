/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

import io.xeres.app.properties.DatabaseProperties;
import io.xeres.ui.support.splash.SplashService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import javax.sql.DataSource;
import java.nio.file.Path;

/**
 * Configuration for the location and options of the database.
 */
@Configuration
@DependsOn("getDataDir")
public class DataSourceConfiguration
{
	private static final Logger log = LoggerFactory.getLogger(DataSourceConfiguration.class);

	private final Environment environment;
	private final DatabaseProperties databaseProperties;
	private final DataDirConfiguration dataDirConfiguration;
	private final SplashService splashService;

	public DataSourceConfiguration(Environment environment, DatabaseProperties databaseProperties, DataDirConfiguration dataDirConfiguration, SplashService splashService)
	{
		this.environment = environment;
		this.databaseProperties = databaseProperties;
		this.dataDirConfiguration = dataDirConfiguration;
		this.splashService = splashService;
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.datasource", name = "url", havingValue = "false", matchIfMissing = true)
	public DataSource getDataSource()
	{
		splashService.status("Loading database");

		var useJMX = "";

		if (environment.acceptsProfiles(Profiles.of("dev")))
		{
			useJMX = ";JMX=TRUE";
		}

		var dataDir = Path.of(dataDirConfiguration.getDataDir(), "userdata").toString();

		log.debug("Using database file: {}", dataDir);

		var dbOpts = "";

		if (databaseProperties.getCacheSize() != null)
		{
			dbOpts += ";CACHE_SIZE=" + databaseProperties.getCacheSize();
		}

		return DataSourceBuilder
				.create()
				.url("jdbc:h2:file:" + dataDir + dbOpts + useJMX)
				.username("sa")
				.driverClassName("org.h2.Driver")
				.build();
	}
}
