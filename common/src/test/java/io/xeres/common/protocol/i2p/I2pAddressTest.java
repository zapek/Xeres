/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.common.protocol.i2p;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I2pAddressTest
{
	// 52 base32 chars (a-z, 2-7)
	private static final String VALID_HOST_52 = "abcdefghijklmnopqrst234567abcdefghijklmnopqrstuvwxyz";

	@Test
	void Instance_Throws() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(I2pAddress.class);
	}

	@Test
	void IsValidAddress_ValidB32Address_Success()
	{
		assertTrue(I2pAddress.isValidAddress(VALID_HOST_52 + ".b32.i2p:10"));
	}

	@Test
	void IsValidAddress_ValidB32AddressMinPort_Success()
	{
		assertTrue(I2pAddress.isValidAddress(VALID_HOST_52 + ".b32.i2p:1"));
	}

	@Test
	void IsValidAddress_ValidB32AddressMaxPort_Success()
	{
		assertTrue(I2pAddress.isValidAddress(VALID_HOST_52 + ".b32.i2p:65535"));
	}

	@Test
	void IsValidAddress_InvalidCharacters_False()
	{
		assertFalse(I2pAddress.isValidAddress("abcdefghij1234567890abcdefghij1234567890abcdefghij1234.b32.i2p:10"));
	}

	@Test
	void IsValidAddress_TooShort_False()
	{
		assertFalse(I2pAddress.isValidAddress("abcdefghijklmnop.b32.i2p:10"));
	}

	@Test
	void IsValidAddress_NoPort_False()
	{
		assertFalse(I2pAddress.isValidAddress(VALID_HOST_52 + ".b32.i2p"));
	}

	@Test
	void IsValidAddress_CorrectDomainButBadHostLength_False()
	{
		assertFalse(I2pAddress.isValidAddress("abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopq.b32.i2p:10"));
	}

	@Test
	void IsValidAddress_WrongDomain_False()
	{
		assertFalse(I2pAddress.isValidAddress(VALID_HOST_52 + ".i2p:10"));
	}
}
