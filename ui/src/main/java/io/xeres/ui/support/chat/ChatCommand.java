/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.chat;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * A utility class to parse outgoing commands.
 */
public final class ChatCommand
{
	private static final Logger log = LoggerFactory.getLogger(ChatCommand.class);

	private static final Pattern SPACE_PATTERN = Pattern.compile("\\s");

	public static final List<AliasEntry> ALIASES = List.of(
			new AliasEntry("code", "text", null, "Send the text as a block of code"),
			new AliasEntry("flip", null, null, "Flip a coin"),
			new AliasEntry("me", "message", null, "Send an action message in the third person"),
			new AliasEntry("pre", "text", null, "Send the text as preformatted"),
			new AliasEntry("quote", "text", null, "Send the text as a quote"),
			new AliasEntry("random", null, "max | min-max", "Send a random number from 1 to 10"),
			new AliasEntry("shrug", null, "target", "Send ¯\\_(ツ)_/¯"),
			new AliasEntry("table", null, "target", "Send (╯°□°)╯︵ ┻━┻")
	);

	private static final String COMMAND_CODE = "/code ";
	private static final String COMMAND_FLIP = "/flip";
	private static final String COMMAND_PRE = "/pre ";
	private static final String COMMAND_QUOTE = "/quote ";
	private static final String COMMAND_RANDOM = "/random";
	private static final String COMMAND_SHRUG = "/shrug";
	private static final String COMMAND_TABLE = "/table";

	private ChatCommand()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * This parses outgoing commands so that they're formatted properly.
	 *
	 * @param s the string to be processed
	 * @return the string with correct formatting
	 */
	public static String parseCommands(String s)
	{
		if (StringUtils.isEmpty(s))
		{
			return s;
		}

		var pre = false;

		if (s.startsWith(COMMAND_CODE))
		{
			pre = true;

			s = s.substring(COMMAND_CODE.length());
		}
		else if (s.startsWith(COMMAND_PRE))
		{
			pre = true;

			s = s.substring(COMMAND_PRE.length());
		}
		else if (s.startsWith(COMMAND_QUOTE))
		{
			s = "\n> " + s.substring(COMMAND_QUOTE.length());
		}
		else if (s.startsWith(COMMAND_FLIP))
		{
			return "🪙 (" + (ThreadLocalRandom.current().nextBoolean() ? "heads" : "tails") + ")";
		}
		else if (s.startsWith(COMMAND_RANDOM))
		{
			var min = 1;
			var max = 11;

			if (s.length() > COMMAND_RANDOM.length() + 1)
			{
				s = s.substring(COMMAND_RANDOM.length() + 1);

				try
				{
					s = SPACE_PATTERN.matcher(s).replaceAll("");
					if (s.contains("-"))
					{
						min = Integer.parseInt(s.substring(0, s.indexOf("-")));
						max = Integer.parseInt(s.substring(s.indexOf("-") + 1));
					}
					else
					{
						max = Integer.parseInt(s);
					}
				}
				catch (NumberFormatException | IndexOutOfBoundsException exception)
				{
					log.error("Couldn't parse /random input: [{}], {}", s, exception.getMessage());
				}
			}
			return "🎲 " + ThreadLocalRandom.current().nextInt(min, max);
		}
		else if (s.startsWith(COMMAND_SHRUG))
		{
			return suffixWithSpaceIfNeeded(s.substring(COMMAND_SHRUG.length())) + "¯\\\\\\_(ツ)\\_/¯";
		}
		else if (s.startsWith(COMMAND_TABLE))
		{
			return "(╯°□°)╯︵ ┻━┻" + prefixWithSpaceIfNeeded(s.substring(COMMAND_TABLE.length()));
		}

		if (pre)
		{
			return "\n" + s.indent(4);
		}
		return s;
	}

	private static String prefixWithSpaceIfNeeded(String s)
	{
		if (!StringUtils.isBlank(s))
		{
			return " " + s;
		}
		return "";
	}

	private static String suffixWithSpaceIfNeeded(String s)
	{
		if (!StringUtils.isBlank(s))
		{
			return s + " ";
		}
		return "";
	}
}
