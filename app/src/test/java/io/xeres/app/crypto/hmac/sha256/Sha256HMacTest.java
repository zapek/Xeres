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

package io.xeres.app.crypto.hmac.sha256;

import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Sha256HMacTest
{
	@Test
	void RFC2202_Test_Case1()
	{
		var hexFormat = HexFormat.of();
		var keySpec = new SecretKeySpec(hexFormat.parseHex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"), "AES");
		var hmac = new Sha256HMac(keySpec);

		assertNotNull(hmac);

		hmac.update("Hi There".getBytes());

		assertArrayEquals(hexFormat.parseHex("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7"), hmac.getBytes());
	}

	@Test
	void RFC2202_Test_Case2()
	{
		var hexFormat = HexFormat.of();
		var keySpec = new SecretKeySpec(hexFormat.parseHex("4a656665"), "AES");
		var hmac = new Sha256HMac(keySpec);

		assertNotNull(hmac);

		hmac.update("what do ya want for nothing?".getBytes());

		assertArrayEquals(hexFormat.parseHex("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843"), hmac.getBytes());
	}
}