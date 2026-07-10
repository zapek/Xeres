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
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static io.xeres.app.xrs.serialization.ListSerializer.deserialize;
import static io.xeres.app.xrs.serialization.ListSerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ListSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ListSerializer.class);
	}

	@Test
	void Serialize_List()
	{
		var buf = Unpooled.buffer();

		var input = List.of("hello", "dude");
		List<String> output = new ArrayList<>();

		var size = serialize(buf, input);

		deserialize(buf, output, new ParameterizedType()
		{
			@Override
			public Type @NonNull [] getActualTypeArguments()
			{
				return new Type[]{String.class};
			}

			@Override
			public @NonNull Type getRawType()
			{
				return List.class;
			}

			@Override
			public Type getOwnerType()
			{
				return null;
			}
		});
		assertEquals(input.size(), output.size());
		assertArrayEquals(input.getFirst().getBytes(), output.getFirst().getBytes());
		assertArrayEquals(input.get(1).getBytes(), output.get(1).getBytes());

		assertEquals(4 + 4 + input.get(0).getBytes().length + 4 + input.get(1).getBytes().length, size);
		assertEquals(input.size(), buf.getInt(0));
		buf.release();
	}

	@Test
	void Serialize_List_Null()
	{
		var buf = Unpooled.buffer();

		var size = serialize(buf, null);
		assertEquals(4, size);

		buf.release();
	}
}