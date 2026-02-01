/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

import io.xeres.common.properties.StartupProperties;
import io.xeres.common.properties.StartupProperties.Origin;
import io.xeres.common.util.OsUtils;

import java.io.IOException;
import java.nio.file.Files;

import static io.xeres.common.properties.StartupProperties.Property.HTTPS;
import static io.xeres.common.properties.StartupProperties.Property.LOGFILE;

public final class DefaultProperties
{
	private DefaultProperties()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void setDefaults()
	{
		// We default to HTTPS and have to specify it here because RemoteUtils
		// uses the property to know in which mode it is.
		StartupProperties.setBoolean(HTTPS, "true", Origin.PROPERTY);

		// If we're running from jpackage (aka, we're a final installation),
		// then we set the log file to a sensible path. We have to do it early too!
		if (OsUtils.isInstalled())
		{
			var logFile = OsUtils.getLogFile();
			try
			{
				Files.createDirectories(logFile.getParent());
				StartupProperties.setString(LOGFILE, logFile.toString(), Origin.PROPERTY);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}
