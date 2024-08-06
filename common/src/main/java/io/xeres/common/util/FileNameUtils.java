package io.xeres.common.util;

import java.util.Optional;
import java.util.regex.Pattern;

public final class FileNameUtils
{
	private static final Pattern EXTENSION = Pattern.compile("\\.(?=[^.]+$)");
	private static final Pattern DOWNLOAD_COUNT = Pattern.compile("\\((\\d{1,3})\\)$");

	private FileNameUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Renames the files in a way similar as Chromium.
	 *
	 * @param fileName the file name
	 * @return the filename with (1) appended or incremented
	 */
	public static String rename(String fileName)
	{
		var tokens = EXTENSION.split(fileName);
		if (tokens.length == 2)
		{
			// We have at least one extension, find out if it's
			// a .tar.gz style case so that we turn it into a
			// "(1).tar.gz" and not into a ".tar (1).gz".
			if (tokens[1].length() == 2)
			{
				var tarTokens = EXTENSION.split(tokens[0]);
				if (tarTokens.length == 2 && tarTokens[1].length() == 3)
				{
					return increment(tarTokens[0]) + "." + tarTokens[1] + "." + tokens[1];
				}
			}
			return increment(tokens[0]) + "." + tokens[1];
		}
		else
		{
			return increment(tokens[0]);
		}
	}

	public static Optional<String> getExtension(String fileName)
	{
		var tokens = EXTENSION.split(fileName);
		if (tokens.length == 2)
		{
			return Optional.of(tokens[1]);
		}
		return Optional.empty();
	}

	private static String increment(String input)
	{
		var matcher = DOWNLOAD_COUNT.matcher(input);
		if (matcher.find())
		{
			var count = Integer.parseInt(matcher.group(1));
			return input.substring(0, input.length() - (matcher.group(1).length() + 2)) + "(" + ++count + ")";
		}
		else
		{
			return input + " (1)";
		}
	}
}
