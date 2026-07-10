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

import static io.xeres.app.xrs.serialization.TlvStringSerializer.deserialize;
import static io.xeres.app.xrs.serialization.TlvStringSerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TlvStringSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvStringSerializer.class);
	}

	@Test
	void Serialize_TlvString()
	{
		var buf = Unpooled.buffer();

		var input = "foobar";

		var size = serialize(buf, TlvType.STR_NAME, input);
		assertEquals(6 + input.getBytes().length, size);

		var result = deserialize(buf, TlvType.STR_NAME);
		assertEquals(input, result);

		buf.release();
	}
}