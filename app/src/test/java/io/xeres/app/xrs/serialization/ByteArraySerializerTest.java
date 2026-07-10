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

import static io.xeres.app.xrs.serialization.ByteArraySerializer.deserialize;
import static io.xeres.app.xrs.serialization.ByteArraySerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteArraySerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ByteArraySerializer.class);
	}

	@Test
	void Serialize_ByteArray()
	{
		var buf = Unpooled.buffer();

		var input = new byte[]{1, 2, 3};

		var size = serialize(buf, input);

		assertEquals(4 + input.length, size);
		var output = new byte[input.length];
		buf.getBytes(4, output);
		assertArrayEquals(input, output);

		var result = deserialize(buf);
		assertArrayEquals(input, result);
		buf.release();
	}

	@Test
	void Serialize_ByteArray_Null()
	{
		var buf = Unpooled.buffer();

		var size = serialize(buf, null);
		assertEquals(4, size);
		buf.release();
	}
}