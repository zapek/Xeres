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

package io.xeres.common.util;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class OsUtils
{
	private static final Logger log = LoggerFactory.getLogger(OsUtils.class);

	private static final String CASE_FILE_PREFIX = "XeresFileSystemCaseDetectorFile";
	private static final String CASE_FILE_EXTENSION = "tmp";

	private OsUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Checks if a file system is case-sensitive.
	 *
	 * @param path the directory path in the filesystem hierarchy. The location must be writable.
	 * @return true if case-sensitive
	 */
	public static boolean isFileSystemCaseSensitive(Path path)
	{
		Path lowerFile;
		Path upperFile = null;
		try
		{
			lowerFile = createFileSystemDetectionFile(path, false);
		}
		catch (IOException e)
		{
			log.warn("Couldn't write file for filesystem case detection: {}, using OS guess workaround", e.getMessage());
			return isOsCaseSensitive();
		}

		try
		{
			upperFile = createFileSystemDetectionFile(path, true);
		}
		catch (FileAlreadyExistsException e)
		{
			return false;
		}
		catch (IOException e)
		{
			log.error("Couldn't write second file for filesystem case detection: {}, shouldn't happen but using OS guess workaround anyway", e.getMessage());
			return isOsCaseSensitive();
		}
		finally
		{
			try
			{
				if (lowerFile != null)
				{
					Files.deleteIfExists(lowerFile);
				}
				if (upperFile != null)
				{
					Files.deleteIfExists(upperFile);
				}
			}
			catch (IOException e)
			{
				log.error("Error while deleting filesystem detection files: {}", e.getMessage());
			}
		}
		return true;
	}

	/**
	 * Executes a shell command and its arguments, for example:
	 * <p>
	 * <code>
	 * shellExecute("ls", "-al");
	 * </code>
	 * </p>
	 *
	 * @param args the command and its arguments
	 * @return the resulting output, line by line (with a {@code \n} separator at the end of each line), or a string starting with "Error: " and the message.
	 */
	public static String shellExecute(String... args)
	{
		var sb = new StringBuilder();
		try
		{
			var processBuilder = new ProcessBuilder(args);
			var process = processBuilder.start();
			var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null)
			{
				sb.append(line).append("\n");
			}
		}
		catch (IOException e)
		{
			return "Error: " + e.getMessage();
		}
		return sb.toString();
	}

	/**
	 * Opens a file like if it was launched from a graphical shell (for example by double-clicking on it).
	 *
	 * @param file the file to open
	 * @throws IllegalStateException if the file doesn't exist or the OS has troubles launching it
	 */
	public static void shellOpen(File file)
	{
		if (!file.exists())
		{
			throw new IllegalStateException("Couldn't open the file " + file + " because it doesn't exist");
		}

		try
		{
			Desktop.getDesktop().open(file);
		}
		catch (IOException | UnsupportedOperationException e)
		{
			throw new IllegalStateException("Couldn't open the file " + file + ": " + e.getMessage());
		}
	}

	/**
	 * Opens the folder with the file selected.
	 *
	 * @param file the file to show in the folderr
	 * @throws IllegalStateException if the file doesn't exist or the OS has troubles launching a file browser
	 */
	public static void showInFolder(File file)
	{
		if (!file.exists())
		{
			throw new IllegalStateException("Couldn't show the folder of the file " + file + " because the later doesn't exist");
		}

		try
		{
			Desktop.getDesktop().browseFileDirectory(file);
		}
		catch (UnsupportedOperationException e)
		{
			if (SystemUtils.IS_OS_WINDOWS)
			{
				try
				{
					new ProcessBuilder("explorer.exe", "/select,", file.getCanonicalPath()).start();
				}
				catch (IOException ex)
				{
					throw new IllegalStateException("Couldn't show the folder of the file " + file + ": " + ex.getMessage());
				}
			}
			else
			{
				throw new IllegalStateException("Couldn't show the folder of the file " + file + ": " + e.getMessage());
			}
		}
	}

	public static void showFolder(File directory)
	{
		if (!directory.exists())
		{
			throw new IllegalStateException("Couldn't show the folder " + directory + " because it doesn't exist");
		}

		if (!directory.isDirectory())
		{
			throw new IllegalStateException("Couldn't show the folder " + directory + " because it is not a directory");
		}

		try
		{
			Desktop.getDesktop().browseFileDirectory(directory); // This is not exactly what we want
		}
		catch (UnsupportedOperationException e)
		{
			if (SystemUtils.IS_OS_WINDOWS)
			{
				try
				{
					new ProcessBuilder("explorer.exe", directory.getCanonicalPath()).start();
				}
				catch (IOException ex)
				{
					throw new IllegalStateException("Couldn't show the folder " + directory + ": " + ex.getMessage());
				}
			}
			else
			{
				throw new IllegalStateException("Couldn't show the folder " + directory + ": " + e.getMessage());
			}
		}
	}

	private static Path createFileSystemDetectionFile(Path path, boolean upperCase) throws IOException
	{
		var file = path.toFile();
		var pid = ManagementFactory.getRuntimeMXBean().getPid();
		var pathCaseFile = Path.of((upperCase ? CASE_FILE_PREFIX.toUpperCase(Locale.ROOT) : CASE_FILE_PREFIX.toLowerCase(Locale.ROOT)) + "_" + pid + "." + CASE_FILE_EXTENSION);

		if (file.isDirectory())
		{
			return Files.createFile(path.resolve(pathCaseFile));
		}
		else if (file.isFile())
		{
			return Files.createFile(path.resolveSibling(pathCaseFile));
		}
		else
		{
			throw new IllegalStateException("Created path is not a directory nor a file");
		}
	}

	private static boolean isOsCaseSensitive()
	{
		if (SystemUtils.IS_OS_LINUX)
		{
			return true;
		}
		else if (SystemUtils.IS_OS_MAC)
		{
			return false;
		}
		else if (SystemUtils.IS_OS_WINDOWS)
		{
			return false;
		}
		else
		{
			throw new IllegalArgumentException("OS is unsupported");
		}
	}
}

