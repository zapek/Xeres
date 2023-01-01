/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

import org.apache.commons.lang3.ArrayUtils;

import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Contains ID conversion and representation methods. Used for locations, PGP identifiers, identities and so on.
 */
public final class Id
{
	private static final char[] HEX = "0123456789abcdef".toCharArray();

	private Id()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Converts a series of bytes into its hexadecimal representation. For example if the
	 * byte array contains 2 bytes like 28 then 3, the result is "1c03".
	 *
	 * @param id the id as a stream of bytes
	 * @return the lowercase hexadecimal representation of those bytes, without any prefix or an empty string if the id is null or empty
	 */
	public static String toString(byte[] id)
	{
		if (ArrayUtils.isEmpty(id))
		{
			return "";
		}

		var sb = new StringBuilder(id.length * 2);

		for (var b : id)
		{
			sb.append(HEX[(b & 0xf0) >> 4])
					.append(HEX[b & 0x0f]);
		}
		return sb.toString();
	}

	/**
	 * Converts a hexadecimal string into an array of bytes. For example
	 * if the input contains "1c03", then the result is an array of 2 bytes with 28 then 3.
	 *
	 * @param id the values as a lowercase hexadecimal series of bytes, without prefix
	 * @return an array of bytes containing those values or an empty array if the id is null or empty
	 */
	public static byte[] toBytes(String id)
	{
		if (isEmpty(id))
		{
			return new byte[0];
		}

		var out = new byte[id.length() / 2];

		for (var i = 0; i < out.length; i++)
		{
			var index = i * 2;
			out[i] = (byte) Integer.parseUnsignedInt(id.substring(index, index + 2), 16);
		}
		return out;
	}

	/**
	 * Converts an id into its hexadecimal representation.
	 *
	 * @param id the id
	 * @return a hexadecimal uppercase representation of the id, without prefix
	 */
	public static String toString(long id)
	{
		return Long.toHexString(id).toUpperCase(Locale.ROOT);
	}

	/**
	 * Converts an id into its hexadecimal representation.
	 *
	 * @param id the id
	 * @return a hexadecimal lowercase representation of the id, without prefix
	 */
	public static String toStringLowerCase(long id)
	{
		return Long.toHexString(id);
	}

	/**
	 * Converts an identifier into its hexadecimal representation.
	 *
	 * @param identifier the identifier
	 * @return a hexadecimal lowercase representation of the identifier, without prefix
	 */
	public static String toString(Identifier identifier)
	{
		return toString(identifier.getBytes());
	}

	/**
	 * Converts a string containing a hexadecimal ASCII representation of bytes into an array of
	 * the corresponding byte values. For example, if the string contains "3133" (0x31 ('1') and 0x33 ('3'))
	 * which represents 0x13, the result is an array of bytes which is { 0x13 }.
	 *
	 * @param id a string of hexadecimal ASCII values
	 * @return an array of corresponding values
	 */
	public static byte[] asciiStringToBytes(String id)
	{
		return asciiToBytes(id.getBytes());
	}

	/**
	 * Converts an array containing a hexadecimal ASCII representation of bytes into an array of
	 * the corresponding byte values. For example, if the array contains 0x31 ('1') and 0x33 ('3')
	 * which represents 0x13, the result is an array of bytes which is { 0x13 }.
	 *
	 * @param id an array of hexadecimal ASCII values
	 * @return an array of corresponding values
	 */
	public static byte[] asciiToBytes(byte[] id)
	{
		if (ArrayUtils.isEmpty(id))
		{
			throw new IllegalArgumentException("id is null or empty");
		}

		if (id.length % 2 == 1)
		{
			throw new IllegalArgumentException("id is not even");
		}

		var result = new byte[id.length / 2];
		byte number;
		var accumulator = 0;

		for (var i = 0; i < id.length; i++)
		{
			number = id[i];

			if (number >= 'a')
			{
				if (number > 'f')
				{
					throw new IllegalArgumentException("id has an invalid ascii value: " + number);
				}
				number -= 'a';
				number += (byte) 10;
			}
			else if (number >= '0')
			{
				number -= '0';
			}
			else
			{
				throw new IllegalArgumentException("id has an invalid ascii value: " + number);
			}

			if (i % 2 == 1)
			{
				result[i / 2] = (byte) (accumulator * 16 + number);
			}
			else
			{
				accumulator = number;
			}
		}
		return result;
	}

	/**
	 * Converts an identifier to its ASCII representation. For example if the identifier is 0x12, then its
	 * ASCII representation will be { 0x31, 0x32 } ('1' and '2').
	 *
	 * @param identifier an identifier
	 * @return the byte array containing the ASCII values of each number of the identifier. The array is twice as long as the input
	 */
	public static byte[] toAsciiBytes(Identifier identifier)
	{
		return Id.toString(identifier).getBytes();
	}

	/**
	 * Same as {@link #toAsciiBytes(Identifier)} but in upper case.
	 *
	 * @param identifier an identifier
	 * @return the byte array containing the ASCII values of each number of the identifier in upper case. The array
	 * is twice as long as the input
	 */
	public static byte[] toAsciiBytesUpperCase(Identifier identifier)
	{
		return Id.toString(identifier).toUpperCase(Locale.ROOT).getBytes();
	}
}
