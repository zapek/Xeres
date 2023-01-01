/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
		return registryValueExists(HKEY_CURRENT_USER, REGISTRY_RUN_PATH, AppName.NAME) && isSplashScreenHidden();
	}

	@Override
	public void enable()
	{
		registrySetStringValue(HKEY_CURRENT_USER, REGISTRY_RUN_PATH, AppName.NAME, "\"" + getApplicationPath() + "\"" + " --iconified");
		updateSplashScreen(false);
	}

	@Override
	public void disable()
	{
		registryDeleteValue(HKEY_CURRENT_USER, REGISTRY_RUN_PATH, AppName.NAME);
		updateSplashScreen(true);
	}

	private String getApplicationPath()
	{
		if (applicationPath != null)
		{
			return applicationPath.toString();
		}

		Path basePath;
		File file;
		try
		{
			var uri = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
			file = uriToFile(uri.toString());
		}
		catch (URISyntaxException | MalformedURLException e)
		{
			log.error("Invalid application path", e);
			return null;
		}

		basePath = file.toPath();

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

	/**
	 * Converts the given URL string to its corresponding {@link File}.
	 * Source: <a href="https://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file">stack overflow</a>
	 *
	 * @param url The URL to convert.
	 * @return A file path suitable for use with e.g. {@link FileInputStream}
	 * @throws IllegalArgumentException if the URL does not correspond to a file.
	 */
	private static File uriToFile(String url) throws MalformedURLException, URISyntaxException
	{
		var path = url;
		if (path.startsWith("jar:"))
		{
			// remove "jar:" prefix and "!/" suffix
			var index = path.indexOf("!/");
			path = path.substring(4, index);
		}
		try
		{
			// For Windows only
			if (path.matches("file:[A-Za-z]:.*"))
			{
				path = "file:/" + path.substring(5);
			}
			return new File(new URL(path).toURI());
		}
		catch (MalformedURLException | URISyntaxException e)
		{
			// NB: URL is not completely well-formed.
			if (path.startsWith("file:"))
			{
				// pass through the URL as-is, minus "file:" prefix
				path = path.substring(5);
				return new File(path);
			}
			throw e;
		}
	}

	private boolean isSplashScreenHidden()
	{
		var configFile = getStartupConfigFile();
		if (configFile == null)
		{
			return false;
		}

		try (var bufferedReader = new BufferedReader(new FileReader(configFile.toFile())))
		{
			String line;
			while ((line = bufferedReader.readLine()) != null)
			{
				if (line.startsWith(JAVA_OPTIONS_SPLASH_DETECTION))
				{
					return false;
				}
			}
		}
		catch (IOException e)
		{
			log.error("Failed to check the config file's content", e);
			return false; // we don't know the status, but let's settle for false
		}
		return true;
	}

	private Path getStartupConfigFile()
	{
		Objects.requireNonNull(applicationPath);

		var configFile = applicationPath.resolveSibling(APP_DIRECTORY + AppName.NAME + CONFIG_EXTENSION);
		if (Files.notExists(configFile))
		{
			return null;
		}
		return configFile;
	}

	private void updateSplashScreen(boolean enable)
	{
		var configFile = getStartupConfigFile();
		if (configFile == null)
		{
			log.error("Failed to update splash screen: startup config file not found");
			return;
		}

		List<String> lines = new ArrayList<>();
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
				lines.add(line);
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
			for (var i = 0; i < lines.size(); i++)
			{
				if (lines.get(i).equals(JAVA_OPTIONS))
				{
					lines.add(i + 1, JAVA_OPTIONS_SPLASH);
					break;
				}
			}
		}
		writeStartupConfigFile(configFile, lines);
	}

	private static void writeStartupConfigFile(Path configFile, List<String> newLines)
	{
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
