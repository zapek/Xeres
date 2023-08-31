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

package io.xeres.ui.support.util;

import java.util.Map;

import static java.util.Map.entry;

/**
 * Class to handle smiley to emoticon conversion.<br>
 *
 * @see <a href="https://en.wikipedia.org/wiki/List_of_emoticons">List of emoticons</a>
 * @see <a href="https://www.webfx.com/tools/emoji-cheat-sheet/">Emoji cheat sheet</a>
 */
public final class SmileyUtils
{
	private static final Map<String, String> smileys = Map.ofEntries(
			entry(":-)", "\uD83D\uDE42"),
			entry(":)", "\uD83D\uDE42"),
			entry(":o)", "\uD83E\uDD21"), // not working on linux
			entry(":-D", "\uD83D\uDE03"),
			entry(":D", "\uD83D\uDE03"),
			entry(":-(", "\uD83D\uDE41"),
			entry(":(", "\uD83D\uDE41"),
			entry(":-O", "\uD83D\uDE2E"),
			entry(":-*", "\uD83D\uDE18"),
			entry(";-)", "\uD83D\uDE09"),
			entry(";)", "\uD83D\uDE09"),
			entry(";-(", "\uD83D\uDE25"),
			entry(";(", "\uD83D\uDE25"),
			entry(":-P", "\uD83D\uDE1B"),
			entry(":P", "\uD83D\uDE1B"),
			entry(":p", "\uD83D\uDE1B"),
			entry(":-/", "\uD83D\uDE15"),
			entry(":/", "\uD83D\uDE15"),
			entry("O:-)", "\uD83D\uDE07"),
			entry("O:)", "\uD83D\uDE07"),
			entry(">:-)", "\uD83D\uDE08"),
			entry(">:)", "\uD83D\uDE08"),
			entry("B-)", "\uD83D\uDE0E"),
			entry("B)", "\uD83D\uDE0E"),
			entry("<3", "\uD83D\uDC96"),
			entry("O_o", "\uD83D\uDE33"),
			entry("o_O", "\uD83D\uDE33")
	);

	private SmileyUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * A smiley is detected on the following conditions:
	 * <ul>
	 *     <li>preceded by nothing or a space</li>
	 *     <li>followed by nothing or a space, a dot, a comma or an end of line</li>
	 * </ul>
	 *
	 * @param s the string
	 * @return a string with smileys replaced by unicode emojis
	 */
	public static String smileysToUnicode(String s)
	{
		if (s.length() >= 2)
		{
			for (var e : smileys.entrySet())
			{
				int index = 0;
				int searchIndex = 0;

				while ((index = s.indexOf(e.getKey(), index)) != -1)
				{
					if (isAlone(index, e.getKey(), s) || isProperlySeparated(index, e.getKey(), s))
					{
						s = s.substring(searchIndex, index) + e.getValue() + s.substring(index + e.getKey().length());
						searchIndex = index + e.getValue().length();
					}
					else
					{
						index += e.getKey().length(); // skip it then
					}
				}
			}
		}
		return s;
	}

	private static boolean isAlone(int index, String key, String s)
	{
		return index == 0 && key.length() == s.length();
	}

	private static boolean isProperlySeparated(int index, String key, String s)
	{
		return beginningIsSeparator(index, s) && (endIsSeparator(index, key, s));
	}

	private static boolean beginningIsSeparator(int index, String s)
	{
		return index == 0 || Character.isSpaceChar(s.charAt(index - 1));
	}

	private static boolean endIsSeparator(int index, String key, String s)
	{
		if (index + key.length() == s.length())
		{
			return true;
		}
		var c = s.charAt(index + key.length());
		return c == '.' || c == ',' || Character.isSpaceChar(c) || c == '\n';
	}
}
