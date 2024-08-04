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

package io.xeres.app.crypto.aead;

import io.xeres.app.crypto.hmac.sha256.Sha256HMac;

import javax.crypto.*;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Objects;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

public final class AEAD
{
	private static final String ENCRYPTION_TRANSFORMATION_CHACHA20_POLY1305 = "ChaCha20-Poly1305";
	private static final String ENCRYPTION_TRANSFORMATION_CHACHA20 = "ChaCha20";
	private static final String ENCRYPTION_ALGORITHM_CHACHA20 = "ChaCha20";
	private static final int TAG_SIZE = 16;

	private AEAD()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Generates a secret key.
	 *
	 * @return the secret key
	 */
	public static SecretKey generateKey()
	{
		try
		{
			var keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM_CHACHA20);
			keyGenerator.init(256);
			return keyGenerator.generateKey();
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Encrypts using ChaCha20 as an AEAD cipher with Poly1305 as the authenticator.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7539">RFC 7539</a>
	 * @param key                         the secret key
	 * @param nonce                       a unique, securely generated nonce
	 * @param plainText                   the data to encrypt
	 * @param additionalAuthenticatedData additional authenticated data. Can be used to authenticate the nonce
	 * @return the encrypted data
	 */
	public static byte[] encryptChaCha20Poly1305(SecretKey key, byte[] nonce, byte[] plainText, byte[] additionalAuthenticatedData)
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
			var cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION_CHACHA20_POLY1305);
			var ivParameterSpec = new IvParameterSpec(nonce);
			var keySpec = new SecretKeySpec(key.getEncoded(), ENCRYPTION_ALGORITHM_CHACHA20);
			cipher.init(ENCRYPT_MODE, keySpec, ivParameterSpec);
			cipher.updateAAD(additionalAuthenticatedData);
			return cipher.doFinal(plainText); // size of plainText + 16 bytes of poly tag data appended
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Decrypts using ChaCha20 as an AEAD cipher with Poly1305 as the authenticator.
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7539">RFC 7539</a>
	 * @param key                         the secret key
	 * @param nonce                       the unique, securely generated nonce that was used for the encryption
	 * @param cipherText                  the encrypted data
	 * @param additionalAuthenticatedData additional authenticated data. Can be used to authenticate the nonce
	 * @return the decrypted data
	 */
	public static byte[] decryptChaCha20Poly1305(SecretKey key, byte[] nonce, byte[] cipherText, byte[] additionalAuthenticatedData)
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
			var cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION_CHACHA20_POLY1305);
			AlgorithmParameterSpec ivParameterSpec = new IvParameterSpec(nonce);
			var keySpec = new SecretKeySpec(key.getEncoded(), ENCRYPTION_ALGORITHM_CHACHA20);
			cipher.init(DECRYPT_MODE, keySpec, ivParameterSpec);
			cipher.updateAAD(additionalAuthenticatedData);
			return cipher.doFinal(cipherText);
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Encrypts using ChaCha20 as an AEAD cipher with HMAC SHA-256.
	 *
	 * @param key                         the secret key
	 * @param nonce                       a unique, securely generated nonce
	 * @param plainText                   the data to encrypt
	 * @param additionalAuthenticatedData additional authenticated data. Can be used to authenticate the nonce
	 * @return the encrypted data
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7539">RFC 7539</a>
	 */
	public static byte[] encryptChaCha20Sha256(SecretKey key, byte[] nonce, byte[] plainText, byte[] additionalAuthenticatedData)
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
			var tag = new byte[TAG_SIZE];
			var cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION_CHACHA20);
			var chaCha20ParameterSpec = new ChaCha20ParameterSpec(nonce, 1);
			var keySpec = new SecretKeySpec(key.getEncoded(), ENCRYPTION_ALGORITHM_CHACHA20);
			cipher.init(ENCRYPT_MODE, keySpec, chaCha20ParameterSpec);
			var encryptedData = cipher.doFinal(plainText);

			var hmac = new Sha256HMac(key);
			hmac.update(additionalAuthenticatedData);
			hmac.update(encryptedData);
			System.arraycopy(hmac.getBytes(), 0, tag, 0, TAG_SIZE);

			return ByteBuffer.allocate(encryptedData.length + TAG_SIZE)
					.put(encryptedData)
					.put(tag)
					.array();
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Decrypts using ChaCha20 as an AEAD cipher with HMAC SHA-256.
	 *
	 * @param key                         the secret key
	 * @param nonce                       the unique, securely generated nonce that was used for the encryption
	 * @param cipherText                  the encrypted data
	 * @param additionalAuthenticatedData additional authenticated data. Can be used to authenticate the nonce
	 * @return the decrypted data
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7539">RFC 7539</a>
	 */
	public static byte[] decryptChaCha20Sha256(SecretKey key, byte[] nonce, byte[] cipherText, byte[] additionalAuthenticatedData)
	{
		Objects.requireNonNull(key);
		Objects.requireNonNull(nonce);
		if (nonce.length != 12)
		{
			throw new IllegalArgumentException("Nonce must be 12 bytes");
		}
		Objects.requireNonNull(cipherText);
		Objects.requireNonNull(additionalAuthenticatedData);

		var encryptedData = new byte[cipherText.length - TAG_SIZE];
		var tag = new byte[TAG_SIZE];
		var resultingTag = new byte[TAG_SIZE];

		var buf = ByteBuffer.wrap(cipherText);
		buf.get(encryptedData);
		buf.get(tag);

		try
		{
			var cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION_CHACHA20);
			var chaCha20ParameterSpecs = new ChaCha20ParameterSpec(nonce, 1);
			var keySpec = new SecretKeySpec(key.getEncoded(), ENCRYPTION_ALGORITHM_CHACHA20);
			cipher.init(DECRYPT_MODE, keySpec, chaCha20ParameterSpecs);
			var decryptedData = cipher.doFinal(encryptedData);

			// Verify the SHA256 tag, performed after the decryption to avoid timing attacks.
			var hmac = new Sha256HMac(key);
			hmac.update(additionalAuthenticatedData);
			hmac.update(encryptedData);
			System.arraycopy(hmac.getBytes(), 0, resultingTag, 0, TAG_SIZE);

			if (!MessageDigest.isEqual(tag, resultingTag))
			{
				throw new IllegalArgumentException("ChaCha20 SHA-256: Authentication failed");
			}
			return decryptedData;
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e)
		{
			throw new IllegalArgumentException(e);
		}
	}
}
