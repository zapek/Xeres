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
import io.xeres.app.xrs.common.Signature;
import io.xeres.testutils.IdFakes;
import io.xeres.testutils.TestUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.serialization.TlvSignatureSerializer.deserialize;
import static io.xeres.app.xrs.serialization.TlvSignatureSerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TlvSignatureSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvSignatureSerializer.class);
	}

	@Test
	void Serialize_TlvKeySignature()
	{
		var buf = Unpooled.buffer();
		var key = RandomUtils.insecure().randomBytes(30);

		var input = new Signature(IdFakes.createGxsId(), key);

		var size = serialize(buf, input);
		assertEquals(6 + 6 + 38 + key.length, size);

		var result = deserialize(buf);
		assertEquals(input.getGxsId(), result.getGxsId());
		assertArrayEquals(input.getData(), result.getData());

		buf.release();
	}
}