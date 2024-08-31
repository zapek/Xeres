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

package io.xeres.app.crypto.rsa;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serial;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.*;

class RSATest
{
	private static final int KEY_SIZE = 512;

	private static KeyPair keyPair;

	@BeforeAll
	static void setup()
	{
		keyPair = RSA.generateKeys(KEY_SIZE);
	}

	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(RSA.class);
	}

	/**
	 * Generates an RSA secret key.
	 */
	@Test
	void GenerateKeys_Success()
	{
		assertNotNull(keyPair);
		assertEquals("RSA", keyPair.getPrivate().getAlgorithm());
		assertEquals("RSA", keyPair.getPublic().getAlgorithm());
	}

	@Test
	void GetPrivateKey_Success() throws InvalidKeySpecException, NoSuchAlgorithmException
	{
		assertEquals(keyPair.getPrivate(), RSA.getPrivateKey(keyPair.getPrivate().getEncoded()));
	}

	@Test
	void GetPublicKey_Success() throws InvalidKeySpecException, NoSuchAlgorithmException
	{
		assertEquals(keyPair.getPublic(), RSA.getPublicKey(keyPair.getPublic().getEncoded()));
	}

	@Test
	void Sign_Success()
	{
		byte[] data = {1, 2, 3};

		var signature = RSA.sign(data, keyPair.getPrivate());

		assertNotNull(signature);

		var result = RSA.verify(keyPair.getPublic(), signature, data);

		assertTrue(result);
	}

	@Test
	void Sign_TemperedData_Failure()
	{
		byte[] data = {1, 2, 3};

		var signature = RSA.sign(data, keyPair.getPrivate());

		assertNotNull(signature);

		data[0] = 0;

		var result = RSA.verify(keyPair.getPublic(), signature, data);

		assertFalse(result);
	}

	@Test
	void Sign_InvalidKey_ThrowsException()
	{
		byte[] data = {1, 2, 3};
		var privateKey = new PrivateKey()
		{
			@Serial
			private static final long serialVersionUID = -5166467762224595264L;

			@Override
			public String getAlgorithm()
			{
				return "RSA";
			}

			@Override
			public String getFormat()
			{
				return "PKCS#8";
			}

			@Override
			public byte[] getEncoded()
			{
				return new byte[0]; // Invalid key
			}
		};

		assertThrows(IllegalArgumentException.class, () -> RSA.sign(data, privateKey));
	}

	@Test
	void Convert_Private_Pkcs8_To_Pkcs1_And_Back_Success() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
		var pkcs1 = RSA.getPrivateKeyAsPkcs1(keyPair.getPrivate());
		var privateKey = RSA.getPrivateKeyFromPkcs1(pkcs1);

		assertArrayEquals(keyPair.getPrivate().getEncoded(), privateKey.getEncoded());
	}

	@Test
	void Convert_Public_X509_To_Pkcs1_And_Back_Success() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
		var pkcs1 = RSA.getPublicKeyAsPkcs1(keyPair.getPublic());
		var publicKey = RSA.getPublicKeyFromPkcs1(pkcs1);

		assertArrayEquals(keyPair.getPublic().getEncoded(), publicKey.getEncoded());
	}
}
