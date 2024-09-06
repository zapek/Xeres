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

package io.xeres.ui.support.chat;

import org.apache.commons.lang3.StringUtils;

/**
 * An utility class to parse outgoing commands.
 */
public final class ChatCommand
{
	private ChatCommand()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * This parses outgoing commands so that they're formatted properly, currently supported:
	 * <p>
	 * <ul>
	 *     <li>code: formats code</li>
	 *     <li>pre: same as code</li>
	 * </ul>
	 *
	 * @param s the string to be processed
	 * @return the string with correct formatting
	 */
	public static String parseCommands(String s)
	{
		if (s == null)
		{
			return s;
		}
		if (StringUtils.isEmpty(s))
		{
			return s;
		}

		var pre = false;

		if (s.startsWith("/code "))
		{
			pre = true;

			s = s.substring(6);
		}
		else if (s.startsWith("/pre "))
		{
			pre = true;

			s = s.substring(5);
		}
		else if (s.startsWith("/quote "))
		{
			s = "\"" + s.substring(7) + "\"";
		}

		if (pre)
		{
			return "\n" + s.indent(4);
		}
		return s;
	}
}
