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

package io.xeres.app.crypto.ec;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public final class Ed25519
{
	private static final String KEY_ALGORITHM = "Ed25519";

	private Ed25519()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static KeyPair generateKeys(int size)
	{
		try
		{
			var keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);

			keyPairGenerator.initialize(size);

			return keyPairGenerator.generateKeyPair();
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalArgumentException("Algorithm not supported");
		}
	}
}
