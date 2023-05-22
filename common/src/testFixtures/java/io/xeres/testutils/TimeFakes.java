/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.testutils;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

public final class TimeFakes
{
	private TimeFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Instant createInstant()
	{
		var end = ZonedDateTime.now();
		var start = end.minus(5, ChronoUnit.YEARS);
		var random = ThreadLocalRandom.current().nextLong(start.toInstant().getEpochSecond(), end.toInstant().getEpochSecond());

		return Instant.ofEpochSecond(random);
	}
}