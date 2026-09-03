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

import static io.xeres.app.xrs.serialization.ArraySerializer.deserialize;
import static io.xeres.app.xrs.serialization.ArraySerializer.serialize;
import static org.junit.jupiter.api.Assertions.*;

class ArraySerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ArraySerializer.class);
	}

	@Test
	void Serialize_ByteArray()
	{
		var buf = Unpooled.buffer();
		var input = new byte[]{1, 2, 3};

		var size = serialize(buf, byte[].class, input);
		assertEquals(4 + input.length, size);

		var result = deserialize(buf, byte[].class);
		assertArrayEquals(input, result);

		buf.release();
	}

	@Test
	void Serialize_UnhandledType_ThrowsException()
	{
		var buf = Unpooled.buffer();

		assertThrows(IllegalArgumentException.class, () -> serialize(buf, int[].class, new int[]{1, 2}));
		assertThrows(IllegalArgumentException.class, () -> deserialize(buf, int[].class));

		buf.release();
	}
}