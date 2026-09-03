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

import java.math.BigInteger;

import static io.xeres.app.xrs.serialization.BigIntegerSerializer.deserialize;
import static io.xeres.app.xrs.serialization.BigIntegerSerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BigIntegerSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(BigIntegerSerializer.class);
	}

	@Test
	void Serialize_BigInteger()
	{
		var buf = Unpooled.buffer();
		var input = new BigInteger("123456789123456789");

		var size = serialize(buf, input);
		assertEquals(4 + input.toByteArray().length, size);

		var result = deserialize(buf);
		assertEquals(input, result);

		buf.release();
	}

	@Test
	void Serialize_BigInteger_Zero()
	{
		var buf = Unpooled.buffer();
		var input = BigInteger.ZERO;

		var size = serialize(buf, input);
		assertEquals(4 + input.toByteArray().length, size);

		var result = deserialize(buf);
		assertEquals(input, result);

		buf.release();
	}
}