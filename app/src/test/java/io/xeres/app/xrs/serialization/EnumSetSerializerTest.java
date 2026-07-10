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

import java.util.EnumSet;

import static io.xeres.app.xrs.serialization.EnumSetSerializer.deserialize;
import static io.xeres.app.xrs.serialization.EnumSetSerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnumSetSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(EnumSetSerializer.class);
	}

	@Test
	void Serialize_EnumSet()
	{
		var buf = Unpooled.buffer();

		var input = EnumSet.of(SerialEnum.TWO, SerialEnum.FOUR);

		var size = serialize(buf, input, FieldSize.INTEGER);
		assertEquals(4, size);
		assertEquals(1 << 1 | 1 << 3, buf.getInt(0));

		var result = deserialize(buf, SerialEnum.class, FieldSize.INTEGER);
		assertEquals(input, result);

		buf.release();
	}

	@Test
	void Serialize_EnumSet_Null()
	{
		var buf = Unpooled.buffer();

		assertThrows(NullPointerException.class, () -> serialize(buf, null, FieldSize.INTEGER));
		buf.release();
	}
}