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

package io.xeres.app.crypto.aes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * AES 256 CBC encryption.
 */
public final class AES
{
	private static final Logger log = LoggerFactory.getLogger(AES.class);

	private static final String ALGORITHM_AES = "AES/CBC/PKCS5Padding";
	private static final int ROUNDS = 5;
	private static final int KEY_SIZE = 256; // in bits
	private static final int INDEX_KEY = 0;
	private static final int INDEX_IV = 1;
	private static final int IV_SIZE = 8; // in bytes

	private AES()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Encrypts using AES with a 16 bytes key and an 8 bytes salt.
	 *
	 * @param key       the 16 bytes key
	 * @param iv        an 8 bytes initialization vector
	 * @param plainText the plain text
	 * @return the encoded text
	 */
	public static byte[] encrypt(byte[] key, byte[] iv, byte[] plainText)
	{
		return process(Cipher.ENCRYPT_MODE, key, iv, plainText);
	}

	/**
	 * Decrypts using AES with a 16 bytes key and an 8 bytes salt.
	 *
	 * @param key           the 16 bytes key
	 * @param iv            an 8 bytes initialization vector
	 * @param encryptedText the encrypted text
	 * @return the plain text
	 */
	public static byte[] decrypt(byte[] key, byte[] iv, byte[] encryptedText)
	{
		return process(Cipher.DECRYPT_MODE, key, iv, encryptedText);
	}

	private static byte[] process(int opMode, byte[] key, byte[] iv, byte[] data)
	{
		if (key == null || key.length != 16)
		{
			throw new IllegalArgumentException("Invalid key");
		}

		if (iv == null || iv.length != IV_SIZE)
		{
			throw new IllegalArgumentException("Invalid salt");
		}

		try
		{
			var cipher = Cipher.getInstance(ALGORITHM_AES);
			var md = MessageDigest.getInstance("SHA-1");

			byte[][] keyAndIv = EVP_BytesToKey(KEY_SIZE / Byte.SIZE, cipher.getBlockSize(), md, iv, key, ROUNDS);

			if (keyAndIv[INDEX_KEY].length != KEY_SIZE / Byte.SIZE)
			{
				throw new IllegalArgumentException("Key size is " + keyAndIv[INDEX_KEY].length + " bits, should be " + KEY_SIZE);
			}

			var secretKeySpecs = new SecretKeySpec(keyAndIv[INDEX_KEY], "AES");
			var ivParameterSpecs = new IvParameterSpec(keyAndIv[INDEX_IV]);

			cipher.init(opMode, secretKeySpecs, ivParameterSpecs);
			return cipher.doFinal(data);
		}
		catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | BadPaddingException | InvalidKeyException e)
		{
			throw new IllegalArgumentException(e);
		}
	}


	/**
	 * OpenSSL equivalent, by Ola Bini, public domain. The source
	 * is <a href="http://olabini.com/blog/tag/evp_bytestokey/">here</a>.
	 */
	private static byte[][] EVP_BytesToKey(int keyLength, int ivLength, MessageDigest md, byte[] salt, byte[] data, int count)
	{
		var both = new byte[2][];
		var key = new byte[keyLength];
		var keyIx = 0;
		var iv = new byte[ivLength];
		var ivIx = 0;
		both[0] = key;
		both[1] = iv;
		byte[] mdBuf = null;
		int nKey = keyLength;
		int nIv = ivLength;
		var i = 0;
		var addMd = 0;
		do
		{
			md.reset();
			if (addMd++ > 0)
			{
				md.update(mdBuf);
			}
			md.update(data);
			if (null != salt)
			{
				md.update(salt, 0, 8);
			}
			mdBuf = md.digest();
			for (i = 1; i < count; i++)
			{
				md.reset();
				md.update(mdBuf);
				mdBuf = md.digest();
			}
			i = 0;
			if (nKey > 0)
			{
				for (; ; )
				{
					if (nKey == 0)
					{
						break;
					}
					if (i == mdBuf.length)
					{
						break;
					}
					key[keyIx++] = mdBuf[i];
					nKey--;
					i++;
				}
			}
			if (nIv > 0 && i != mdBuf.length)
			{
				for (; ; )
				{
					if (nIv == 0)
					{
						break;
					}
					if (i == mdBuf.length)
					{
						break;
					}
					iv[ivIx++] = mdBuf[i];
					nIv--;
					i++;
				}
			}
		} while (nKey != 0 || nIv != 0);

		for (i = 0; i < mdBuf.length; i++)
		{
			mdBuf[i] = 0;
		}
		return both;
	}
}
