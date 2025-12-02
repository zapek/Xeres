/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.util;

public final class RsUtils
{
	private RsUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Retroshare doesn't recognize markdown images, so we send them as an HTML img tag which is supported
	 * by Markdown.
	 *
	 * @param input the input
	 * @return the output, compatible with Retroshare
	 */
	public static String replaceImageLines(String input)
	{
		String[] lines = input.split("\n");
		var result = new StringBuilder();

		for (String line : lines)
		{
			if (line.startsWith("![](data:image/"))
			{
				// Extract everything between ![]( and )
				int start = line.indexOf("(data:image/");
				int end = line.lastIndexOf(")");
				if (end > start)
				{
					var imageData = line.substring(start + 1, end); // +1 to skip the opening (
					String imgTag = "<img src=\"" + imageData + "\"></img>";
					result.append(imgTag);
				}
				else
				{
					// If no closing parenthesis found, leave as-is
					result.append(line);
				}
			}
			else
			{
				result.append(line);
			}
			result.append("\n");
		}

		return result.toString();
	}
}
