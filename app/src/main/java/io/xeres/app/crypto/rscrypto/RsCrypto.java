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

package io.xeres.app.crypto.rscrypto;

import io.xeres.app.crypto.aead.AEAD;
import io.xeres.common.util.SecureRandomUtils;

import javax.crypto.SecretKey;
import java.nio.ByteBuffer;

/**
 * This class implements the custom RS encryption, notably to encrypt file transfer tunnels.
 *  <p>
 *  <img src="doc-files/format.png" alt="Format diagram">
 */
public final class RsCrypto
{
	public enum EncryptionFormat
	{
		CHACHA20_POLY1305(1),
		CHACHA20_SHA256(2);

		private final int value;

		EncryptionFormat(int value)
		{
			this.value = value;
		}

		public int getValue()
		{
			return value;
		}
	}

	private static final int INITIALIZATION_VECTOR_SIZE = 12;
	private static final int AUTHENTICATION_TAG_SIZE = 16;
	private static final int HEADER_SIZE = 4;
	private static final int EDATA_SIZE = 4;

	private RsCrypto()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static byte[] encryptAuthenticateData(SecretKey key, byte[] plainText, EncryptionFormat format)
	{
		// Initialization vector (AAD)
		var initializationVector = new byte[INITIALIZATION_VECTOR_SIZE];
		SecureRandomUtils.nextBytes(initializationVector);

		var aad = new byte[INITIALIZATION_VECTOR_SIZE + EDATA_SIZE];
		System.arraycopy(initializationVector, 0, aad, 0, INITIALIZATION_VECTOR_SIZE);

		aad[INITIALIZATION_VECTOR_SIZE] = (byte) ((plainText.length) & 0xff);
		aad[INITIALIZATION_VECTOR_SIZE + 1] = (byte) ((plainText.length >> 8) & 0xff);
		aad[INITIALIZATION_VECTOR_SIZE + 2] = (byte) ((plainText.length >> 16) & 0xff);
		aad[INITIALIZATION_VECTOR_SIZE + 3] = (byte) ((plainText.length >> 24) & 0xff);

		var totalSize = HEADER_SIZE + INITIALIZATION_VECTOR_SIZE + EDATA_SIZE + plainText.length + AUTHENTICATION_TAG_SIZE;
		var encryptedData = new byte[totalSize];
		var offset = 0;

		// Header
		encryptedData[0] = (byte) 0xae;
		encryptedData[1] = (byte) 0xad;
		encryptedData[2] = (byte) format.getValue();
		encryptedData[3] = (byte) 0x1;

		offset += HEADER_SIZE;

		// Copy AAD data (initialization vector + length)
		System.arraycopy(aad, 0, encryptedData, offset, aad.length);
		offset += aad.length;

		byte[] cipherText;

		if (encryptedData[2] == EncryptionFormat.CHACHA20_POLY1305.getValue())
		{
			cipherText = AEAD.encryptChaCha20Poly1305(key, initializationVector, plainText, aad);
		}
		else if (encryptedData[2] == EncryptionFormat.CHACHA20_SHA256.getValue())
		{
			cipherText = AEAD.encryptChaCha20Sha256(key, initializationVector, plainText, aad);
		}
		else
		{
			throw new IllegalArgumentException("Unsupported encrypted data type: " + encryptedData[2]);
		}

		System.arraycopy(cipherText, 0, encryptedData, offset, cipherText.length);

		return encryptedData;
	}

	public static byte[] decryptAuthenticateData(SecretKey key, byte[] cipherText)
	{
		if (cipherText.length < HEADER_SIZE + INITIALIZATION_VECTOR_SIZE + EDATA_SIZE)
		{
			throw new IllegalArgumentException("Ciphertext is too short");
		}

		var buf = ByteBuffer.wrap(cipherText);
		var magic1 = buf.get();
		var magic2 = buf.get();
		var format = buf.get();
		var magic3 = buf.get();

		if (magic1 != (byte) 0xae && magic2 != (byte) 0xad && magic3 != (byte) 0x1)
		{
			throw new IllegalArgumentException("Invalid ciphertext header");
		}
		if (format != EncryptionFormat.CHACHA20_POLY1305.getValue() && format != EncryptionFormat.CHACHA20_SHA256.getValue())
		{
			throw new IllegalArgumentException("Unsupported encrypted data type: " + cipherText[2]);
		}

		var initializationVector = new byte[INITIALIZATION_VECTOR_SIZE];
		buf.get(initializationVector);

		var aad = new byte[INITIALIZATION_VECTOR_SIZE + EDATA_SIZE];
		var eDataArray = new byte[EDATA_SIZE];
		buf.get(eDataArray);
		System.arraycopy(initializationVector, 0, aad, 0, INITIALIZATION_VECTOR_SIZE);
		System.arraycopy(eDataArray, 0, aad, INITIALIZATION_VECTOR_SIZE, EDATA_SIZE);

		int eDataSize = Byte.toUnsignedInt(eDataArray[0]);
		eDataSize += Byte.toUnsignedInt(eDataArray[1]) << 8;
		eDataSize += Byte.toUnsignedInt(eDataArray[2]) << 16;
		eDataSize += Byte.toUnsignedInt(eDataArray[3]) << 24;

		var expectedSize = eDataSize + HEADER_SIZE + INITIALIZATION_VECTOR_SIZE + EDATA_SIZE + AUTHENTICATION_TAG_SIZE;

		if (expectedSize != cipherText.length)
		{
			throw new IllegalArgumentException("Encrypted data size is wrong, expected: " + expectedSize + ", got: " + cipherText.length);
		}

		byte[] decryptedText;
		var encryptedText = new byte[eDataSize + AUTHENTICATION_TAG_SIZE];
		buf.get(encryptedText);

		if (format == EncryptionFormat.CHACHA20_POLY1305.getValue())
		{
			decryptedText = AEAD.decryptChaCha20Poly1305(key, initializationVector, encryptedText, aad);
		}
		else
		{
			decryptedText = AEAD.decryptChaCha20Sha256(key, initializationVector, encryptedText, aad);
		}

		var decryptedData = new byte[eDataSize];
		System.arraycopy(decryptedText, 0, decryptedData, 0, eDataSize);

		return decryptedData;
	}
}
