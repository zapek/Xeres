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

package io.xeres.app.net.util;

import org.junit.jupiter.api.Test;

import static io.xeres.app.net.util.NetworkMode.*;
import static org.junit.jupiter.api.Assertions.*;

class NetworkModeTest
{
	@Test
	void NetworkMode_Enum_Order()
	{
		assertEquals(0, PUBLIC.ordinal());
		assertEquals(1, PRIVATE.ordinal());
		assertEquals(2, INVERTED.ordinal());
		assertEquals(3, DARKNET.ordinal());

		assertEquals(4, values().length);
	}

	@Test
	void NetworkMode_IsDiscoverable()
	{
		assertTrue(isDiscoverable(PUBLIC));
		assertTrue(isDiscoverable(PRIVATE));
		assertFalse(isDiscoverable(INVERTED));
		assertFalse(isDiscoverable(DARKNET));
	}

	@Test
	void NetworkMode_HasDht()
	{
		assertTrue(hasDht(PUBLIC));
		assertTrue(hasDht(INVERTED));
		assertFalse(hasDht(PRIVATE));
		assertFalse(hasDht(DARKNET));
	}

	@Test
	void NetworkMode_GetNetworkMode()
	{
		assertEquals(PUBLIC, getNetworkMode(2, 2));
		assertEquals(PRIVATE, getNetworkMode(2, 0));
		assertEquals(INVERTED, getNetworkMode(0, 2));
		assertEquals(DARKNET, getNetworkMode(0, 0));
	}
}
