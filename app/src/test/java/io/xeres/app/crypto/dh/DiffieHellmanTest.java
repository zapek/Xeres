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

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static io.xeres.app.crypto.dh.DiffieHellman.G;
import static io.xeres.app.crypto.dh.DiffieHellman.P;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiffieHellmanTest
{
	@Test
	void DiffieHellman_Validate()
	{
		assertTrue(isSafePrime(P));
		assertTrue(isGeneratorValid(G));
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