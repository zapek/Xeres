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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static com.sun.jna.platform.win32.Advapi32Util.*;
import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class AutoStarterWindows implements AutoStarter
{
	private static final Logger log = LoggerFactory.getLogger(AutoStarterWindows.class);

	public static final String REGISTRY_RUN_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
	public static final String JAVA_OPTIONS = "[JavaOptions]";
	public static final String JAVA_OPTIONS_SPLASH_DETECTION = "java-options=-splash:";
	public static final String JAVA_OPTIONS_SPLASH = "java-options=-splash:$APPDIR/startup.jpg";
	public static final String CONFIG_EXTENSION = ".cfg";
	public static final String EXECUTABLE_EXTENSION = ".exe";
	public static final String APP_DIRECTORY = "./app/";

	private Path applicationPath;

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
			updateSplashScreen(false);
		}
	}

	@Override
	public void disable()
	{
		if (isSupported())
		{
			registryDeleteValue(HKEY_CURRENT_USER, REGISTRY_RUN_PATH, AppName.NAME);
			registryDeleteKey(HKEY_CURRENT_USER, REGISTRY_RUN_PATH, AppName.NAME);
			updateSplashScreen(true);
		}
	}

	private String getApplicationPath()
	{
		if (applicationPath != null)
		{
			return applicationPath.toString();
		}

		var basePath = System.getProperty("user.dir");
		if (basePath == null)
		{
			return null;
		}

		var appPath = Path.of(basePath, AppName.NAME + EXECUTABLE_EXTENSION);
		if (Files.notExists(appPath))
		{
			return null;
		}

		applicationPath = appPath.toAbsolutePath();

		return applicationPath.toString();
	}

	private void updateSplashScreen(boolean enable)
	{
		if (applicationPath == null)
		{
			return;
		}

		var configFile = applicationPath.resolveSibling(APP_DIRECTORY + AppName.NAME + CONFIG_EXTENSION);
		if (Files.notExists(configFile))
		{
			return;
		}

		List<String> newLines = new ArrayList<>();
		var alreadyEnabled = false;

		try (var bufferedReader = new BufferedReader(new FileReader(configFile.toFile())))
		{
			String line;
			while ((line = bufferedReader.readLine()) != null)
			{
				if (line.startsWith(JAVA_OPTIONS_SPLASH_DETECTION))
				{
					alreadyEnabled = true;
					if (!enable)
					{
						continue;
					}
				}
				newLines.add(line);
			}
		}
		catch (IOException e)
		{
			log.error("Failed to update the splash screen state (read)", e);
		}

		if (enable == alreadyEnabled)
		{
			// The file is already in the proper state, don't touch it
			return;
		}

		if (enable)
		{
			for (var i = 0; i < newLines.size(); i++)
			{
				if (newLines.get(i).equals(JAVA_OPTIONS))
				{
					newLines.add(i + 1, JAVA_OPTIONS_SPLASH);
					break;
				}
			}
		}

		try
		{
			var tmpFile = Files.createTempFile(configFile.getParent(), AppName.NAME, CONFIG_EXTENSION);
			Files.write(tmpFile, newLines);
			Files.move(tmpFile, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e)
		{
			log.error("Failed to update the splash screen state (write)", e);
		}
	}
}
