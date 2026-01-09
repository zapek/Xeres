/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

/**
 * Utility class where all time and date displays are located. It only supports
 * ISO style.
 */
public final class DateUtils
{
	/**
	 * Formats the date and time, like: 2026-01-06 21:39
	 */
	public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
			.withZone(ZoneId.systemDefault());

	/**
	 * Formats the date and time with seconds, like: 2026-01-06 21:40:36
	 */
	public static final DateTimeFormatter DATE_TIME_PRECISE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	/**
	 * Formats the time in a localized way, like: 21:37
	 */
	public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
			.withZone(ZoneId.systemDefault());

	/**
	 * Formats the date only, like: 2026-01-06
	 */
	public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
			.withZone(ZoneId.systemDefault());

	/**
	 * Formats the time with seconds in a localized way, like: 21:41:38
	 */
	public static final DateTimeFormatter TIME_PRECISE_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	/**
	 * Formats the date and time, to be used as a filename, like: 2026-01-06_214229
	 */
	public static final DateTimeFormatter DATE_TIME_FILENAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
			.withZone(ZoneId.systemDefault());

	private DateUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}
}
