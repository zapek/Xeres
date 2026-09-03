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

class ProfileFingerprintTest
{
	@Test
	void Constructor_NullIdentifier_Throws()
	{
		assertThrows(NullPointerException.class, () -> new ProfileFingerprint(null));
	}

	@Test
	void Constructor_WrongLength_Throws()
	{
		assertThrows(IllegalArgumentException.class, () -> new ProfileFingerprint(new byte[17]));
	}

	@Test
	void Constructor_V4Length_Success()
	{
		var fp = new ProfileFingerprint(new byte[ProfileFingerprint.V4_LENGTH]);
		assertNotNull(fp);
		assertEquals(ProfileFingerprint.V4_LENGTH, fp.getLength());
	}

	@Test
	void Constructor_FullLength_Success()
	{
		var fp = new ProfileFingerprint(new byte[ProfileFingerprint.LENGTH]);
		assertNotNull(fp);
		assertEquals(ProfileFingerprint.LENGTH, fp.getLength());
	}

	@Test
	void GetBytes_ReturnsCorrectData()
	{
		var bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
		var fp = new ProfileFingerprint(bytes);
		assertArrayEquals(bytes, fp.getBytes());
	}

	@Test
	void AsString_ReturnsHexUppercase()
	{
		var bytes = new byte[]{0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d};
		var fp = new ProfileFingerprint(bytes);
		assertEquals("0A0B0C0D0E0F101112131415161718191A1B1C1D", fp.asString());
	}

	@Test
	void ToString_V4Length_FormatsWithDoubleSpaceSeparator()
	{
		var bytes = new byte[ProfileFingerprint.V4_LENGTH];
		var fp = new ProfileFingerprint(bytes);
		var result = fp.toString();
		// 20 bytes = 40 hex chars, split into 10 groups of 4 with spaces, double space between byte 10 and 11
		assertEquals("0000 0000 0000 0000 0000  0000 0000 0000 0000 0000", result);
	}

	@Test
	void Equals_SameBytes_True()
	{
		var bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
		var fp1 = new ProfileFingerprint(bytes);
		var fp2 = new ProfileFingerprint(bytes.clone());
		assertEquals(fp1, fp2);
		assertEquals(fp1.hashCode(), fp2.hashCode());
	}

	@Test
	void Equals_DifferentBytes_False()
	{
		var fp1 = new ProfileFingerprint(new byte[ProfileFingerprint.V4_LENGTH]);
		var bytes = new byte[ProfileFingerprint.V4_LENGTH];
		bytes[0] = 1;
		var fp2 = new ProfileFingerprint(bytes);
		assertNotEquals(fp1, fp2);
	}

	@Test
	void Equals_DifferentLengthsSameContent_False()
	{
		var v4 = new byte[ProfileFingerprint.V4_LENGTH];
		var full = new byte[ProfileFingerprint.LENGTH];
		var fp1 = new ProfileFingerprint(v4);
		var fp2 = new ProfileFingerprint(full);
		assertNotEquals(fp1, fp2);
	}

	@Test
	void SetBytes_Works()
	{
		var fp = new ProfileFingerprint(new byte[ProfileFingerprint.V4_LENGTH]);
		var newBytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
		fp.setBytes(newBytes);
		assertArrayEquals(newBytes, fp.getBytes());
	}

	@Test
	void GetLength_V4_Returns20()
	{
		var fp = new ProfileFingerprint(new byte[ProfileFingerprint.V4_LENGTH]);
		assertEquals(20, fp.getLength());
	}

	@Test
	void GetLength_Full_Returns32()
	{
		var fp = new ProfileFingerprint(new byte[ProfileFingerprint.LENGTH]);
		assertEquals(32, fp.getLength());
	}
}
