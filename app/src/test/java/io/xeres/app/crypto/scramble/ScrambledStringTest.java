/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.crypto.scramble;

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
		String TEST = "1234";
		String HASH = "A6xnQhbz4Vx2HuGl4lXwZ5U2I8iziLRFnhP5eNfIRvQ=";
		var ss = new ScrambledString(TEST.toCharArray());

		assertEquals("[SCRAMBLED]", ss.toString());
		assertEquals(HASH, ss.getBase64SHA256Hash());
		assertTrue(ss.verifyBase64SHA256Hash(HASH));
		ss.access(clearChars -> assertArrayEquals(TEST.toCharArray(), clearChars));
	}

	@Test
	void ScrambledString_Append_OK()
	{
		String TEST = "1234";
		String HASH = "A6xnQhbz4Vx2HuGl4lXwZ5U2I8iziLRFnhP5eNfIRvQ=";

		var ss = new ScrambledString();

		for (char c : TEST.toCharArray())
		{
			ss.appendChar(c);
		}
		assertTrue(ss.verifyBase64SHA256Hash(HASH));
	}

	@Test
	void ScrambledString_Dispose_OK()
	{
		String TEST = "1234";

		var ss = new ScrambledString(TEST.toCharArray());

		ss.dispose();

		assertThrows(IllegalStateException.class, () -> ss.access(System.out::print));
		assertThrows(IllegalStateException.class, () -> ss.appendChar('a'));
		assertThrows(IllegalStateException.class, () -> ss.verifyBase64SHA256Hash("a"));
		assertThrows(IllegalStateException.class, ss::getBase64SHA256Hash);
		assertEquals("", ss.toString());
	}

	@Test
	void ScrambledString_Equality_OK()
	{
		String TEST = "1234";

		var ss1 = new ScrambledString(TEST.toCharArray());
		var ss2 = new ScrambledString(TEST.toCharArray());

		assertEquals(ss1, ss2);
	}

	@Test
	void ScrambledString_Equality_Fail()
	{
		String TEST1 = "1234";
		String TEST2 = "5678";

		var ss1 = new ScrambledString(TEST1.toCharArray());
		var ss2 = new ScrambledString(TEST2.toCharArray());

		assertNotEquals(ss1, ss2);
	}
}
