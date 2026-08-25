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

package io.xeres.common.id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class Sha1SumTest
{
	@Test
	void Constructor_ValidBytes_Success()
	{
		var bytes = new byte[20];
		bytes[0] = (byte) 0xAB;

		var sha1 = new Sha1Sum(bytes);

		assertArrayEquals(bytes, sha1.getBytes());
	}

	@Test
	void Constructor_Null_ThrowsNullPointerException()
	{
		assertThrows(NullPointerException.class, () -> new Sha1Sum(null));
	}

	@Test
	void Constructor_WrongLength_ThrowsIllegalArgumentException()
	{
		assertThrows(IllegalArgumentException.class, () -> new Sha1Sum(new byte[17]));
		assertThrows(IllegalArgumentException.class, () -> new Sha1Sum(new byte[21]));
	}

	@Test
	void FromString_ValidHex_RoundTrips()
	{
		var hex = "ab12cd34ef56ab12cd34ef56ab12cd34ef56ab12";

		var sha1 = Sha1Sum.fromString(hex);

		assertEquals(hex, sha1.asString());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz", "ab12cd"})
	void FromString_InvalidInput_ReturnsNullIdentifier(String input)
	{
		var sha1 = Sha1Sum.fromString(input);

		assertTrue(sha1.isNullIdentifier());
	}

	@Test
	void Equals_SameBytes_True()
	{
		var bytes = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A,
				0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14};

		var sha1a = new Sha1Sum(bytes.clone());
		var sha1b = new Sha1Sum(bytes.clone());

		assertEquals(sha1a, sha1b);
		assertEquals(sha1a.hashCode(), sha1b.hashCode());
	}

	@Test
	void Equals_DifferentBytes_False()
	{
		var sha1a = new Sha1Sum(new byte[20]);
		var sha1b = new Sha1Sum(new byte[20]);
		sha1b.getBytes()[0] = (byte) 0xFF;

		assertNotEquals(sha1a, sha1b);
	}

	@Test
	void CompareTo_LessThan_Negative()
	{
		var bytesA = new byte[20];
		var bytesB = new byte[20];
		bytesA[0] = 0x01;
		bytesB[0] = 0x02;
		var sha1a = new Sha1Sum(bytesA);
		var sha1b = new Sha1Sum(bytesB);

		assertTrue(sha1a.compareTo(sha1b) < 0);
	}

	@Test
	void CompareTo_GreaterThan_Positive()
	{
		var bytesA = new byte[20];
		var bytesB = new byte[20];
		bytesA[0] = 0x02;
		bytesB[0] = 0x01;
		var sha1a = new Sha1Sum(bytesA);
		var sha1b = new Sha1Sum(bytesB);

		assertTrue(sha1a.compareTo(sha1b) > 0);
	}

	@Test
	void CompareTo_Equal_Zero()
	{
		var sha1a = new Sha1Sum(new byte[20]);
		var sha1b = new Sha1Sum(new byte[20]);

		assertEquals(0, sha1a.compareTo(sha1b));
	}

	@Test
	void Clone_DeepCopy()
	{
		var bytes = new byte[20];
		bytes[0] = 0x42;
		var sha1 = new Sha1Sum(bytes);

		var clone = sha1.clone();

		assertEquals(sha1, clone);
		assertNotSame(sha1, clone);
		clone.getBytes()[0] = (byte) 0xFF;
		assertNotEquals(sha1, clone);
	}

	@Test
	void IsNullIdentifier_AllZeros_True()
	{
		var sha1 = new Sha1Sum(new byte[20]);

		assertTrue(sha1.isNullIdentifier());
	}

	@Test
	void IsNullIdentifier_NonZero_False()
	{
		var sha1 = new Sha1Sum(new byte[20]);
		sha1.getBytes()[0] = 0x01;

		assertFalse(sha1.isNullIdentifier());
	}

	@Test
	void ToString_MatchesAsString()
	{
		var sha1 = Sha1Sum.fromString("ab12cd34ef56ab12cd34ef56ab12cd34ef56ab12");

		assertEquals(sha1.asString(), sha1.toString());
	}

	@Test
	void GetLength_Returns20()
	{
		assertEquals(20, new Sha1Sum(new byte[20]).getLength());
	}
}
