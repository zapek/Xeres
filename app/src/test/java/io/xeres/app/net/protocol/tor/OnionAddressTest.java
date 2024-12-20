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

package io.xeres.app.net.protocol.tor;

import io.xeres.common.protocol.tor.OnionAddress;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static io.xeres.common.protocol.tor.OnionAddress.isValidAddress;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnionAddressTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(OnionAddress.class);
	}

	@Test
	void IsValidAddress_Success()
	{
		assertTrue(isValidAddress("answerszuvs3gg2l64e6hmnryudl5zgrmwm3vh65hzszdghblddvfiqd.onion:1234"));
	}

	@Test
	void IsValidAddress_Failure()
	{
		assertFalse(isValidAddress("3g2upl4pq6kufc4m.onion:1234"));
	}
}
