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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.xeres.app.xrs.serialization.LongSerializer.deserialize;
import static io.xeres.app.xrs.serialization.LongSerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LongSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(LongSerializer.class);
	}

	@ParameterizedTest
	@ValueSource(longs = {Long.MIN_VALUE, Long.MAX_VALUE, 0L, 5L})
	void Serialize_Long(long input)
	{
		var buf = Unpooled.buffer();

		var size = serialize(buf, input);

		assertEquals(8, size);
		assertEquals(input, buf.getLong(0));

		var result = deserialize(buf);
		assertEquals(input, result);
		buf.release();
	}
}