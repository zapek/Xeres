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

package io.xeres.ui.support.version;

import io.micrometer.common.util.StringUtils;

import java.util.regex.Pattern;

public final class VersionChecker
{
	private static final Pattern VERSION_PATTERN = Pattern.compile("^v(\\d{1,5})\\.(\\d{1,5})\\.(\\d{1,5})$");

	private VersionChecker()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static Version createVersion(String versionString)
	{
		if (!versionString.startsWith("v"))
		{
			versionString = "v" + versionString;
		}
		var matcher = VERSION_PATTERN.matcher(versionString);

		if (matcher.matches())
		{
			return new Version((Integer.parseInt(matcher.group(1))), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
		}
		return new Version(0, 0, 0);
	}

	public static boolean isVersionMoreRecent(String newVersionString, String currentVersionString)
	{
		if (StringUtils.isBlank(newVersionString))
		{
			return false;
		}

		var currentVersion = createVersion(currentVersionString);
		if (currentVersion.isNotARelease())
		{
			return false;
		}

		var newVersion = createVersion(newVersionString);

		return newVersion.compareTo(currentVersion) > 0;
	}
}
