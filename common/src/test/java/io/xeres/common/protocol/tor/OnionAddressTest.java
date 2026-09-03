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

package io.xeres.common.protocol.tor;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnionAddressTest
{
	// 56 base32 chars (a-z, 2-7)
	private static final String VALID_HOST_56 = "abcdefghijklmnopqrstuvwx234567abcdefghijklmnopqrstuvwxyz";

	@Test
	void Instance_Throws() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(OnionAddress.class);
	}

	@Test
	void IsValidAddress_ValidOnionAddress_Success()
	{
		assertTrue(OnionAddress.isValidAddress(VALID_HOST_56 + ".onion:1234"));
	}

	@Test
	void IsValidAddress_ValidOnionAddressMinPort_Success()
	{
		assertTrue(OnionAddress.isValidAddress(VALID_HOST_56 + ".onion:1"));
	}

	@Test
	void IsValidAddress_ValidOnionAddressMaxPort_Success()
	{
		assertTrue(OnionAddress.isValidAddress(VALID_HOST_56 + ".onion:65535"));
	}

	@Test
	void IsValidAddress_InvalidCharacters_False()
	{
		assertFalse(OnionAddress.isValidAddress("abcdefghij1234567890abcdefghij1234567890abcdefghij1234567890ab.onion:1234"));
	}

	@Test
	void IsValidAddress_TooShort_False()
	{
		assertFalse(OnionAddress.isValidAddress("abcdefghijklmnopqrstuvwxy.onion:1234"));
	}

	@Test
	void IsValidAddress_NoPort_False()
	{
		assertFalse(OnionAddress.isValidAddress(VALID_HOST_56 + ".onion"));
	}

	@Test
	void IsValidAddress_Empty_False()
	{
		assertFalse(OnionAddress.isValidAddress(""));
	}
}
