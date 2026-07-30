/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * String obfuscator. This class is used to store a password in memory (for example after asking
 * the user for a password). Instead of storing the password in clear text, it is stored in a
 * scrambled form which makes it harder to recover if it ever ends up in a memory dump.
 * <p>
 * Once the password has been handled, it is recommended to call dispose() to clear it.
 * <p>
 * Please be wary that it is still possible to recover the password if the attacker knows the
 * memory layout and what he's looking for but at least the password won't show up for a simple
 * string search.
 */
public class ScrambledString
{
	private boolean disposed;
	private byte[] padBytes;
	private byte[] scrambledBytes;

	/**
	 * Create an empty scrambled string.
	 */
	public ScrambledString()
	{
		this(new char[0]);
	}

	/**
	 * Create a scrambled string from cleartext characters.
	 * The caller is responsible for clearing the cleartext characters itself.
	 *
	 * @param clearChars the cleartext characters
	 */
	public ScrambledString(char[] clearChars)
	{
		scrambleChars(clearChars);
	}

	public ScrambledString(byte[] clearBytes)
	{
		scrambleChars(bytesToChars(clearBytes));
	}

	public ScrambledString(String clearString)
	{
		this(clearString.toCharArray());
	}

	/**
	 * Allows access to the cleartext characters.
	 * <p>Don't forget to clear the array with the {@link #clear(char[])} call as soon as possible (ideally
	 * in a finally block)
	 * @return the cleartext
	 */
	public char[] getAsCharArrayToClear()
	{
		checkNotDisposed();
		return unscrambleChars();
	}

	public byte[] getAsByteArrayToClear()
	{
		checkNotDisposed();
		return unscrambleBytes();
	}

	/**
	 * Allows access to the cleartext string.
	 * <p>Only use this method if you have no alternative (when it has to be a string). Because
	 * it's not possible to clear it manually. Prefer {@link #getAsCharArrayToClear()}
	 *
	 * @return the cleartext
	 */
	public String getAsInsecureString()
	{
		return new String(getAsCharArrayToClear());
	}

	/**
	 * Clear the scrambled string. Should be called as soon as we're done with the
	 * string. Note that the string cannot be reused afterwards and a new one must be
	 * created.
	 */
	public void dispose()
	{
		clear(scrambledBytes);
		disposed = true;
	}

	private void regeneratePad(int length)
	{
		clear(padBytes);
		padBytes = new byte[length];
		SecureRandomUtils.nextBytes(padBytes);
	}

	private void scrambleBytes(byte[] bytes)
	{
		regeneratePad(bytes.length);
		var newBytes = new byte[bytes.length];

		for (var i = 0; i < bytes.length; i++)
		{
			newBytes[i] = (byte) (padBytes[i] ^ bytes[i]);
		}

		clear(scrambledBytes);
		scrambledBytes = newBytes;
	}

	private byte[] unscrambleBytes()
	{
		var unscrambledBytes = new byte[scrambledBytes.length];
		for (var i = 0; i < scrambledBytes.length; i++)
		{
			unscrambledBytes[i] = (byte) (padBytes[i] ^ scrambledBytes[i]);
		}
		return unscrambledBytes;
	}

	private void scrambleChars(char[] chars)
	{
		byte[] clearBytes = null;
		try
		{
			clearBytes = charsToBytes(chars);
			scrambleBytes(clearBytes);
		}
		finally
		{
			clear(clearBytes);
		}
	}

	private char[] unscrambleChars()
	{
		var unscrambledBytes = unscrambleBytes();
		var unscrambledChars = bytesToChars(unscrambledBytes);
		clear(unscrambledBytes);
		return unscrambledChars;
	}

	private byte[] charsToBytes(char[] chars)
	{
		CharsetEncoder encoder = UTF_8.newEncoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);

		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer;

		try
		{
			byteBuffer = encoder.encode(charBuffer);
		}
		catch (CharacterCodingException e)
		{
			throw new IllegalArgumentException("Invalid character data for UTF‑8 encoding", e);
		}

		byte[] result = new byte[byteBuffer.remaining()];
		byteBuffer.get(result);
		return result;
	}

	private char[] bytesToChars(byte[] bytes)
	{
		CharsetDecoder decoder = UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);

		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		CharBuffer charBuffer;

		try
		{
			charBuffer = decoder.decode(byteBuffer);
		}
		catch (CharacterCodingException e)
		{
			throw new IllegalArgumentException("Invalid UTF‑8 byte sequence", e);
		}

		char[] result = new char[charBuffer.remaining()];
		charBuffer.get(result);
		return result;
	}

	public static void clear(byte[] bytes)
	{
		if (bytes == null)
		{
			return;
		}
		Arrays.fill(bytes, (byte) 0);
	}

	public static void clear(char[] chars)
	{
		if (chars == null)
		{
			return;
		}
		Arrays.fill(chars, (char) 0);
	}

	private void checkNotDisposed()
	{
		if (disposed)
		{
			throw new IllegalStateException("String is disposed already");
		}
	}

	@Override
	public String toString()
	{
		return disposed ? "" : "[SCRAMBLED]";
	}
}
