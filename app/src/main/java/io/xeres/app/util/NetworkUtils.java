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

package io.xeres.app.util;

import io.xeres.common.util.OsUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.SystemUtils;

public final class NetworkUtils
{
	private NetworkUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Finds if a network is private, usually a home LAN.
	 * <p>
	 * Currently only works on Windows.
	 *
	 * @return true if private
	 */
	public static boolean isPrivate()
	{
		if (SystemUtils.IS_OS_WINDOWS)
		{
			var result = OsUtils.shellExecute("powershell.exe", "-Command", "(Get-NetConnectionProfile).NetworkCategory");
			return Strings.CI.equals("Private", result.trim());
		}
		else
		{
			return false;
		}
	}
}
