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

import java.time.Instant;

import static io.xeres.app.xrs.serialization.InstantSerializer.deserialize;
import static io.xeres.app.xrs.serialization.InstantSerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InstantSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(InstantSerializer.class);
	}

	@ParameterizedTest
	@ValueSource(longs = {0L, 1_700_000_000L, 2_147_483_647L, 4_294_967_295L})
	void Serialize_Instant(long epochSecond)
	{
		var buf = Unpooled.buffer();

		var input = Instant.ofEpochSecond(epochSecond);

		var size = serialize(buf, input);

		assertEquals(4, size);
		assertEquals(epochSecond, buf.getUnsignedInt(0));

		var result = deserialize(buf);
		assertEquals(input, result);
		buf.release();
	}

	@Test
	void Serialize_NullInstant()
	{
		var buf = Unpooled.buffer();

		var size = serialize(buf, null);

		assertEquals(4, size);
		assertEquals(0, buf.getUnsignedInt(0));

		var result = deserialize(buf);
		assertEquals(Instant.EPOCH, result);
		buf.release();
	}
}