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

package io.xeres.common.id;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IdTest
{
	@Test
	void Instance_Throws() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(Id.class);
	}

	@Test
	void ToString_FromBytes_Success()
	{
		var value = "13352839ab34093f";
		var id = new BigInteger(value, 16);

		var result = Id.toString(id.toByteArray());

		assertEquals(value, result);
	}

	@Test
	void ToString_FromBytes_Null_Success()
	{
		var result = Id.toString((byte[]) null);

		assertEquals("", result);
	}

	@Test
	void ToString_FromBytes_Empty_Success()
	{
		var result = Id.toString(new byte[0]);

		assertEquals("", result);
	}

	@Test
	void ToBytes_FromString_Success()
	{
		var id = "e40f238ecb395023";

		var result = Id.toBytes(id);

		assertArrayEquals(new byte[]{(byte) 0xe4, 0xf, 0x23, (byte) 0x8e, (byte) 0xcb, 0x39, 0x50, 0x23}, result);
	}

	@Test
	void ToBytes_FromString_Null_Success()
	{
		var result = Id.toBytes(null);

		assertArrayEquals(new byte[0], result);
	}

	@Test
	void ToBytes_FromString_Empty_Success()
	{
		var result = Id.toBytes("");

		assertArrayEquals(new byte[0], result);
	}

	@Test
	void ToString_FromLong_Success()
	{
		var id = 0x2344ab38L;

		var result = Id.toString(id);

		assertEquals("2344AB38", result);
	}

	@Test
	void ToString_FromIdentifier_Success()
	{
		var gxsId = new GxsId(new byte[]{0x32, 0x5e, 0x38, 0x1, (byte) 0x98, (byte) 0x8a, 0x34, 0x73, 0x47, (byte) 0xef, 0x3e, 0x5a, (byte) 0xe2, 0x4a, 0x63, (byte) 0xba});

		var result = Id.toString(gxsId);

		assertEquals("325e3801988a347347ef3e5ae24a63ba", result);
	}

	@Test
	void AsciiToBytes_Success()
	{
		byte[] id = {0x30, 0x30, 0x35, 0x36, 0x33, 0x65, 0x38, 0x36, 0x61, 0x31, 0x64, 0x62, 0x36, 0x61, 0x61, 0x30, 0x32, 0x64, 0x36, 0x62, 0x36, 0x65, 0x38, 0x66, 0x37, 0x64, 0x61, 0x32, 0x62, 0x36, 0x39, 0x35};

		var result = Id.asciiToBytes(id);

		assertArrayEquals(new byte[]{0x0, 0x56, 0x3e, (byte) 0x86, (byte) 0xa1, (byte) 0xdb, 0x6a, (byte) 0xa0, 0x2d, 0x6b, 0x6e, (byte) 0x8f, 0x7d, (byte) 0xa2, (byte) 0xb6, (byte) 0x95}, result);
	}

	@Test
	void IdentifierToAscii_Success()
	{
		var gxsId = new GxsId(new byte[]{0x32, 0x5e, 0x38, 0x1, (byte) 0x98, (byte) 0x8a, 0x34, 0x73, 0x47, (byte) 0xef, 0x3e, 0x5a, (byte) 0xe2, 0x4a, 0x63, (byte) 0xba});

		var result = Id.toAsciiBytes(gxsId);

		assertArrayEquals(new byte[]{0x33, 0x32, 0x35, 0x65, 0x33, 0x38, 0x30, 0x31, 0x39, 0x38, 0x38, 0x61, 0x33, 0x34, 0x37, 0x33, 0x34, 0x37, 0x65, 0x66, 0x33, 0x65, 0x35, 0x61, 0x65, 0x32, 0x34, 0x61, 0x36, 0x33, 0x62, 0x61}, result);
	}
}
