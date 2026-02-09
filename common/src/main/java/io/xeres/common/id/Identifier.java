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

package io.xeres.common.id;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Arrays;

/**
 * Interface that represents an identifier of an object in the Retroshare protocol that doesn't fit
 * in a primitive type.
 * <br>
 * Note that, unlike Retroshare, there's no identifier full of zeroes, they are null instead.
 */
public interface Identifier
{
	String LENGTH_FIELD_NAME = "LENGTH";
	String NULL_FIELD_NAME = "NULL_IDENTIFIER";

	/**
	 * Gets a byte representation of the identifier.
	 *
	 * @return an array of bytes containing the identifier
	 */
	byte[] getBytes();

	/**
	 * Gets how many bytes are needed to store the identifier.
	 *
	 * @return the length of the identifier
	 */
	int getLength();

	/**
	 * Gets the representation of the identifier. To be used every time the identity is needed
	 * as a string (UI, headers, etc...).
	 *
	 * @return a string representation
	 */
	@Override
	String toString();

	@JsonIgnore
	default byte[] getNullIdentifier()
	{
		return createNullIdentifier(getLength());
	}

	default boolean isNullIdentifier()
	{
		return Arrays.equals(getNullIdentifier(), getBytes());
	}

	static byte[] createNullIdentifier(int length)
	{
		var a = new byte[length];
		Arrays.fill(a, (byte) 0);
		return a;
	}

	static byte[] parseString(String s, int length)
	{
		byte[] bytes;
		if (s == null || s.length() != length * 2)
		{
			bytes = createNullIdentifier(length);
		}
		else
		{
			try
			{
				bytes = Id.toBytes(s);
			}
			catch (NumberFormatException _)
			{
				bytes = createNullIdentifier(length);
			}
		}
		return bytes;
	}
}
