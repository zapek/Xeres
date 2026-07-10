/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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
import io.xeres.app.database.model.location.LocationFakes;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SerializerTest
{
	@Test
	void Serialize_ComplexObject()
	{
		var buf = Unpooled.buffer();

		var input = new SerialAll();

		input.setIntPrimitiveField(5);
		input.setIntegerField(5);

		input.setShortPrimitiveField((short) 8);
		input.setShortField((short) 8);

		input.setBytePrimitiveField((byte) 10);
		input.setByteField((byte) 10);

		input.setLongPrimitiveField(12L);
		input.setLongField(12L);

		input.setFloatPrimitiveField(14f);
		input.setFloatField(14f);

		input.setDoublePrimitiveField(16.0);
		input.setDoubleField(16.0);

		input.setBooleanPrimitiveField(true);
		input.setBooleanField(true);

		input.setBytes(new byte[]{1, 2, 3});

		input.setBigInteger(new BigInteger("123456789"));

		input.setLocationIdentifier(LocationFakes.createLocation().getLocationIdentifier());

		input.setStringList(List.of("foo", "bar"));

		input.setStringMap(Map.of(1, "bleh", 2, "plop"));

		input.setSerialEnum(SerialEnum.THREE);

		input.setEnumSet(EnumSet.of(SerialEnum.ONE, SerialEnum.TWO));

		input.setEnumSetByte(EnumSet.of(SerialEnum.ONE, SerialEnum.TWO));

		input.setEnumSetShort(EnumSet.of(SerialEnum.ONE, SerialEnum.TWO));

		input.setTlvName("foobar");

		var size = Serializer.serialize(buf, input.getClass(), input, null);
		assertTrue(size > 0);

		var result = Serializer.deserialize(buf, SerialAll.class);

		assertEquals(input.getIntPrimitiveField(), result.getIntPrimitiveField());
		assertEquals(input.getIntegerField(), result.getIntegerField());

		assertEquals(input.getShortPrimitiveField(), result.getShortPrimitiveField());
		assertEquals(input.getShortField(), result.getShortField());

		assertEquals(input.getBytePrimitiveField(), result.getBytePrimitiveField());
		assertEquals(input.getByteField(), result.getByteField());

		assertEquals(input.getLongPrimitiveField(), result.getLongPrimitiveField());
		assertEquals(input.getLongField(), result.getLongField());

		assertEquals(input.getFloatPrimitiveField(), result.getFloatPrimitiveField());
		assertEquals(input.getFloatField(), result.getFloatField());

		assertEquals(input.getDoublePrimitiveField(), result.getDoublePrimitiveField());
		assertEquals(input.getDoubleField(), result.getDoubleField());

		assertEquals(input.isBooleanPrimitiveField(), result.isBooleanPrimitiveField());
		assertEquals(input.getBooleanField(), result.getBooleanField());

		assertArrayEquals(input.getBytes(), result.getBytes());

		assertEquals(input.getBigInteger(), result.getBigInteger());

		assertEquals(input.getLocationIdentifier().getLength(), result.getLocationIdentifier().getLength());
		assertArrayEquals(input.getLocationIdentifier().getBytes(), result.getLocationIdentifier().getBytes());

		assertEquals(input.getStringList().size(), result.getStringList().size());
		assertIterableEquals(input.getStringList(), result.getStringList());

		assertEquals(input.getStringMap().size(), result.getStringMap().size());
		assertEquals(input.getStringMap(), result.getStringMap());

		assertEquals(input.getSerialEnum(), result.getSerialEnum());

		assertEquals(input.getEnumSet(), result.getEnumSet());

		assertEquals(input.getEnumSetByte(), result.getEnumSetByte());

		assertEquals(input.getEnumSetShort(), result.getEnumSetShort());

		assertEquals(input.getTlvName(), result.getTlvName());

		buf.release();
	}
}
