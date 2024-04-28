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

package io.xeres.app.crypto.scramble;

import io.xeres.app.crypto.hash.sha256.Sha256MessageDigest;
import io.xeres.common.util.SecureRandomUtils;

import java.util.Arrays;
import java.util.Base64;

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
 * <p>
 * Note: the interface is similar to Sun's GuardedString.
 */
public class ScrambledString
{
	/**
	 * Callback to access the clear text of the secure string.
	 * If possible, prefer the use of verifyBase64SHA256Hash()
	 * which avoids unscrambling the secure string.
	 */
	public interface Accessor
	{
		void access(char[] clearChars);
	}

	private boolean disposed;
	private byte[] padBytes;
	private byte[] scrambledBytes;
	private String hash;
	private final Sha256MessageDigest digest;

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
		digest = new Sha256MessageDigest();
		scrambleChars(clearChars);
	}

	/**
	 * Allow access to the cleartext characters.
	 * <p>
	 * They're only available during the call and are automatically
	 * cleared afterwards.
	 *
	 * @param accessor the Accessor callback
	 * @throws IllegalStateException the string has been disposed already
	 */
	public void access(Accessor accessor)
	{
		checkNotDisposed();
		char[] clearText = null;
		try
		{
			clearText = unscrambleChars();
			accessor.access(clearText);
		}
		finally
		{
			clear(clearText);
		}
	}

	/**
	 * Append a single char to the scrambled string.
	 *
	 * @param c the character to append
	 * @throws IllegalStateException if the string has been disposed already
	 */
	public void appendChar(char c)
	{
		checkNotDisposed();
		char[] oldArray = null;
		char[] newArray = null;

		try
		{
			oldArray = unscrambleChars();
			newArray = new char[oldArray.length + 1];
			System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
			newArray[newArray.length - 1] = c;
			scrambleChars(newArray);
		}
		finally
		{
			clear(oldArray);
			clear(newArray);
		}

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

	/**
	 * Check that the base64 encoded SHA-256 hash of the original clear text
	 * matches the given value.
	 *
	 * @param hash the base64 encoded SHA-256 hash of the clear text
	 * @return true of the hash matches the original clear text's hash
	 */
	public boolean verifyBase64SHA256Hash(String hash)
	{
		checkNotDisposed();
		return this.hash.equals(hash);
	}

	/**
	 * Get the base64 encoded SHA-256 hash of the original clear text. Useful to
	 * store in a database for example.
	 *
	 * @return the base64 encoded SHA-256 hash of the original clear text
	 */
	public String getBase64SHA256Hash()
	{
		checkNotDisposed();
		return hash;
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
		digest.update(bytes);
		hash = Base64.getEncoder().encodeToString(digest.getBytes());
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
		var bytes = new byte[chars.length];
		for (var i = 0; i < chars.length; i++)
		{
			bytes[i] = (byte) chars[i];
		}
		return bytes;
	}

	private char[] bytesToChars(byte[] bytes)
	{
		var chars = new char[bytes.length];
		for (var i = 0; i < bytes.length; i++)
		{
			chars[i] = (char) bytes[i];
		}
		return chars;
	}

	private void clear(byte[] bytes)
	{
		if (bytes == null)
		{
			return;
		}
		Arrays.fill(bytes, (byte) 0);
	}

	private void clear(char[] chars)
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
	public boolean equals(Object obj)
	{
		if (obj instanceof ScrambledString other)
		{
			return hash.equals(other.hash);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return hash.hashCode();
	}

	@Override
	public String toString()
	{
		return disposed ? "" : "[SCRAMBLED]";
	}
}
