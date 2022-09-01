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

package io.xeres.ui.support.util;

import java.util.Map;

import static java.util.Map.entry;

public final class SmileyUtils
{
	private static final Map<String, String> smileys = Map.ofEntries(
			entry(":-)", "\u263A\uFE0F"),
			entry(":)", "\u263A\uFE0F"),
			entry(":o)", "\uD83E\uDD21"), // not working on linux
			entry(":-D", "\uD83D\uDE03"),
			entry(":D", "\uD83D\uDE03"),
			entry(":-(", "\u2639\uFE0F"),
			entry(":(", "\u2639\uFE0F"),
			entry(":-O", "\uD83D\uDE2E"),
			entry(":-*", "\uD83D\uDE18"),
			entry(";-)", "\uD83D\uDE09"),
			entry(";)", "\uD83D\uDE09"),
			entry(";-(", "\uD83D\uDE25"),
			entry(";(", "\uD83D\uDE25"),
			entry(":-P", "\uD83D\uDE1B"),
			entry(":-/", "\uD83D\uDE15"),
			entry("O:-)", "\uD83D\uDE07"),
			entry("O:)", "\uD83D\uDE07"),
			entry(">:-)", "\uD83D\uDE08"),
			entry(">:)", "\uD83D\uDE08"),
			entry("B-)", "\uD83D\uDE0E"),
			entry("<3", "\u2764"),
			entry("O_o", "\uD83D\uDE33"),
			entry("o_O", "\uD83D\uDE33")
	);

	private SmileyUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static String smileysToUnicode(String s)
	{
		// Only replace if first in the string or preceded by a space, to avoid false positives
		for (var e : smileys.entrySet())
		{
			if (s.regionMatches(0, e.getKey(), 0, e.getKey().length()))
			{
				s = e.getValue() + s.substring(e.getKey().length());
			}
			s = s.replace(" " + e.getKey(), " " + e.getValue());
		}
		return s;
	}
}
