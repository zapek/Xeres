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
import java.util.HashMap;
import java.util.Map;

import static io.xeres.app.xrs.serialization.TlvMapSerializer.deserialize;
import static io.xeres.app.xrs.serialization.TlvMapSerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TlvMapSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvMapSerializer.class);
	}

	@Test
	void Serialize_TlvMap()
	{
		var buf = Unpooled.buffer();

		Map<Integer, String> input = Map.of(1, "foo", 2, "barbaz");
		Map<Integer, String> output = new HashMap<>();

		var size = serialize(buf, TlvType.TLV_ONE, TlvType.TLV_ONE, TlvType.TLV_ONE, TlvType.TLV_ONE, input);

		deserialize(buf, TlvType.TLV_ONE, TlvType.TLV_ONE, TlvType.TLV_ONE, TlvType.TLV_ONE, output, new ParameterizedType()
		{
			@Override
			public Type @NonNull [] getActualTypeArguments()
			{
				return new Type[]{Integer.class, String.class};
			}

			@Override
			public @NonNull Type getRawType()
			{
				return Map.class;
			}

			@Override
			public Type getOwnerType()
			{
				return null;
			}
		});

		assertEquals(input.size(), output.size());
		assertArrayEquals(input.get(1).getBytes(), output.get(1).getBytes());
		assertArrayEquals(input.get(2).getBytes(), output.get(2).getBytes());

		assertEquals(67, size);
		buf.release();
	}

	@Test
	void Serialize_TlvMap_Null()
	{
		var buf = Unpooled.buffer();

		var size = serialize(buf, TlvType.TLV_ONE, TlvType.TLV_ONE, TlvType.TLV_ONE, TlvType.TLV_ONE, null);
		assertEquals(6, size);

		buf.release();
	}
}