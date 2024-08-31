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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class RsCryptoTest
{
	private static SecretKey key;

	@BeforeAll
	static void setup()
	{
		key = AEAD.generateKey();
	}

	@Test
	void ChaCha20Sha256_Encrypt_Decrypt_Success()
	{
		var plainText = "hello, world".getBytes(StandardCharsets.UTF_8);

		var cipherText = RsCrypto.encryptAuthenticateData(key, plainText, RsCrypto.EncryptionFormat.CHACHA20_SHA256);
		var decryptedText = RsCrypto.decryptAuthenticateData(key, cipherText);

		assertArrayEquals(plainText, decryptedText);
	}

	@Test
	void ChaCha20Poly1305_Encrypt_Decrypt_Success()
	{
		var plainText = "bye, cruel world".getBytes(StandardCharsets.UTF_8);

		var cipherText = RsCrypto.encryptAuthenticateData(key, plainText, RsCrypto.EncryptionFormat.CHACHA20_POLY1305);
		var decryptedText = RsCrypto.decryptAuthenticateData(key, cipherText);

		assertArrayEquals(plainText, decryptedText);
	}
}
