/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.app.net.protocol.i2p;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static io.xeres.app.net.protocol.i2p.I2pAddress.isValidAddress;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I2pAddressTest
{
	@Test
	void I2pAddress_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(I2pAddress.class);
	}

	@Test
	void I2pAddress_isValidAddress_OK()
	{
		assertTrue(isValidAddress("g6u4vqiuy6bdc3dbu6a7gmi3ip45sqwgtbgrr6uupqaaqfyztrka.b32.i2p:1234"));
	}

	@Test
	void I2PAddress_IsValidAddress_Fail()
	{
		assertFalse(isValidAddress("foobar.b32.i2p:1234"));
	}
}
