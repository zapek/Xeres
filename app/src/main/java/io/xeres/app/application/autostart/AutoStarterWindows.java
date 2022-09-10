/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.app.application.autostart;

import io.xeres.common.AppName;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.sun.jna.platform.win32.Advapi32Util.*;
import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class AutoStarterWindows implements AutoStarter
{
	public static final String REGISTRY_RUN_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";

	private String applicationPath;

	/**
	 * Autostart only works for an installed executable launched normally.
	 * No portable installations, no local installations.
	 *
	 * @return if autostart is supported
	 */
	@Override
	public boolean isSupported()
	{
		return isNotBlank(getApplicationPath());
	}

	@Override
	public boolean isEnabled()
	{
		if (!isSupported())
		{
			return false;
		}
		return isNotBlank(registryGetStringValue(HKEY_CURRENT_USER, REGISTRY_RUN_PATH, AppName.NAME));
	}

	@Override
	public void enable()
	{
		if (isSupported())
		{
			registryCreateKey(HKEY_CURRENT_USER, REGISTRY_RUN_PATH, AppName.NAME);
			registrySetStringValue(HKEY_CURRENT_USER, REGISTRY_RUN_PATH, AppName.NAME, "\"" + getApplicationPath() + "\"" + " --iconified");
		}
	}

	@Override
	public void disable()
	{
		if (isSupported())
		{
			registryDeleteKey(HKEY_CURRENT_USER, REGISTRY_RUN_PATH, AppName.NAME);
		}
	}

	private String getApplicationPath()
	{
		if (applicationPath != null)
		{
			return applicationPath;
		}

		var basePath = System.getProperty("user.dir");
		if (basePath == null)
		{
			return null;
		}

		var appPath = Path.of(basePath, AppName.NAME + ".exe");
		if (Files.notExists(appPath))
		{
			return null;
		}

		applicationPath = appPath.toAbsolutePath().toString();

		return applicationPath;
	}
}
