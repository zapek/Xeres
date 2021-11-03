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
import java.nio.file.Paths;
import java.util.Objects;

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
	 *
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
			dataDir = getDataDirFromDevelopmentSetup();
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
		return dataDir;
	}

	private String getDataDirFromArgs()
	{
		return StartupProperties.getString(StartupProperties.Property.DATA_DIR);
	}

	private String getDataDirFromPortableFileLocation()
	{
		var portable = Path.of("portable");
		if (Files.exists(portable))
		{
			return portable.resolveSibling(LOCAL_DATA).toAbsolutePath().toString();
		}
		return null;
	}

	private String getDataDirFromNativePlatform()
	{
		var appDirs = AppDirsFactory.getInstance();
		return appDirs.getUserDataDir(AppName.NAME, null, null, true);
	}

	private String getDataDirFromDevelopmentSetup()
	{
		// Find out if we're running from rootProject, which means
		// we have an 'app' folder in there.
		// We use a relative directory because currentDir is not supposed
		// to change, and it looks clearer.
		var appDir = Path.of("app");
		if (Files.exists(appDir))
		{
			return Path.of(".", LOCAL_DATA).toString();
		}
		appDir = Path.of("..", "app");
		if (Files.exists(appDir))
		{
			return Path.of("..", LOCAL_DATA).toString();
		}
		throw new IllegalStateException("Unable to find/create data directory. Current directory must be the project's root directory or 'app'. It is " + Paths.get("").toAbsolutePath());
	}
}
