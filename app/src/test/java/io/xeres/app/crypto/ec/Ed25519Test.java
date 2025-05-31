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

package io.xeres.app.crypto.ec;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Ed25519Test
{
	private static KeyPair keyPair;

	@BeforeAll
	static void setup()
	{
		keyPair = Ed25519.generateKeys(255);
	}

	@Test
	void utilityClassCheck() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(Ed25519.class);
	}

	@Test
	void Generation_Success()
	{
		assertNotNull(keyPair);
		assertEquals("EdDSA", keyPair.getPrivate().getAlgorithm());
		assertEquals("EdDSA", keyPair.getPublic().getAlgorithm());
	}
}