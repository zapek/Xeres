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
	private static final String /* 🙂 */ SLIGHTLY_SMILING_FACE = Character.toString(0x1F642);
	private static final String /* 😃 */ GRINNING_FACE_WITH_BIG_EYES = Character.toString(0x1F603);
	private static final String /* 😄 */ GRINNING_FACE_WITH_SMILING_EYES = Character.toString(0x1F604);
	private static final String /* 😅 */ GRINNING_FACE_WITH_SWEAT = Character.toString(0x1F605);
	private static final String /* 😂 */ FACE_WITH_TEARS_OF_JOY = Character.toString(0x1F602);
	private static final String /* 😠 */ ANGRY_FACE = Character.toString(0x1F620);
	private static final String /* 😶 */ FACE_WITHOUT_MOUTH = Character.toString(0x1F636);
	private static final String /* 😵 */ FACE_WITH_CROSSED_OUT_EYES = Character.toString(0x1F635);
	private static final String /* 😳 */ FLUSHED_FACE = Character.toString(0x1F633);
	private static final String /* 🙁 */ SLIGHTLY_FROWNING_FACE = Character.toString(0x1F641);
	private static final String /* 😮 */ FACE_WITH_OPEN_MOUTH = Character.toString(0x1F62E);
	private static final String /* 😘 */ FACE_BLOWING_A_KISS = Character.toString(0x1F618);
	private static final String /* 😉 */ WINKING_FACE = Character.toString(0x1F609);
	private static final String /* 😥 */ SAD_BUT_RELIEVED_FACE = Character.toString(0x1F625);
	private static final String /* 😛 */ FACE_WITH_TONGUE = Character.toString(0x1F61B);
	private static final String /* 😕 */ CONFUSED_FACE = Character.toString(0x1F615);
	private static final String /* 😇 */ SMILING_FACE_WITH_HALO = Character.toString(0x1F607);
	private static final String /* 😈 */ SMILING_FACE_WITH_HORNS = Character.toString(0x1F608);
	private static final String /* 😎 */ SMILING_FACE_WITH_SUNGLASSES = Character.toString(0x1F60E);
	private static final String /* 💖 */ SPARKLING_HEART = Character.toString(0x1F496);
	private static final String /* 🤡 */ CLOWN_FACE = Character.toString(0x1F921);

	private static final Map<String, String> smileys = Map.ofEntries(
			entry(":-)", SLIGHTLY_SMILING_FACE),
			entry(":)", SLIGHTLY_SMILING_FACE),
			entry(":-D", GRINNING_FACE_WITH_BIG_EYES),
			entry(":D", GRINNING_FACE_WITH_BIG_EYES),
			entry(":-DD", GRINNING_FACE_WITH_SMILING_EYES),
			entry(":DD", GRINNING_FACE_WITH_SMILING_EYES),
			entry("':)", GRINNING_FACE_WITH_SWEAT),
			entry("':-)", GRINNING_FACE_WITH_SWEAT),
			entry("':D", GRINNING_FACE_WITH_SWEAT),
			entry("':-D", GRINNING_FACE_WITH_SWEAT),
			entry(":')", FACE_WITH_TEARS_OF_JOY),
			entry(":'-)", FACE_WITH_TEARS_OF_JOY),
			entry(":-(", SLIGHTLY_FROWNING_FACE),
			entry(":(", SLIGHTLY_FROWNING_FACE),
			entry(":-O", FACE_WITH_OPEN_MOUTH),
			entry(":O", FACE_WITH_OPEN_MOUTH),
			entry(":-*", FACE_BLOWING_A_KISS),
			entry(":*", FACE_BLOWING_A_KISS),
			entry(";-)", WINKING_FACE),
			entry(";)", WINKING_FACE),
			entry(";-(", SAD_BUT_RELIEVED_FACE),
			entry(";(", SAD_BUT_RELIEVED_FACE),
			entry(":-P", FACE_WITH_TONGUE),
			entry(":P", FACE_WITH_TONGUE),
			entry(":p", FACE_WITH_TONGUE),
			entry(":-/", CONFUSED_FACE),
			entry(":/", CONFUSED_FACE),
			entry("O:-)", SMILING_FACE_WITH_HALO),
			entry("O:)", SMILING_FACE_WITH_HALO),
			entry(">:-)", SMILING_FACE_WITH_HORNS),
			entry(">:)", SMILING_FACE_WITH_HORNS),
			entry("B-)", SMILING_FACE_WITH_SUNGLASSES),
			entry("B)", SMILING_FACE_WITH_SUNGLASSES),
			entry(":@", ANGRY_FACE),
			entry(":-X", FACE_WITHOUT_MOUTH),
			entry(":-x", FACE_WITHOUT_MOUTH),
			entry(":X", FACE_WITHOUT_MOUTH),
			entry(":x", FACE_WITHOUT_MOUTH),
			entry("%-)", FACE_WITH_CROSSED_OUT_EYES),
			entry("%)", FACE_WITH_CROSSED_OUT_EYES),
			entry(":-$", FLUSHED_FACE),
			entry(":$", FLUSHED_FACE),
			entry("<3", SPARKLING_HEART),
			entry(":o)", CLOWN_FACE)
	);

	private SmileyUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * A smiley is detected if the following 2 conditions are met:
	 * <ul>
	 *     <li>preceded by nothing or a space</li>
	 *     <li>followed by nothing or a space, a dot, a comma or an end of line</li>
	 * </ul>
	 *
	 * @param s the string
	 * @return a string with smileys replaced by Unicode emojis
	 */
	public static String smileysToUnicode(String s)
	{
		if (s.length() >= 2 && (s.contains(":") || s.contains(";") || s.contains("B") || s.contains("%") || s.contains("<"))) // optimizations
		{
			for (var e : smileys.entrySet())
			{
				var index = 0;

				while ((index = s.indexOf(e.getKey(), index)) != -1)
				{
					if (isAlone(index, e.getKey(), s) || isProperlySeparated(index, e.getKey(), s))
					{
						s = s.substring(0, index) + e.getValue() + s.substring(index + e.getKey().length());
						index += e.getValue().length();
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
