/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

import io.xeres.app.util.DevUtils;
import io.xeres.common.AppName;
import net.harawata.appdirs.AppDirsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This configuration handles the cache directory location. This is stored locally and is deleted upon
 * uninstallation.
 * <p>
 * Portable versions use a cache directory alongside the data directory.
 */
@Configuration
public class CacheDirConfiguration
{
	private static final Logger log = LoggerFactory.getLogger(CacheDirConfiguration.class);

	private final Environment environment;

	private String cacheDir;

	public CacheDirConfiguration(Environment environment)
	{
		this.environment = environment;
	}

	public String getCacheDir()
	{
		if (cacheDir != null)
		{
			return cacheDir;
		}

		// If a datasource is already set (that is, tests), then we don't return anything
		if (environment.getProperty("spring.datasource.url") != null)
		{
			return null;
		}

		if (environment.acceptsProfiles(Profiles.of("dev")))
		{
			cacheDir = DevUtils.getDirFromDevelopmentSetup("cache");
		}

		if (cacheDir == null)
		{
			cacheDir = getCacheDirFromPortableFileLocation();
		}

		if (cacheDir == null)
		{
			cacheDir = getCacheDirFromNativePlatform();
		}

		var path = Path.of(cacheDir);
		if (Files.notExists(path))
		{
			try
			{
				Files.createDirectory(path);
			}
			catch (IOException e)
			{
				log.error("Couldn't create cache directory: {}, {}. Cache won't be available", cacheDir, e.getMessage());
				return null;
			}
		}
		return cacheDir;
	}

	private static String getCacheDirFromPortableFileLocation()
	{
		var portable = Path.of("portable");
		if (Files.exists(portable))
		{
			return portable.resolveSibling("Cache").toAbsolutePath().toString();
		}
		return null;
	}

	private static String getCacheDirFromNativePlatform()
	{
		var appDirs = AppDirsFactory.getInstance();
		return appDirs.getUserCacheDir(AppName.NAME, null, null);
	}
}
