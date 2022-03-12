/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
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
	void RSA_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(RSA.class);
	}

	/**
	 * Generates an RSA secret key.
	 */
	@Test
	void RSA_GenerateKeys_OK()
	{
		assertNotNull(keyPair);
		assertEquals("RSA", keyPair.getPrivate().getAlgorithm());
		assertEquals("RSA", keyPair.getPublic().getAlgorithm());
	}

	@Test
	void RSA_GetPrivateKey_OK() throws InvalidKeySpecException, NoSuchAlgorithmException
	{
		assertEquals(keyPair.getPrivate(), RSA.getPrivateKey(keyPair.getPrivate().getEncoded()));
	}

	@Test
	void RSA_GetPublicKey_OK() throws InvalidKeySpecException, NoSuchAlgorithmException
	{
		assertEquals(keyPair.getPublic(), RSA.getPublicKey(keyPair.getPublic().getEncoded()));
	}

	@Test
	void RSA_Sign_OK()
	{
		byte[] data = {1, 2, 3};

		var signature = RSA.sign(data, keyPair.getPrivate());

		assertNotNull(signature);

		var result = RSA.verify(keyPair.getPublic(), signature, data);

		assertTrue(result);
	}

	@Test
	void RSA_Sign_TemperedData()
	{
		byte[] data = {1, 2, 3};

		var signature = RSA.sign(data, keyPair.getPrivate());

		assertNotNull(signature);

		data[0] = 0;

		var result = RSA.verify(keyPair.getPublic(), signature, data);

		assertFalse(result);
	}

	@Test
	void RSA_Convert_Private_Pkcs8_To_Pkcs1_And_Back() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
		var pkcs1 = RSA.getPrivateKeyAsPkcs1(keyPair.getPrivate());
		var privateKey = RSA.getPrivateKeyFromPkcs1(pkcs1);

		assertArrayEquals(keyPair.getPrivate().getEncoded(), privateKey.getEncoded());
	}

	@Test
	void RSA_Convert_Public_X509_To_Pkcs1_And_Back() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
		var pkcs1 = RSA.getPublicKeyAsPkcs1(keyPair.getPublic());
		var publicKey = RSA.getPublicKeyFromPkcs1(pkcs1);

		assertArrayEquals(keyPair.getPublic().getEncoded(), publicKey.getEncoded());
	}
}
