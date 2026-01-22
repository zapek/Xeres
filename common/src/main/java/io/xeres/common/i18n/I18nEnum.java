/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.common.i18n;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public interface I18nEnum
{
	/**
	 * Returns the message key for an enum. The format is:
	 * {@code enum.(<enclosing class>.)<enum name>.<enum value>} all in lower case.
	 *
	 * @param e the enum
	 * @return the enum message key
	 */
	default String getMessageKey(Enum<?> e)
	{
		var enumClass = e.getClass();
		var sb = new StringBuilder("enum.");
		if (enumClass.getEnclosingClass() != null)
		{
			sb.append(getAsKebabCase(enumClass.getEnclosingClass().getSimpleName()));
			sb.append(".");
		}
		sb.append(getAsKebabCase(enumClass.getSimpleName()));
		sb.append(".");
		sb.append(e.name().toLowerCase(Locale.ROOT));
		return sb.toString();
	}

	private static String getAsKebabCase(String input)
	{
		return StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(input), "-").toLowerCase(Locale.ROOT);
	}
}
