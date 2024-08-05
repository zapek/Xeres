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
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	private static Stream<Character> getUpperCaseChars(int count)
	{
		var upperChars = SECURE_RANDOM.ints(count, 65, 91);
		return upperChars.mapToObj(data -> (char) data);
	}

	private static Stream<Character> getLowerCaseChars(int count)
	{
		var lowerChars = SECURE_RANDOM.ints(count, 97, 123);
		return lowerChars.mapToObj(data -> (char) data);
	}

	private static Stream<Character> getNumbers(int count)
	{
		var lowerChars = SECURE_RANDOM.ints(count, 48, 58);
		return lowerChars.mapToObj(data -> (char) data);
	}

	public static void nextPassword(char[] password)
	{
		Objects.requireNonNull(password);
		var size = password.length;
		if (size == 0)
		{
			throw new IllegalArgumentException("Password length must be at least 1");
		}
		if (size > 512)
		{
			throw new IllegalArgumentException("Password length must be less than or equal to 512");
		}

		var upperSize = size / 3;
		var lowerSize = size / 3;
		size -= lowerSize + upperSize;
		var numberSize = size;

		var passwordList = Stream.concat(getUpperCaseChars(upperSize),
						Stream.concat(getLowerCaseChars(lowerSize),
								getNumbers(numberSize)))
				.collect(Collectors.toList());

		Collections.shuffle(passwordList);

		for (var i = 0; i < passwordList.size(); i++)
		{
			password[i] = passwordList.get(i);
		}
	}
}
