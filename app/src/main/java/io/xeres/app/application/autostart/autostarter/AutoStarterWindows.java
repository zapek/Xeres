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

package io.xeres.app.application.autostart.autostarter;

import io.xeres.app.application.autostart.AutoStarter;
import io.xeres.common.AppName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.sun.jna.platform.win32.Advapi32Util.*;
import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Handles the automatic startup of the application by Windows.
 * <p>
 * In case of problems, press ctrl-alt-del, launch the Task Manager, go to Startup apps and
 * make sure the status is set to Enabled.
 */
public class AutoStarterWindows implements AutoStarter
{
	private static final Logger log = LoggerFactory.getLogger(AutoStarterWindows.class);

	public static final String REGISTRY_RUN_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
	public static final String EXECUTABLE_EXTENSION = ".exe";

	private Path applicationPath;

	@Override
	public boolean isSupported()
	{
		return isNotBlank(getApplicationPath());
	}

	@Override
	public boolean isEnabled()
	{
		return registryValueExists(HKEY_CURRENT_USER, REGISTRY_RUN_PATH, AppName.NAME);
	}

	@Override
	public void enable()
	{
		registrySetStringValue(HKEY_CURRENT_USER, REGISTRY_RUN_PATH, AppName.NAME, "\"" + getApplicationPath() + "\"" + " --iconified");
	}

	@Override
	public void disable()
	{
		registryDeleteValue(HKEY_CURRENT_USER, REGISTRY_RUN_PATH, AppName.NAME);
	}

	/**
	 * Gets the application path.
	 * <p>
	 * Source: <a href="https://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file">stack overflow</a>
	 *
	 * @return the application path
	 */
	private String getApplicationPath()
	{
		if (applicationPath != null)
		{
			return applicationPath.toString();
		}

		var basePath = Paths.get(getClass().getProtectionDomain().getPermissions().elements().nextElement().getName()).toAbsolutePath();

		// We are located in 'app/something.jar', get the parent directory of 'app'
		if (basePath.getParent() == null || basePath.getParent().getParent() == null)
		{
			log.error("Couldn't get parent directory");
			return null;
		}

		var appPath = basePath.getParent().getParent().resolve(AppName.NAME + EXECUTABLE_EXTENSION);
		if (Files.notExists(appPath))
		{
			return null;
		}

		applicationPath = appPath.toAbsolutePath();

		log.info("Application path: {}", appPath);

		return applicationPath.toString();
	}
}
