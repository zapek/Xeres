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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.xeres.app.xrs.serialization.RsSerializableSerializer.deserialize;
import static io.xeres.app.xrs.serialization.RsSerializableSerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RsSerializableSerializerTest implements RsSerializable
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(RsSerializableSerializer.class);
	}

	@Test
	void Serialize()
	{
		var buf = Unpooled.buffer();

		var size = serialize(buf, this);

		assertEquals(15, size);

		deserialize(buf, this); // assert is in readObject() below

		buf.release();
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		return Serializer.serialize(buf, "hello world");
	}

	@Override
	public void readObject(ByteBuf buf)
	{
		assertEquals("hello world", Serializer.deserializeString(buf));
	}
}