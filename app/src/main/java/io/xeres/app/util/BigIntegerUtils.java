/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.util;

import java.math.BigInteger;
import java.util.Arrays;

public final class BigIntegerUtils
{
	private BigIntegerUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Gets a BigInteger as one's complement.
	 * <p>
	 * This is useful when requiring compatibility with OpenSSL's BN_bn2bin().
	 *
	 * @param value the signed value
	 * @return the value as a one's complement (that is, without leading zero if positive)
	 */
	public static byte[] getAsOneComplement(BigInteger value)
	{
		var bytes = value.toByteArray();

		if (bytes[0] == 0 && bytes.length > 1)
		{
			return Arrays.copyOfRange(bytes, 1, bytes.length);
		}
		return bytes;
	}
}
