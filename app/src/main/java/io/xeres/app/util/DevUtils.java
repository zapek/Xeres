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

package io.xeres.app.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class DevUtils
{
	private DevUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static String getDirFromDevelopmentSetup(String directory)
	{
		// Find out if we're running from rootProject, which means
		// we have an 'app' folder in there.
		// We use a relative directory because currentDir is not supposed
		// to change, and it looks clearer.
		var appDir = Path.of("app");
		if (Files.exists(appDir))
		{
			return Path.of(".", directory).toString();
		}
		appDir = Path.of("..", "app");
		if (Files.exists(appDir))
		{
			return Path.of("..", directory).toString();
		}
		throw new IllegalStateException("Unable to find/create directory. Current directory must be the project's root directory or 'app'. It is " + Paths.get("").toAbsolutePath());
	}
}
