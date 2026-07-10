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
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;
import io.xeres.testutils.IdFakes;
import io.xeres.testutils.TestUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvSetSerializer.deserializeIdentifier;
import static io.xeres.app.xrs.serialization.TlvSetSerializer.serializeIdentifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TlvSetSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvSetSerializer.class);
	}

	@Test
	void Serialize_TlvSet_GxsId()
	{
		var buf = Unpooled.buffer();
		var gxsId1 = IdFakes.createGxsId();
		var gxsId2 = IdFakes.createGxsId();
		Set<GxsId> input = new HashSet<>();
		input.add(gxsId1);
		input.add(gxsId2);

		var size = serializeIdentifier(buf, TlvType.SET_GXS_ID, input);
		assertEquals(TLV_HEADER_SIZE + GxsId.LENGTH * input.size(), size);

		@SuppressWarnings("unchecked") var result = (Set<GxsId>) deserializeIdentifier(buf, TlvType.SET_GXS_ID, GxsId.class);
		assertEquals(2, result.size());
		assertTrue(result.contains(gxsId1));
		assertTrue(result.contains(gxsId2));

		buf.release();
	}

	@Test
	void Serialize_TlvSet_MsgId()
	{
		var buf = Unpooled.buffer();
		var msgId1 = new MsgId(RandomUtils.insecure().randomBytes(MsgId.LENGTH));
		var msgId2 = new MsgId(RandomUtils.insecure().randomBytes(MsgId.LENGTH));
		Set<MsgId> input = new HashSet<>();
		input.add(msgId1);
		input.add(msgId2);

		var size = serializeIdentifier(buf, TlvType.SET_GXS_MSG_ID, input);
		assertEquals(TLV_HEADER_SIZE + MsgId.LENGTH * input.size(), size);

		@SuppressWarnings("unchecked") var result = (Set<MsgId>) deserializeIdentifier(buf, TlvType.SET_GXS_MSG_ID, MsgId.class);
		assertEquals(2, result.size());
		assertTrue(result.contains(msgId1));
		assertTrue(result.contains(msgId2));

		buf.release();
	}
}