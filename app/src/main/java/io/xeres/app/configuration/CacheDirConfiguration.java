/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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
import net.harawata.appdirs.AppDirsFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class CacheDirConfiguration
{
	private final Environment environment;

	private String cacheDir;

	public CacheDirConfiguration(Environment environment)
	{
		this.environment = environment;
	}

	@Bean
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

		cacheDir = getCacheDirFromPortableFileLocation();
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
				throw new IllegalStateException("Couldn't create cache directory: " + cacheDir + ", :" + e.getMessage());
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
