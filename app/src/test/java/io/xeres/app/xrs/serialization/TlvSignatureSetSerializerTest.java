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
import io.xeres.common.id.GxsId;
import io.xeres.testutils.IdFakes;
import io.xeres.testutils.TestUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvSignatureSetSerializer.deserialize;
import static io.xeres.app.xrs.serialization.TlvSignatureSetSerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TlvSignatureSetSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvSignatureSetSerializer.class);
	}

	@Test
	void Serialize_TlvKeySignatureSet()
	{
		var buf = Unpooled.buffer();
		Set<Signature> input = new HashSet<>();
		var gxsId = IdFakes.createGxsId();
		var signature = RandomUtils.insecure().randomBytes(20);
		var keySignature = new Signature(Signature.Type.ADMIN, gxsId, signature);
		input.add(keySignature);

		var size = serialize(buf, input);
		assertEquals(TLV_HEADER_SIZE + TLV_HEADER_SIZE + 4 + TLV_HEADER_SIZE + TLV_HEADER_SIZE + GxsId.LENGTH * 2 + TLV_HEADER_SIZE + signature.length, size);

		var result = deserialize(buf);
		assertEquals(input.stream().findFirst().orElseThrow().getGxsId(), result.stream().findFirst().orElseThrow().getGxsId());
		assertArrayEquals(input.stream().findFirst().orElseThrow().getData(), result.stream().findFirst().orElseThrow().getData());

		buf.release();
	}
}