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

package io.xeres.common.mui;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

final class AWTUtils
{
	private AWTUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static boolean isDarkMode()
	{
		if (SystemUtils.IS_OS_WINDOWS)
		{
			return isWindowsDarkMode();
		}
		else if (SystemUtils.IS_OS_MAC)
		{
			return isMacDarkMode();
		}
		else if (SystemUtils.IS_OS_LINUX)
		{
			return isLinuxDarkMode();
		}
		else
		{
			return false;
		}
	}

	private static boolean isWindowsDarkMode()
	{
		try
		{
			int value = Advapi32Util.registryGetIntValue(
					WinReg.HKEY_CURRENT_USER,
					"Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
					"AppsUseLightTheme"
			);
			// 0 = Dark mode, 1 = Light mode
			return value == 0;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private static boolean isMacDarkMode()
	{
		try
		{
			Process process = Runtime.getRuntime().exec(new String[]{"defaults", "read", "-g", "AppleInterfaceStyle"});
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = reader.readLine();
			return "Dark".equals(line);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private static boolean isLinuxDarkMode()
	{
		try
		{
			Process process = Runtime.getRuntime().exec(new String[]{"gsettings", "get", "org.gnome.desktop.interface", "gtk-theme"});
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = reader.readLine();
			return line != null && line.toLowerCase().contains("dark");
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
