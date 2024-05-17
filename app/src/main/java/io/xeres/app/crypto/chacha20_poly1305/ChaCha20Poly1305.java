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

package io.xeres.app.crypto.chacha20_poly1305;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Objects;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

public final class ChaCha20Poly1305
{
	private static final String ENCRYPTION_TRANSFORMATION = "ChaCha20-Poly1305"; // XXX: replaced the "ChaCha20-Poly1305/None/NoPadding", I think it means we don't need to do the padding ourselves...
	private static final String ENCRYPTION_ALGORITHM = "ChaCha20";

	private ChaCha20Poly1305()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static SecretKey generateKey()
	{
		try
		{
			var keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
			keyGenerator.init(256);
			return keyGenerator.generateKey();
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Encrypts using ChaCha20-Poly1305.
	 *
	 * @param key                         the secret key
	 * @param nonce                       a unique, securely generated nonce
	 * @param plainText                   the data to encrypt
	 * @param additionalAuthenticatedData additional authenticated data. Can be used to authenticate the nonce
	 * @return the encrypted data
	 */
	public static byte[] encrypt(SecretKey key, byte[] nonce, byte[] plainText, byte[] additionalAuthenticatedData)
	{
		Objects.requireNonNull(key);
		Objects.requireNonNull(nonce);
		if (nonce.length != 12)
		{
			throw new IllegalArgumentException("Nonce must be 12 bytes");
		}
		Objects.requireNonNull(plainText);
		Objects.requireNonNull(additionalAuthenticatedData);

		try
		{
			var cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
			AlgorithmParameterSpec ivParameterSpec = new IvParameterSpec(nonce);
			var keySpec = new SecretKeySpec(key.getEncoded(), ENCRYPTION_ALGORITHM);
			cipher.init(ENCRYPT_MODE, keySpec, ivParameterSpec);
			cipher.updateAAD(additionalAuthenticatedData);
			return cipher.doFinal(plainText);
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Decrypts using ChaCha20-Poly1305.
	 *
	 * @param key                         the secret key
	 * @param nonce                       the unique, securely generated nonce that was used for the encryption
	 * @param cipherText                  the encrypted data
	 * @param additionalAuthenticatedData additional authenticated data. Can be used to authenticate the nonce
	 * @return the decrypted data
	 */
	public static byte[] decrypt(SecretKey key, byte[] nonce, byte[] cipherText, byte[] additionalAuthenticatedData)
	{
		Objects.requireNonNull(key);
		Objects.requireNonNull(nonce);
		if (nonce.length != 12)
		{
			throw new IllegalArgumentException("Nonce must be 12 bytes");
		}
		Objects.requireNonNull(cipherText);
		Objects.requireNonNull(additionalAuthenticatedData);

		try
		{
			var cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
			AlgorithmParameterSpec ivParameterSpec = new IvParameterSpec(nonce);
			var keySpec = new SecretKeySpec(key.getEncoded(), ENCRYPTION_ALGORITHM);
			cipher.init(DECRYPT_MODE, keySpec, ivParameterSpec);
			cipher.updateAAD(additionalAuthenticatedData);
			return cipher.doFinal(cipherText);
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e)
		{
			throw new IllegalArgumentException(e);
		}
	}
}
