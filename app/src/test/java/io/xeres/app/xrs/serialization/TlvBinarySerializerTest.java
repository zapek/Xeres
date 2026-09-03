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

package io.xeres.app.xrs.serialization;

import io.netty.buffer.Unpooled;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.serialization.TlvBinarySerializer.*;
import static io.xeres.app.xrs.serialization.TlvSerializer.TLV_HEADER_SIZE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TlvBinarySerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvBinarySerializer.class);
	}

	@Test
	void Serialize_TlvBinary()
	{
		var buf = Unpooled.buffer();
		var input = new byte[]{1, 2, 3, 4};

		var size = serialize(buf, TlvType.BIN_IMAGE, input);
		assertEquals(getSize(input), size);
		assertEquals(TLV_HEADER_SIZE + input.length, size);

		var result = deserialize(buf, TlvType.BIN_IMAGE);
		assertArrayEquals(input, result);

		buf.release();
	}

	@Test
	void Serialize_TlvBinary_Empty()
	{
		var buf = Unpooled.buffer();
		var input = new byte[0];

		var size = serialize(buf, TlvType.BIN_IMAGE, input);
		assertEquals(TLV_HEADER_SIZE, size);

		var result = deserialize(buf, TlvType.BIN_IMAGE);
		assertArrayEquals(input, result);

		buf.release();
	}

	@Test
	void Serialize_TlvBinary_Null()
	{
		var buf = Unpooled.buffer();

		var size = serialize(buf, TlvType.BIN_IMAGE, null);
		assertEquals(TLV_HEADER_SIZE, size);

		var result = deserialize(buf, TlvType.BIN_IMAGE);
		assertArrayEquals(new byte[0], result);

		buf.release();
	}

	@Test
	void Serialize_TlvBinary_WithIntType()
	{
		var buf = Unpooled.buffer();
		var input = new byte[]{7, 8};

		var size = serialize(buf, TlvType.BIN_IMAGE.getValue(), input);
		assertEquals(TLV_HEADER_SIZE + input.length, size);

		var result = deserialize(buf, TlvType.BIN_IMAGE.getValue());
		assertArrayEquals(input, result);

		buf.release();
	}
}