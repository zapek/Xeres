/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.app.application.environment;

import io.xeres.app.application.SingleInstanceRun;
import io.xeres.app.util.DevUtils;
import io.xeres.common.AppName;
import io.xeres.common.properties.StartupProperties;
import io.xeres.common.util.OsUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static io.xeres.common.properties.StartupProperties.Property.DATA_DIR;

public final class DataDirLocator
{
	private static final String LOCAL_DATA = "data";

	private static String dataDir;

	private DataDirLocator()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void init()
	{
		if (dataDir != null)
		{
			throw new IllegalStateException("init() called twice");
		}

		dataDir = getDataDirFromArgs();
		if (dataDir == null && Optional.ofNullable(System.getProperty("spring.profiles.active")).orElse("").contains("dev")) // IntelliJ passes the profile that way
		{
			dataDir = DevUtils.getDirFromDevelopmentSetup(LOCAL_DATA);
		}

		if (dataDir == null && DevUtils.isTesting())
		{
			return;
		}

		if (dataDir == null)
		{
			dataDir = getDataDirFromPortableFileLocation();
		}
		if (dataDir == null)
		{
			dataDir = OsUtils.getDataDir().toString();
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
	}

	public static String getDataDir()
	{
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
}
