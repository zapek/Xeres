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

import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.testutils.TestUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AESTest
{
	private static final byte[] aesKey = new byte[16];
	private static byte[] iv;

	@BeforeAll
	static void setup()
	{
		Sha1MessageDigest digest = new Sha1MessageDigest();
		digest.update(RandomUtils.insecure().randomBytes(16));
		System.arraycopy(digest.getBytes(), 0, aesKey, 0, aesKey.length);

		iv = RandomUtils.insecure().randomBytes(8);
	}

	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(AES.class);
	}

	@Test
	void Encrypt_AES_Success()
	{
		var plainText = "Hello cruel world".getBytes(StandardCharsets.UTF_8);

		var cipherText = AES.encrypt(aesKey, iv, plainText);
		var decryptedText = AES.decrypt(aesKey, iv, cipherText);

		assertArrayEquals(plainText, decryptedText);
	}

	@Test
	void Encrypt_AES_BadKey()
	{
		var plainText = "Hello cruel world".getBytes(StandardCharsets.UTF_8);

		assertThrows(IllegalArgumentException.class, () -> AES.encrypt(new byte[8], iv, plainText));
	}

	@Test
	void Encrypt_AES_BadIv()
	{
		var plainText = "Hello cruel world".getBytes(StandardCharsets.UTF_8);

		assertThrows(IllegalArgumentException.class, () -> AES.encrypt(aesKey, new byte[4], plainText));
	}
}