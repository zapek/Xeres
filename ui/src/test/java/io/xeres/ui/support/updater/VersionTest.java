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

package io.xeres.ui.support.updater;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class VersionTest
{
	@ParameterizedTest
	@CsvSource({
			"1, 0, 0, 2, 0, 0, -1",
			"2, 0, 0, 1, 0, 0, 1",
			"1, 2, 0, 1, 3, 0, -1",
			"1, 3, 0, 1, 2, 0, 1",
			"1, 2, 3, 1, 2, 4, -1",
			"1, 2, 4, 1, 2, 3, 1",
			"1, 2, 3, 1, 2, 3, 0"
	})
	void CompareTo_VariousCombinations(int major1, int minor1, int patch1, int major2, int minor2, int patch2, int expected)
	{
		var v1 = new Version(major1, minor1, patch1);
		var v2 = new Version(major2, minor2, patch2);

		assertEquals(expected, Integer.signum(v1.compareTo(v2)));
	}

	@Test
	void IsNotARelease_AllZero_True()
	{
		assertTrue(new Version(0, 0, 0).isNotARelease());
	}

	@Test
	void IsNotARelease_NonZero_False()
	{
		assertFalse(new Version(1, 0, 0).isNotARelease());
		assertFalse(new Version(0, 1, 0).isNotARelease());
		assertFalse(new Version(0, 0, 1).isNotARelease());
		assertFalse(new Version(1, 2, 3).isNotARelease());
	}

	@Test
	void ToString_FormatsCorrectly()
	{
		assertEquals("1.2.3", new Version(1, 2, 3).toString());
		assertEquals("0.0.0", new Version(0, 0, 0).toString());
		assertEquals("10.20.30", new Version(10, 20, 30).toString());
	}

	@Test
	void Equals_SameValues_True()
	{
		assertEquals(new Version(1, 2, 3), new Version(1, 2, 3));
	}

	@Test
	void Equals_DifferentValues_False()
	{
		assertNotEquals(new Version(1, 2, 3), new Version(1, 2, 4));
	}

	@Test
	void HashCode_SameValues_Equal()
	{
		assertEquals(new Version(1, 2, 3).hashCode(), new Version(1, 2, 3).hashCode());
	}
}
