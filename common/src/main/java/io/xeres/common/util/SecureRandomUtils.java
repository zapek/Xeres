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

package io.xeres.common.util;

import java.security.SecureRandom;

/**
 * A utility class to get secure random numbers. Prefer this instead of using new SecureRandom() directly
 * as it's more efficient. If you don't need a secure random, use {@code ThreadLocalRandom.current()}.
 */
public final class SecureRandomUtils
{
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private SecureRandomUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static short nextShort()
	{
		return (short) SECURE_RANDOM.nextInt();
	}

	public static int nextInt()
	{
		return SECURE_RANDOM.nextInt();
	}

	public static long nextLong()
	{
		return SECURE_RANDOM.nextLong();
	}

	public static double nextDouble()
	{
		return SECURE_RANDOM.nextDouble();
	}

	public static void nextBytes(byte[] bytes)
	{
		SECURE_RANDOM.nextBytes(bytes);
	}
}
