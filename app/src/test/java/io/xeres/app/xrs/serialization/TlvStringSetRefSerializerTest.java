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

import java.util.List;

import static io.xeres.app.xrs.serialization.TlvSerializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvStringSetRefSerializer.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class TlvStringSetRefSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvStringSetRefSerializer.class);
	}

	@Test
	void Serialize_TlvStringSetRef()
	{
		var buf = Unpooled.buffer();
		var input = List.of("foo", "bar", "baz");

		var size = serialize(buf, TlvType.SET_RECOGN, input);
		assertEquals(getSize(input), size);

		var result = deserialize(buf, TlvType.SET_RECOGN);
		assertIterableEquals(input, result);

		buf.release();
	}

	@Test
	void Serialize_TlvStringSetRef_Empty()
	{
		var buf = Unpooled.buffer();
		var input = List.<String>of();

		var size = serialize(buf, TlvType.SET_RECOGN, input);
		assertEquals(TLV_HEADER_SIZE, size);

		var result = deserialize(buf, TlvType.SET_RECOGN);
		assertIterableEquals(input, result);

		buf.release();
	}
}