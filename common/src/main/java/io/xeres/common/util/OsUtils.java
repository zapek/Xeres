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
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public final class OsUtils
{
	private static final Logger log = LoggerFactory.getLogger(OsUtils.class);

	private static final String CASE_FILE_PREFIX = "XeresFileSystemCaseDetectorFile";
	private static final String CASE_FILE_EXTENSION = "tmp";

	private static final Pattern INVALID_WINDOWS_FILE_CHARS = Pattern.compile("([\\\\/:*?\"<>|\\p{Cntrl}]|^nul$)", CASE_INSENSITIVE);
	private static final Pattern INVALID_LINUX_FILE_CHARS = Pattern.compile("[\\x00/]");
	private static final Pattern INVALID_MACOS_FILE_CHARS = Pattern.compile("[:/]");

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
		Objects.requireNonNull(path);
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
		Objects.requireNonNull(file);
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
	 * @param file the file to show in the folder
	 * @throws IllegalStateException if the file doesn't exist or the OS has troubles launching a file browser
	 */
	public static void showInFolder(File file)
	{
		Objects.requireNonNull(file);
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

	/**
	 * Opens the directory in the file explorer and lists its content
	 *
	 * @param directory the directory
	 */
	public static void showFolder(File directory)
	{
		Objects.requireNonNull(directory);
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

	/**
	 * Sanitizes a file name. Replaces non-valid characters by '_'.
	 *
	 * @param fileName the file name to sanitize
	 * @return a sanitized version of the file name, or the original one if there's nothing to sanitize
	 */
	public static String sanitizeFileName(String fileName)
	{
		Objects.requireNonNull(fileName);
		if (SystemUtils.IS_OS_WINDOWS)
		{
			// Any Unicode except control characters, \, /, :, *, ?, ", <, >, | and no spaces at the beginning
			// or the end. A single period at the end is automatically removed by the Win32 API.
			// Forget about the "invalids" CON, AUX, COM1...9, LPT1...9. Those are only restricted in a cmd.exe or by explorer.exe, but they are valid
			// file names (you can create them with PowerShell for example). Only NUL is restricted.
			return INVALID_WINDOWS_FILE_CHARS.matcher(fileName).replaceAll("_").trim();
		}
		else if (SystemUtils.IS_OS_MAC)
		{
			// MacOS is : and /
			return INVALID_MACOS_FILE_CHARS.matcher(fileName).replaceAll("_");
		}
		else // Assume the rest is Linux
		{
			// Linux is NUL and /
			return INVALID_LINUX_FILE_CHARS.matcher(fileName).replaceAll("_");
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

	/**
	 * Sets the security level of the file. This currently only works on Windows.
	 *
	 * @param path    the path of the file
	 * @param trusted if true, the security zone is set to Trusted Site Zone, otherwise it's set to Internet Zone
	 */
	public static void setFileSecurity(Path path, boolean trusted)
	{
		Objects.requireNonNull(path);
		if (SystemUtils.IS_OS_WINDOWS)
		{
			try (var ads = new RandomAccessFile(path + ":Zone.Identifier", "rw")) // We can't use Path.of() here as it won't accept the ':'
			{
				byte[] data = ("[ZoneTransfer]\r\nZoneId=" + (trusted ? "2" : "3") + "\r\nHostUrl=about:internet\r\n").getBytes();
				ads.write(data);
			}
			catch (IOException e)
			{
				log.warn("Couldn't set security zone of file {}: {}", path, e.getMessage());
			}
		}
	}

	/**
	 * Sets the visibility of the file.
	 * <p>
	 * Note: only works on Windows.
	 *
	 * @param path    the path of the file
	 * @param visible true if the file must be visible (default when creating new files), false otherwise
	 */
	public static void setFileVisible(Path path, boolean visible)
	{
		if (SystemUtils.IS_OS_WINDOWS)
		{
			try
			{
				Files.setAttribute(path, "dos:hidden", !visible);
			}
			catch (IOException e)
			{
				log.warn("Couldn't set the visibility of file at {}: {}", path, e.getMessage());
			}
		}
	}
}

