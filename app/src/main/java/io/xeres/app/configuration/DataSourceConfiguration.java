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

package io.xeres.app.configuration;

import io.xeres.app.properties.DatabaseProperties;
import io.xeres.app.service.UiBridgeService;
import org.h2.tools.Upgrade;
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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuration for the location and options of the database.
 */
@Configuration
@DependsOn("getDataDir")
public class DataSourceConfiguration
{
	private static final Logger log = LoggerFactory.getLogger(DataSourceConfiguration.class);

	private static final int H2_UPGRADE_FROM_VERSION = 214;
	private static final int H2_UPGRADE_CURRENT_FORMAT = 3;
	private static final String H2_URL_PREFIX = "jdbc:h2:file:";
	private static final String H2_USERNAME = "sa";

	private final Environment environment;
	private final DatabaseProperties databaseProperties;
	private final DataDirConfiguration dataDirConfiguration;
	private final UiBridgeService uiBridgeService;

	public DataSourceConfiguration(Environment environment, DatabaseProperties databaseProperties, DataDirConfiguration dataDirConfiguration, UiBridgeService uiBridgeService)
	{
		this.environment = environment;
		this.databaseProperties = databaseProperties;
		this.dataDirConfiguration = dataDirConfiguration;
		this.uiBridgeService = uiBridgeService;
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.datasource", name = "url", havingValue = "false", matchIfMissing = true)
	public DataSource getDataSource()
	{
		uiBridgeService.setSplashStatus("Loading database");

		var useJMX = "";

		if (environment.acceptsProfiles(Profiles.of("dev")))
		{
			useJMX = ";JMX=TRUE";
		}

		var dataDir = Path.of(dataDirConfiguration.getDataDir(), "userdata").toString();

		log.debug("Using database file: {}", dataDir);

		var dbOpts = ";DB_CLOSE_ON_EXIT=FALSE";

		if (databaseProperties.getCacheSize() != null)
		{
			dbOpts += ";CACHE_SIZE=" + databaseProperties.getCacheSize();
		}

		if (databaseProperties.getMaxCompactTime() != null)
		{
			dbOpts += ";MAX_COMPACT_TIME=" + databaseProperties.getMaxCompactTime();
		}

		var url = H2_URL_PREFIX + dataDir + dbOpts + useJMX;

		upgradeIfNeeded(url);

		return DataSourceBuilder
				.create()
				.url(url)
				.username(H2_USERNAME)
				.driverClassName("org.h2.Driver")
				.build();
	}

	private static void upgradeIfNeeded(String url)
	{
		if (!url.startsWith(H2_URL_PREFIX))
		{
			log.debug("Not an H2 file, no upgrade needed");
			return;
		}

		var fileName = url.substring(13, url.indexOf(";")) + ".mv.db";
		var filePath = Path.of(fileName);

		if (!Files.exists(filePath) || !Files.isRegularFile(filePath))
		{
			log.debug("No file present, no upgrade needed");
			return;
		}

		try (var reader = new BufferedReader(new FileReader(filePath.toFile())))
		{
			var header = reader.readLine();
			if (header.contains("format:" + H2_UPGRADE_CURRENT_FORMAT))
			{
				log.debug("No upgrade needed for H2");
				return;
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException("Couldn't read database: " + e.getMessage());
		}

		var properties = new Properties();
		properties.put("USER", H2_USERNAME);
		properties.put("PASSWORD", "");
		try
		{
			Upgrade.upgrade(url, properties, H2_UPGRADE_FROM_VERSION);
		}
		catch (Exception e)
		{
			log.error("Couldn't perform upgrade: {}", e.getMessage(), e);
		}
	}
}
