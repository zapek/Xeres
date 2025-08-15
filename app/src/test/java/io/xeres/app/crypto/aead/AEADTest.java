/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

import io.xeres.testutils.TestUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AEADTest
{
	private static SecretKey key;

	@BeforeAll
	static void setup()
	{
		key = AEAD.generateKey();
	}

	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(AEAD.class);
	}

	@Test
	void EncryptChaCha20Poly1305_DecryptChaCha20Poly1305_Success()
	{
		var nonce = RandomUtils.insecure().randomBytes(12);
		var plainText = "hello world".getBytes(StandardCharsets.UTF_8);
		var aad = RandomUtils.insecure().randomBytes(16);

		var cipherText = AEAD.encryptChaCha20Poly1305(key, nonce, plainText, aad);
		var decryptedText = AEAD.decryptChaCha20Poly1305(key, nonce, cipherText, aad);

		assertArrayEquals(plainText, decryptedText);
	}

	@Test
	void EncryptChaCha20Poly1305_DecryptChaCha20Poly1305_BadNonce()
	{
		var nonce = RandomUtils.insecure().randomBytes(8);
		var plainText = "hello world".getBytes(StandardCharsets.UTF_8);
		var aad = RandomUtils.insecure().randomBytes(16);

		assertThrows(IllegalArgumentException.class, () -> AEAD.encryptChaCha20Poly1305(key, nonce, plainText, aad));
	}

	@Test
	void EncryptChaCha20Aes256_DecryptChaCha20Aes256_Success()
	{
		var nonce = RandomUtils.insecure().randomBytes(12);
		var plainText = "hello world".getBytes(StandardCharsets.UTF_8);
		var aad = RandomUtils.insecure().randomBytes(16);

		var cipherText = AEAD.encryptChaCha20Sha256(key, nonce, plainText, aad);
		var decryptedText = AEAD.decryptChaCha20Sha256(key, nonce, cipherText, aad);

		assertArrayEquals(plainText, decryptedText);
	}
}
