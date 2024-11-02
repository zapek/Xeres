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

package io.xeres.ui.support.util;

public final class PublicKeyUtils
{
	private PublicKeyUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static String getKeyAlgorithmName(int algorithm)
	{
		return switch (algorithm)
		{
			case 1 -> "RSA";
			case 2 -> "RSA Encrypt Only (!)";
			case 3 -> "RSA Sign Only (!)";
			case 16 -> "Elgamal";
			case 17 -> "DSA";
			case 18 -> "ECDH";
			case 19 -> "ECDSA";
			case 20 -> "Elgamal General (!)";
			case 21 -> "Diffie Hellman";
			case 22 -> "EdDSA";
			case 23 -> "AEDH";
			case 24 -> "AEDSA";
			case 25 -> "x25519";
			case 26 -> "x448";
			case 27 -> "Ed25519";
			case 28 -> "Ed448";
			default -> "Unknown (" + algorithm + ")";
		};
	}

	public static String getSignatureHash(int hash)
	{
		return switch (hash)
		{
			case 1 -> "MD5";
			case 2 -> "SHA-1";
			case 3 -> "RIPEMD-160";
			case 8 -> "SHA-256";
			case 9 -> "SHA-384";
			case 10 -> "SHA-512";
			case 11 -> "SHA-224";
			case 12 -> "SHA3-256";
			case 14 -> "SHA3-512";
			default -> "Unknown (" + hash + ")";
		};
	}
}
