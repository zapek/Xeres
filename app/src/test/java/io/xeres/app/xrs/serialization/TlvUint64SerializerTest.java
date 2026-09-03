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

import static io.xeres.app.xrs.serialization.TlvUint64Serializer.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TlvUint64SerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvUint64Serializer.class);
	}

	@Test
	void Serialize_TlvUint64()
	{
		var buf = Unpooled.buffer();
		var input = 123456789012345L;

		var size = serialize(buf, TlvType.LONG_OFFSET, input);
		assertEquals(getSize(), size);

		var result = deserialize(buf, TlvType.LONG_OFFSET);
		assertEquals(input, result);

		buf.release();
	}

	@Test
	void Serialize_TlvUint64_Zero()
	{
		var buf = Unpooled.buffer();
		var input = 0L;

		var size = serialize(buf, TlvType.LONG_OFFSET, input);
		assertEquals(getSize(), size);

		var result = deserialize(buf, TlvType.LONG_OFFSET);
		assertEquals(input, result);

		buf.release();
	}

	@Test
	void Serialize_TlvUint64_MaxValue()
	{
		var buf = Unpooled.buffer();
		var input = Long.MAX_VALUE;

		var size = serialize(buf, TlvType.LONG_OFFSET, input);
		assertEquals(getSize(), size);

		var result = deserialize(buf, TlvType.LONG_OFFSET);
		assertEquals(input, result);

		buf.release();
	}

	@Test
	void Serialize_TlvUint64_NegativeValue()
	{
		var buf = Unpooled.buffer();
		var input = -1L;

		var size = serialize(buf, TlvType.LONG_OFFSET, input);
		assertEquals(getSize(), size);

		var result = deserialize(buf, TlvType.LONG_OFFSET);
		assertEquals(input, result);

		buf.release();
	}

	@Test
	void Deserialize_WrongType_ThrowsException()
	{
		var buf = Unpooled.buffer();

		serialize(buf, TlvType.LONG_OFFSET, 123L);
		assertThrows(IllegalArgumentException.class, () -> deserialize(buf, TlvType.UINT_SIZE));

		buf.release();
	}
}