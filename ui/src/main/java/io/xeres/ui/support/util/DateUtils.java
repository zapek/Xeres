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

package io.xeres.ui.support.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public final class DateUtils
{
	public static final DateTimeFormatter DATE_TIME_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
			.withZone(ZoneId.systemDefault());

	public static final DateTimeFormatter DATE_TIME_PRECISE_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	public static final DateTimeFormatter TIME_DISPLAY = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
			.withLocale(Locale.ROOT)
			.withZone(ZoneId.systemDefault());

	public static final DateTimeFormatter TIME_DISPLAY_WITH_SECONDS = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
			.withLocale(Locale.ROOT)
			.withZone(ZoneId.systemDefault());

	public static final DateTimeFormatter DATE_TIME_FILENAME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
			.withZone(ZoneId.systemDefault());

	private DateUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}
}
