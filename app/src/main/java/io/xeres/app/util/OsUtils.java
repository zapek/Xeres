package io.xeres.app.util;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class OsUtils
{
	private static final Logger log = LoggerFactory.getLogger(OsUtils.class);

	private static final String CASE_FILE = "XeresFileSystemCaseDetectorFile";

	private OsUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static boolean isFileSystemCaseSensitive(Path path)
	{
		Path lowerFile = null;
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

	private static Path createFileSystemDetectionFile(Path path, boolean upperCase) throws IOException
	{
		var file = path.toFile();
		var pathCaseFile = Path.of(upperCase ? CASE_FILE.toUpperCase(Locale.ROOT) : CASE_FILE.toLowerCase(Locale.ROOT));

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

