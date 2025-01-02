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

package io.xeres.app.application.environment;

import io.xeres.common.properties.StartupProperties;

import java.util.Arrays;

import static io.xeres.common.properties.StartupProperties.Property.UI;

/**
 * Utility class containing cloud related functions.
 */
public final class Cloud
{
	private Cloud()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Checks if we are running on the cloud. This is done by checking if the profile <i>cloud</i> is in the <b>SPRING_PROFILES_ACTIVE</b> env variable.
	 *
	 * @return true if running on the cloud
	 */
	private static boolean isRunningOnCloud()
	{
		var profiles = System.getenv("SPRING_PROFILES_ACTIVE");
		if (profiles != null)
		{
			var tokens = profiles.split(",");
			return Arrays.asList(tokens).contains(
					"cloud");
		}
		return false;
	}

	public static void checkIfRunningOnCloud()
	{
		if (isRunningOnCloud())
		{
			StartupProperties.setBoolean(UI, "false", StartupProperties.Origin.ENVIRONMENT_VARIABLE);
		}
	}
}
