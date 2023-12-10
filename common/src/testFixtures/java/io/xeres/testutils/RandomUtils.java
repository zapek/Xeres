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

import org.apache.commons.lang3.Validate;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Apache stupidly deprecated RandomUtils from lang3, so we use a subset here.
 */
public final class RandomUtils
{
	private RandomUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static boolean nextBoolean()
	{
		return random().nextBoolean();
	}

	public static byte[] nextBytes(int count)
	{
		Validate.isTrue(count >= 0, "Count cannot be negative.");

		final byte[] result = new byte[count];
		random().nextBytes(result);
		return result;
	}

	public static int nextInt()
	{
		return nextInt(0, Integer.MAX_VALUE);
	}

	public static int nextInt(int startInclusive, int endExclusive)
	{
		Validate.isTrue(endExclusive >= startInclusive, "Start value must be smaller or equal to end value.");
		Validate.isTrue(startInclusive >= 0, "Both range values must be non-negative.");

		if (startInclusive == endExclusive)
		{
			return startInclusive;
		}
		return startInclusive + random().nextInt(endExclusive - startInclusive);
	}

	public static double nextDouble()
	{
		return nextDouble(0, Double.MAX_VALUE);
	}

	public static double nextDouble(double startInclusive, double endExclusive)
	{
		Validate.isTrue(endExclusive >= startInclusive, "Start value must be smaller or equal to end value.");
		Validate.isTrue(startInclusive >= 0, "Both range values must be non-negative.");

		if (startInclusive == endExclusive)
		{
			return startInclusive;
		}
		return startInclusive + ((endExclusive - startInclusive) * random().nextDouble());
	}

	public static float nextFloat()
	{
		return nextFloat(0, Float.MAX_VALUE);
	}

	public static long nextLong()
	{
		return nextLong(Long.MAX_VALUE);
	}

	public static long nextLong(long startInclusive, long endExclusive)
	{
		Validate.isTrue(endExclusive >= startInclusive, "Start value must be smaller or equal to end value.");
		Validate.isTrue(startInclusive >= 0, "Both range values must be non-negative.");

		if (startInclusive == endExclusive)
		{
			return startInclusive;
		}
		return startInclusive + nextLong(endExclusive - startInclusive);
	}

	public static float nextFloat(float startInclusive, float endExclusive)
	{
		Validate.isTrue(endExclusive >= startInclusive, "Start value must be smaller or equal to end value.");
		Validate.isTrue(startInclusive >= 0, "Both range values must be non-negative.");

		if (startInclusive == endExclusive)
		{
			return startInclusive;
		}
		return startInclusive + ((endExclusive - startInclusive) * random().nextFloat());
	}

	private static long nextLong(long n)
	{
		long bits;
		long val;
		do
		{
			bits = random().nextLong() >>> 1;
			val = bits % n;
		}
		while (bits - val + (n - 1) < 0);
		return val;
	}

	private static ThreadLocalRandom random()
	{
		return ThreadLocalRandom.current();
	}
}
