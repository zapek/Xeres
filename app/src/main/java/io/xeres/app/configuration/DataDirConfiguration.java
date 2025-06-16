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

import io.xeres.app.application.SingleInstanceRun;
import io.xeres.app.util.DevUtils;
import io.xeres.common.AppName;
import io.xeres.common.properties.StartupProperties;
import net.harawata.appdirs.AppDirsFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static io.xeres.common.properties.StartupProperties.Property.DATA_DIR;

/**
 * Configuration for everything related to the user data directory (database, keys, user data, ...).
 */
@Configuration
public class DataDirConfiguration
{
	private static final String LOCAL_DATA = "data";

	private final Environment environment;

	private String dataDir;

	public DataDirConfiguration(Environment environment)
	{
		this.environment = environment;
	}

	/**
	 * Gets the data directory where all user data is stored.
	 * Note: this is not really used as a proper bean. DataSourceConfiguration depends on it, but it's accessed by the method.
	 * @return the path to the data directory
	 */
	@Bean
	public String getDataDir()
	{
		if (dataDir != null)
		{
			return dataDir;
		}

		// If a datasource is already set (that is, tests), then we don't return anything
		if (environment.getProperty("spring.datasource.url") != null)
		{
			return null;
		}

		dataDir = getDataDirFromArgs();
		if (dataDir == null && environment.acceptsProfiles(Profiles.of("dev")))
		{
			dataDir = DevUtils.getDirFromDevelopmentSetup(LOCAL_DATA);
		}

		if (dataDir == null)
		{
			dataDir = getDataDirFromPortableFileLocation();
		}
		if (dataDir == null)
		{
			dataDir = getDataDirFromNativePlatform();
		}

		Objects.requireNonNull(dataDir);

		var path = Path.of(dataDir);

		if (Files.notExists(path))
		{
			try
			{
				Files.createDirectory(path);
			}
			catch (IOException e)
			{
				throw new IllegalStateException("Couldn't create data directory: " + dataDir + ", :" + e.getMessage());
			}
		}

		if (!SingleInstanceRun.enforceSingleInstance(dataDir))
		{
			throw new IllegalStateException("An instance of " + AppName.NAME + " is already running, path: " + dataDir);
		}

		return dataDir;
	}

	private static String getDataDirFromArgs()
	{
		return StartupProperties.getString(DATA_DIR);
	}

	private static String getDataDirFromPortableFileLocation()
	{
		var portable = Path.of("portable");
		if (Files.exists(portable))
		{
			return portable.resolveSibling(LOCAL_DATA).toAbsolutePath().toString();
		}
		return null;
	}

	private static String getDataDirFromNativePlatform()
	{
		var appDirs = AppDirsFactory.getInstance();
		return appDirs.getUserDataDir(AppName.NAME, null, null, true);
	}
}
