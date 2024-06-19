package io.xeres.app.util;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
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
	 * @param path the path where to write the file to check, obviously used by the file system we want to check
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
	 * @return the resulting output, line by line (with a {@code \n} separator at the end of each line).
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
		catch (Exception e)
		{
			return "Error: " + e.getMessage();
		}
		return sb.toString();
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

