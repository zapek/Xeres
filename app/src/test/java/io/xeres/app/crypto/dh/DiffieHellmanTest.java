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

package io.xeres.app.crypto.dh;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.interfaces.DHPublicKey;
import java.math.BigInteger;
import java.security.KeyPair;

import static io.xeres.app.crypto.dh.DiffieHellman.G;
import static io.xeres.app.crypto.dh.DiffieHellman.P;
import static org.junit.jupiter.api.Assertions.*;

class DiffieHellmanTest
{
	private static KeyPair keyPair;

	@BeforeAll
	static void setup()
	{
		keyPair = io.xeres.app.crypto.dh.DiffieHellman.generateKeys();
	}

	@Test
	void utilityClassCheck() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(DiffieHellman.class);
	}

	@Test
	void DiffieHellman_Validate()
	{
		assertTrue(isSafePrime(P));
		assertTrue(isGeneratorValid(G));
	}

	@Test
	void DiffieHellman_Generation_Success()
	{
		assertNotNull(keyPair);
		assertEquals("DH", keyPair.getPrivate().getAlgorithm());
		assertEquals("DH", keyPair.getPublic().getAlgorithm());
	}

	@Test
	void DiffieHellman_GetPublicKey()
	{
		var publicKeyNum = ((DHPublicKey) keyPair.getPublic()).getY();

		assertEquals(((DHPublicKey) keyPair.getPublic()).getY(), ((DHPublicKey) DiffieHellman.getPublicKey(publicKeyNum)).getY());
	}

	@Test
	void DiffieHellman_GenerateCommonSecret()
	{
		var receivedKeyPair = DiffieHellman.generateKeys();

		var common = DiffieHellman.generateCommonSecretKey(keyPair.getPrivate(), receivedKeyPair.getPublic());

		assertNotNull(common);
	}

	@Test
	void DiffieHellman_FullExchange()
	{
		var heike = DiffieHellman.generateKeys();
		var juergen = DiffieHellman.generateKeys();

		var heikeSecret = DiffieHellman.generateCommonSecretKey(heike.getPrivate(), juergen.getPublic());
		var juergenSecret = DiffieHellman.generateCommonSecretKey(juergen.getPrivate(), heike.getPublic());

		assertArrayEquals(heikeSecret, juergenSecret);
	}

	private static boolean isSafePrime(BigInteger p)
	{
		// Check if p is a safe prime (p = 2q + 1, where q is also prime)
		BigInteger q = p.subtract(BigInteger.ONE).divide(BigInteger.TWO);
		return p.isProbablePrime(10) && q.isProbablePrime(10);
	}

	private static boolean isGeneratorValid(BigInteger g)
	{
		// Usually 2 or 5.
		return g.equals(new BigInteger("2")) || g.equals(new BigInteger("5"));
	}
}