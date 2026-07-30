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

package io.xeres.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScrambledStringTest
{
	@Test
	void ScrambledString_Constructor_Empty_OK()
	{
		var ss = new ScrambledString();

		assertEquals("[SCRAMBLED]", ss.toString());
	}

	@Test
	void ScrambledString_Constructor_OK()
	{
		var TEST = "1234";
		var ss = new ScrambledString(TEST.toCharArray());

		assertEquals("[SCRAMBLED]", ss.toString());
	}

	@Test
	void ScrambledString_Dispose_OK()
	{
		var TEST = "1234";

		var ss = new ScrambledString(TEST.toCharArray());

		ss.dispose();

		assertThrows(IllegalStateException.class, ss::getAsInsecureString);
		assertThrows(IllegalStateException.class, ss::getAsCharArrayToClear);
		assertEquals("", ss.toString());
	}

	@Test
	void ScrambledString_AsInsecureString()
	{
		var TEST = "1234";

		var ss = new ScrambledString(TEST);

		assertEquals("1234", ss.getAsInsecureString());
	}

	@Test
	void ScrambledString_AsClear()
	{
		var TEST = "1234".toCharArray();

		var ss = new ScrambledString(TEST);

		assertArrayEquals("1234".toCharArray(), ss.getAsCharArrayToClear());
	}

	@Test
	void ScrambledString_Accents()
	{
		var TEST = "éèà😊".toCharArray();

		var ss = new ScrambledString(TEST);

		assertArrayEquals("éèà😊".toCharArray(), ss.getAsCharArrayToClear());
	}
}