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

import static org.junit.jupiter.api.Assertions.*;

class MsgIdTest
{
	@Test
	void Constructor_NullIdentifier_Throws()
	{
		assertThrows(NullPointerException.class, () -> new MsgId(null));
	}

	@Test
	void Constructor_WrongLength_Throws()
	{
		assertThrows(IllegalArgumentException.class, () -> new MsgId(new byte[17]));
	}

	@Test
	void Constructor_CorrectLength_Success()
	{
		var id = new MsgId(new byte[MsgId.LENGTH]);
		assertNotNull(id);
		assertEquals(MsgId.LENGTH, id.getLength());
	}

	@Test
	void FromString_ValidHex_Success()
	{
		var hex = "00112233445566778899aabbccddeeff00112233";
		var id = MsgId.fromString(hex);
		assertNotNull(id);
		assertFalse(id.isNullIdentifier());
		assertEquals(hex, id.asString());
	}

	@Test
	void FromString_NullInput_ReturnsNullIdentifier()
	{
		var id = MsgId.fromString(null);
		assertTrue(id.isNullIdentifier());
	}

	@Test
	void FromString_WrongLength_ReturnsNullIdentifier()
	{
		var id = MsgId.fromString("abc");
		assertTrue(id.isNullIdentifier());
	}

	@Test
	void FromString_InvalidHex_ReturnsNullIdentifier()
	{
		var id = MsgId.fromString("xyznotahexstring00000000000000000000");
		assertTrue(id.isNullIdentifier());
	}

	@Test
	void Equals_SameBytes_True()
	{
		var bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
		var id1 = new MsgId(bytes);
		var id2 = new MsgId(bytes.clone());
		assertEquals(id1, id2);
		assertEquals(id1.hashCode(), id2.hashCode());
	}

	@Test
	void Equals_DifferentBytes_False()
	{
		var id1 = new MsgId(new byte[MsgId.LENGTH]);
		var bytes = new byte[MsgId.LENGTH];
		bytes[0] = 1;
		var id2 = new MsgId(bytes);
		assertNotEquals(id1, id2);
	}

	@Test
	void CompareTo_LessThan_Negative()
	{
		var id1 = new MsgId(new byte[MsgId.LENGTH]);
		var bytes = new byte[MsgId.LENGTH];
		bytes[MsgId.LENGTH - 1] = 1;
		var id2 = new MsgId(bytes);
		assertTrue(id1.compareTo(id2) < 0);
	}

	@Test
	void CompareTo_Equal_Zero()
	{
		var bytes = new byte[MsgId.LENGTH];
		var id1 = new MsgId(bytes);
		var id2 = new MsgId(bytes.clone());
		assertEquals(0, id1.compareTo(id2));
	}

	@Test
	void CompareTo_GreaterThan_Positive()
	{
		var bytes = new byte[MsgId.LENGTH];
		bytes[MsgId.LENGTH - 1] = 1;
		var id1 = new MsgId(bytes);
		var id2 = new MsgId(new byte[MsgId.LENGTH]);
		assertTrue(id1.compareTo(id2) > 0);
	}

	@Test
	void AsString_ReturnsHexLowercase()
	{
		var bytes = new byte[]{0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d};
		var id = new MsgId(bytes);
		assertEquals("0a0b0c0d0e0f101112131415161718191a1b1c1d", id.asString());
	}

	@Test
	void ToString_EqualsAsString()
	{
		var id = MsgId.fromString("00112233445566778899aabbccddeeff00112233");
		assertEquals(id.asString(), id.toString());
	}

	@Test
	void NullIdentifier_AllZeros()
	{
		var id = new MsgId(new byte[MsgId.LENGTH]);
		assertTrue(id.isNullIdentifier());
	}

	@Test
	void SetBytes_Works()
	{
		var id = new MsgId(new byte[MsgId.LENGTH]);
		var newBytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
		id.setBytes(newBytes);
		assertArrayEquals(newBytes, id.getBytes());
	}
}
