/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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
import io.xeres.app.database.model.gxs.ForumGroupItemFakes;
import io.xeres.app.database.model.gxs.ForumMessageItemFakes;
import io.xeres.app.database.model.gxs.IdentityGroupItemFakes;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.xrs.common.Signature;
import io.xeres.app.xrs.common.SignatureSet;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.MessageId;
import io.xeres.testutils.IdFakes;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static org.junit.jupiter.api.Assertions.*;

class SerializerTest
{
	@ParameterizedTest
	@ValueSource(ints = {Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 5})
	void Serializer_Serialize_Int(int input)
	{
		var buf = Unpooled.buffer();

		var size = Serializer.serialize(buf, input);

		assertEquals(4, size);
		assertEquals(input, buf.getInt(0));

		var result = Serializer.deserializeInt(buf);
		assertEquals(input, result);
		buf.release();
	}

	@Test
	void Serializer_Serialize_Int_Null()
	{
		var buf = Unpooled.buffer();

		assertThrows(NullPointerException.class, () -> Serializer.serialize(buf, (Integer) null));
		buf.release();
	}

	@ParameterizedTest
	@ValueSource(shorts = {Short.MIN_VALUE, Short.MAX_VALUE, 0, 5})
	void Serializer_Serialize_Short(short input)
	{
		var buf = Unpooled.buffer();

		var size = Serializer.serialize(buf, input);

		assertEquals(2, size);
		assertEquals(input, buf.getShort(0));

		var result = Serializer.deserializeShort(buf);
		assertEquals(input, result);
		buf.release();
	}

	@Test
	void Serializer_Serialize_Short_Null()
	{
		var buf = Unpooled.buffer();

		assertThrows(NullPointerException.class, () -> Serializer.serialize(buf, (Short) null));
		buf.release();
	}

	@ParameterizedTest
	@ValueSource(bytes = {Byte.MIN_VALUE, Byte.MAX_VALUE, 0, 5})
	void Serializer_Serialize_Byte(byte input)
	{
		var buf = Unpooled.buffer();

		var size = Serializer.serialize(buf, input);

		assertEquals(1, size);
		assertEquals(input, buf.getByte(0));

		var result = Serializer.deserializeByte(buf);
		assertEquals(input, result);
		buf.release();
	}

	@Test
	void Serializer_Serialize_Byte_Null()
	{
		var buf = Unpooled.buffer();

		assertThrows(NullPointerException.class, () -> Serializer.serialize(buf, (Byte) null));
		buf.release();
	}

	@ParameterizedTest
	@ValueSource(longs = {Long.MIN_VALUE, Long.MAX_VALUE, 0L, 5L})
	void Serializer_Serialize_Long(long input)
	{
		var buf = Unpooled.buffer();

		var size = Serializer.serialize(buf, input);

		assertEquals(8, size);
		assertEquals(input, buf.getLong(0));

		var result = Serializer.deserializeLong(buf);
		assertEquals(input, result);
		buf.release();
	}

	@Test
	void Serializer_Serialize_Long_Null()
	{
		var buf = Unpooled.buffer();

		assertThrows(NullPointerException.class, () -> Serializer.serialize(buf, (Long) null));
		buf.release();
	}

	@ParameterizedTest
	@ValueSource(floats = {Float.MIN_VALUE, Float.MAX_VALUE, 0f, 5f})
	void Serializer_Serialize_Float(float input)
	{
		var buf = Unpooled.buffer();

		var size = Serializer.serialize(buf, input);

		assertEquals(4, size);
		assertEquals(input, buf.getFloat(0));

		var result = Serializer.deserializeFloat(buf);
		assertEquals(input, result);
		buf.release();
	}

	@Test
	void Serializer_Serialize_Float_Null()
	{
		var buf = Unpooled.buffer();

		assertThrows(NullPointerException.class, () -> Serializer.serialize(buf, (Float) null));
		buf.release();
	}

	@ParameterizedTest
	@ValueSource(doubles = {Double.MIN_VALUE, Double.MAX_VALUE, 0.0, 5.0})
	void Serializer_Serialize_Double(double input)
	{
		var buf = Unpooled.buffer();

		var size = Serializer.serialize(buf, input);

		assertEquals(8, size);
		assertEquals(input, buf.getDouble(0));

		var result = Serializer.deserializeDouble(buf);
		assertEquals(input, result);
		buf.release();
	}

	@Test
	void Serializer_Serialize_Double_Null()
	{
		var buf = Unpooled.buffer();

		assertThrows(NullPointerException.class, () -> Serializer.serialize(buf, (Double) null));
		buf.release();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void Serializer_Serialize_Boolean(boolean input)
	{
		var buf = Unpooled.buffer();

		var size = Serializer.serialize(buf, input);

		assertEquals(1, size);
		assertEquals(input, buf.getBoolean(0));

		var result = Serializer.deserializeBoolean(buf);
		assertEquals(input, result);
		buf.release();
	}

	@Test
	void Serializer_Serialize_Boolean_Null()
	{
		var buf = Unpooled.buffer();

		assertThrows(NullPointerException.class, () -> Serializer.serialize(buf, (Boolean) null));
		buf.release();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "hello", "hello world", " "})
	void Serializer_Serialize_String(String input)
	{
		var buf = Unpooled.buffer();

		var size = Serializer.serialize(buf, input);

		var stringBytes = input.getBytes();

		assertEquals(stringBytes.length + 4, size);
		var output = new byte[stringBytes.length];
		buf.getBytes(4, output);
		assertArrayEquals(stringBytes, output);

		var result = Serializer.deserializeString(buf);
		assertEquals(input, result);
		buf.release();
	}

	@Test
	void Serializer_Serialize_String_Null()
	{
		var buf = Unpooled.buffer();

		var size = Serializer.serialize(buf, (String) null);
		assertEquals(4, size);
		buf.release();
	}

	@Test
	void Serializer_Serialize_ByteArray()
	{
		var buf = Unpooled.buffer();

		var input = new byte[]{1, 2, 3};

		var size = Serializer.serialize(buf, input);

		assertEquals(4 + input.length, size);
		var output = new byte[input.length];
		buf.getBytes(4, output);
		assertArrayEquals(input, output);

		var result = Serializer.deserializeByteArray(buf);
		assertArrayEquals(input, result);
		buf.release();
	}

	@Test
	void Serializer_Serialize_ByteArray_Null()
	{
		var buf = Unpooled.buffer();

		var size = Serializer.serialize(buf, (byte[]) null);
		assertEquals(4, size);
		buf.release();
	}

	@Test
	void Serializer_Serialize_Identifier()
	{
		var buf = Unpooled.buffer();

		var input = LocationFakes.createLocation().getLocationId();

		var size = Serializer.serialize(buf, input, LocationId.class);

		assertEquals(input.getLength(), size);
		var output = new byte[input.getLength()];
		buf.getBytes(0, output);
		assertArrayEquals(input.getBytes(), output);

		var result = (LocationId) Serializer.deserialize(buf, LocationId.class);

		assertEquals(input, result);
		buf.release();
	}

	@Test
	void Serializer_Serialize_Identifier_Null()
	{
		var buf = Unpooled.buffer();

		var size = IdentifierSerializer.serialize(buf, GxsId.class, null);
		assertEquals(GxsId.LENGTH, size);

		var result = (GxsId) Serializer.deserialize(buf, GxsId.class);
		assertNull(result);
		buf.release();
	}

	@Test
	void Serializer_Serialize_List()
	{
		var buf = Unpooled.buffer();

		var input = List.of("hello", "dude");

		var size = Serializer.serialize(buf, input.getClass(), input, null);

		var listObject = new SerialList();
		var result = Serializer.deserializeAnnotatedFields(buf, listObject);
		assertTrue(result);
		assertEquals(input.size(), listObject.getList().size());
		assertArrayEquals(input.get(0).getBytes(), listObject.getList().get(0).getBytes());
		assertArrayEquals(input.get(1).getBytes(), listObject.getList().get(1).getBytes());

		assertEquals(4 + 4 + input.get(0).getBytes().length + 4 + input.get(1).getBytes().length, size);
		assertEquals(input.size(), buf.getInt(0));
		buf.release();
	}

	@Test
	void Serializer_Serialize_List_Null()
	{
		var buf = Unpooled.buffer();

		var size = Serializer.serialize(buf, List.class, null, null);
		assertEquals(4, size);

		buf.release();
	}

	@Test
	void Serializer_Serialize_Map()
	{
		var buf = Unpooled.buffer();

		var input = Map.of(1, "foo", 2, "barbaz");

		var size = Serializer.serialize(buf, input.getClass(), input, null);

		var mapObject = new SerialMap();
		var result = Serializer.deserializeAnnotatedFields(buf, mapObject);
		assertTrue(result);
		assertEquals(input.size(), mapObject.getMap().size());
		assertArrayEquals(input.get(1).getBytes(), mapObject.getMap().get(1).getBytes());
		assertArrayEquals(input.get(2).getBytes(), mapObject.getMap().get(2).getBytes());

		assertEquals(67, size);
		buf.release();
	}

	@Test
	void Serializer_Serialize_Map_Null()
	{
		var buf = Unpooled.buffer();

		var size = Serializer.serialize(buf, Map.class, null, null);
		assertEquals(6, size);

		buf.release();
	}

	@Test
	void Serializer_Serialize_Enum()
	{
		var buf = Unpooled.buffer();

		var input = SerialEnum.TWO;

		var size = Serializer.serialize(buf, input);
		assertEquals(4, size);
		assertEquals(1, buf.getInt(0));

		var result = Serializer.deserializeEnum(buf, SerialEnum.class);
		assertEquals(input, result);

		buf.release();
	}

	@Test
	void Serializer_Serialize_Enum_Null()
	{
		var buf = Unpooled.buffer();

		assertThrows(NullPointerException.class, () -> Serializer.serialize(buf, (Enum<?>) null));
		buf.release();
	}

	@Test
	void Serializer_Serialize_EnumSet()
	{
		var buf = Unpooled.buffer();

		var input = EnumSet.of(SerialEnum.TWO, SerialEnum.FOUR);

		var size = Serializer.serialize(buf, input, FieldSize.INTEGER);
		assertEquals(4, size);
		assertEquals(1 << 1 | 1 << 3, buf.getInt(0));

		var result = Serializer.deserializeEnumSet(buf, SerialEnum.class, FieldSize.INTEGER);
		assertEquals(input, result);

		buf.release();
	}

	@Test
	void Serializer_Serialize_EnumSet_Null()
	{
		var buf = Unpooled.buffer();

		assertThrows(NullPointerException.class, () -> Serializer.serialize(buf, (EnumSet<?>) null, FieldSize.INTEGER));
		buf.release();
	}

	@Test
	void Serializer_Serialize_TlvString()
	{
		var buf = Unpooled.buffer();

		var input = "foobar";

		var size = Serializer.serialize(buf, TlvType.STR_NAME, input);
		assertEquals(6 + input.getBytes().length, size);

		var result = Serializer.deserialize(buf, TlvType.STR_NAME);
		assertEquals(input, result);

		buf.release();
	}

	@Test
	void Serializer_Serialize_TlvKeySignature()
	{
		var buf = Unpooled.buffer();
		var key = new byte[1];

		var input = new Signature(IdFakes.createGxsId(), key);

		var size = Serializer.serialize(buf, TlvType.SIGNATURE, input);
		assertEquals(6 + 6 + 38 + key.length, size);

		var result = (Signature) Serializer.deserialize(buf, TlvType.SIGNATURE);
		assertEquals(input.gxsId(), result.gxsId());
		assertArrayEquals(input.data(), result.data());

		buf.release();
	}

	@Test
	void Serializer_Serialize_TlvKeySignatureSet()
	{
		var buf = Unpooled.buffer();
		var input = new SignatureSet();
		var gxsId = IdFakes.createGxsId();
		var signature = RandomUtils.nextBytes(20);
		var keySignature = new Signature(gxsId, signature);
		input.put(SignatureSet.Type.ADMIN, keySignature);

		var size = Serializer.serialize(buf, TlvType.SIGNATURE_SET, input);
		assertEquals(TLV_HEADER_SIZE + TLV_HEADER_SIZE + 4 + TLV_HEADER_SIZE + TLV_HEADER_SIZE + GxsId.LENGTH * 2 + TLV_HEADER_SIZE + signature.length, size);

		var result = (SignatureSet) Serializer.deserialize(buf, TlvType.SIGNATURE_SET);
		assertEquals(input.getSignatures().get(SignatureSet.Type.ADMIN.getValue()).gxsId(), result.getSignatures().get(SignatureSet.Type.ADMIN.getValue()).gxsId());
		assertArrayEquals(input.getSignatures().get(SignatureSet.Type.ADMIN.getValue()).data(), result.getSignatures().get(SignatureSet.Type.ADMIN.getValue()).data());

		buf.release();
	}

	@Test
	void Serializer_Serialize_TlvImage()
	{
		var buf = Unpooled.buffer();
		var input = new byte[2];

		var size = Serializer.serialize(buf, TlvType.IMAGE, input);
		assertEquals(6 + 6 + 4 + input.length, size);

		var result = (byte[]) Serializer.deserialize(buf, TlvType.IMAGE);
		assertArrayEquals(input, result);

		buf.release();
	}

	@Test
	void Serializer_Serialize_TlvImage_Empty_Array()
	{
		var buf = Unpooled.buffer();
		var input = new byte[0];

		var size = Serializer.serialize(buf, TlvType.IMAGE, input);
		assertEquals(6 + 6 + 4 + input.length, size);

		var result = (byte[]) Serializer.deserialize(buf, TlvType.IMAGE);
		assertArrayEquals(input, result);

		buf.release();
	}

	@Test
	void Serializer_Serialize_TlvSet_GxsId()
	{
		var buf = Unpooled.buffer();
		var gxsId1 = IdFakes.createGxsId();
		var gxsId2 = IdFakes.createGxsId();
		Set<GxsId> input = new HashSet<>();
		input.add(gxsId1);
		input.add(gxsId2);

		var size = Serializer.serialize(buf, TlvType.SET_GXS_ID, input);
		assertEquals(TLV_HEADER_SIZE + GxsId.LENGTH * input.size(), size);

		@SuppressWarnings("unchecked") var result = (Set<GxsId>) Serializer.deserialize(buf, TlvType.SET_GXS_ID);
		assertEquals(2, result.size());
		assertTrue(result.contains(gxsId1));
		assertTrue(result.contains(gxsId2));

		buf.release();
	}

	@Test
	void Serializer_Serialize_TlvSet_MessageId()
	{
		var buf = Unpooled.buffer();
		var messageId1 = new MessageId(RandomUtils.nextBytes(MessageId.LENGTH));
		var messageId2 = new MessageId(RandomUtils.nextBytes(MessageId.LENGTH));
		Set<MessageId> input = new HashSet<>();
		input.add(messageId1);
		input.add(messageId2);

		var size = Serializer.serialize(buf, TlvType.SET_GXS_MSG_ID, input);
		assertEquals(TLV_HEADER_SIZE + MessageId.LENGTH * input.size(), size);

		@SuppressWarnings("unchecked") var result = (Set<MessageId>) Serializer.deserialize(buf, TlvType.SET_GXS_MSG_ID);
		assertEquals(2, result.size());
		assertTrue(result.contains(messageId1));
		assertTrue(result.contains(messageId2));

		buf.release();
	}

	@Test
	void Serializer_Serialize_TlvAddress()
	{
		var buf = Unpooled.buffer();
		var peerAddress = PeerAddress.fromAddress("192.168.1.1:1234");

		var size = Serializer.serialize(buf, TlvType.ADDRESS, peerAddress);
		assertEquals(TLV_HEADER_SIZE * 2 + 6, size);

		var result = (PeerAddress) Serializer.deserialize(buf, TlvType.ADDRESS);
		assertEquals(PeerAddress.Type.IPV4, result.getType());
		assertTrue(result.isValid());
		assertTrue(result.getAddress().isPresent());
		assertEquals("192.168.1.1:1234", result.getAddress().get());

		buf.release();
	}

	@Test
	void Serializer_Serialize_IdentityGroupItem()
	{
		var buf = Unpooled.buffer();
		var identityGroupItem = IdentityGroupItemFakes.createIdentityGroupItem();

		var size = Serializer.serializeGxsMetaAndDataItem(buf, identityGroupItem, EnumSet.noneOf(SerializationFlags.class));
		assertEquals(178, size);

		buf.release();
	}

	@Test
	void Serializer_Serialize_ForumGroupItem()
	{
		var buf = Unpooled.buffer();
		var forumGroupItem = ForumGroupItemFakes.createForumGroupItem();

		var size = Serializer.serializeGxsMetaAndDataItem(buf, forumGroupItem, EnumSet.noneOf(SerializationFlags.class));
		assertEquals(172, size);

		buf.release();
	}

	@Test
	void Serializer_Serialize_ForumMessageItem()
	{
		var buf = Unpooled.buffer();
		var forumMessageItem = ForumMessageItemFakes.createForumMessageItem();

		var size = Serializer.serializeGxsMetaAndDataItem(buf, forumMessageItem, EnumSet.noneOf(SerializationFlags.class));
		assertEquals(154, size);

		buf.release();
	}

	@Test
	void Serializer_Serialize_ComplexObject()
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

		input.setLocationId(LocationFakes.createLocation().getLocationId());

		input.setStringList(List.of("foo", "bar"));

		input.setStringMap(Map.of(1, "bleh", 2, "plop"));

		input.setSerialEnum(SerialEnum.THREE);

		input.setEnumSet(EnumSet.of(SerialEnum.ONE, SerialEnum.TWO));

		input.setEnumSetByte(EnumSet.of(SerialEnum.ONE, SerialEnum.TWO));

		input.setEnumSetShort(EnumSet.of(SerialEnum.ONE, SerialEnum.TWO));

		input.setTlvName("foobar");

		var size = Serializer.serialize(buf, input.getClass(), input, null);
		assertTrue(size > 0);

		var result = (SerialAll) Serializer.deserialize(buf, SerialAll.class);

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

		assertEquals(input.getLocationId().getLength(), result.getLocationId().getLength());
		assertArrayEquals(input.getLocationId().getBytes(), result.getLocationId().getBytes());

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
