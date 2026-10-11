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
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static io.xeres.app.xrs.serialization.TlvSerializer.TLV_HEADER_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SerializerUtilsTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(SerializerUtils.class);
	}

	@Test
	void ReadLength_Valid()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(3);
			buf.writeBytes(new byte[]{1, 2, 3});
			assertEquals(3, SerializerUtils.readLength(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void ReadLength_ZeroLength_Ok()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(0);
			assertEquals(0, SerializerUtils.readLength(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void ReadLength_Negative_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(-1);
			assertThrows(IllegalArgumentException.class, () -> SerializerUtils.readLength(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void ReadLength_ExceedsReadableBytes_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(10);
			buf.writeBytes(new byte[]{1, 2});
			assertThrows(IllegalArgumentException.class, () -> SerializerUtils.readLength(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void ReadEntriesCount_Valid()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(2);
			buf.writeZero(2);
			assertEquals(2, SerializerUtils.readEntriesCount(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void ReadEntriesCount_Negative_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(-1);
			assertThrows(IllegalArgumentException.class, () -> SerializerUtils.readEntriesCount(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void ReadEntriesCount_ExceedsReadableBytes_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(100);
			assertThrows(IllegalArgumentException.class, () -> SerializerUtils.readEntriesCount(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void ReadEntriesCount_AboveMax_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			var count = 20000;
			buf.writeInt(count);
			buf.writeZero(count);
			assertThrows(IllegalArgumentException.class, () -> SerializerUtils.readEntriesCount(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void GetElementsCount_Valid()
	{
		assertEquals(2, SerializerUtils.getElementsCount(16, Long.BYTES));
		assertEquals(0, SerializerUtils.getElementsCount(0, Long.BYTES));
	}

	@Test
	void GetElementsCount_Remainder_Throws()
	{
		assertThrows(IllegalArgumentException.class, () -> SerializerUtils.getElementsCount(7, Long.BYTES));
		assertThrows(IllegalArgumentException.class, () -> SerializerUtils.getElementsCount(10, Long.BYTES));
	}

	@Test
	void StringDeserialize_NegativeLength_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(-1);
			assertThrows(IllegalArgumentException.class, () -> StringSerializer.deserialize(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void StringDeserialize_TruncatedLength_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(10);
			buf.writeBytes(new byte[]{1, 2});
			assertThrows(IllegalArgumentException.class, () -> StringSerializer.deserialize(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void StringDeserialize_RoundTrip_StillWorks()
	{
		var buf = Unpooled.buffer();
		try
		{
			StringSerializer.serialize(buf, "hello");
			assertEquals("hello", StringSerializer.deserialize(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void ByteArrayDeserialize_InvalidLength_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(-5);
			assertThrows(IllegalArgumentException.class, () -> ByteArraySerializer.deserialize(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void ByteArrayDeserialize_Truncated_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(8);
			buf.writeBytes(new byte[]{1, 2, 3});
			assertThrows(IllegalArgumentException.class, () -> ByteArraySerializer.deserialize(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void BigIntegerDeserialize_InvalidLength_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(Integer.MAX_VALUE);
			assertThrows(IllegalArgumentException.class, () -> BigIntegerSerializer.deserialize(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void ListDeserialize_NegativeCount_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(-1);
			assertThrows(IllegalArgumentException.class, () -> ListSerializer.deserialize(buf, new ArrayList<String>(), stringListType()));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void ListDeserialize_HugeCount_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(Integer.MAX_VALUE);
			assertThrows(IllegalArgumentException.class, () -> ListSerializer.deserialize(buf, new ArrayList<String>(), stringListType()));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void MapDeserialize_NegativeCount_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(-2);
			assertThrows(IllegalArgumentException.class, () -> MapSerializer.deserialize(buf, new HashMap<Integer, String>(), intStringMapType()));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void TlvListDeserialize_NegativeCount_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeInt(-1);
			assertThrows(IllegalArgumentException.class, () -> TlvListSerializer.deserialize(buf, TlvType.STR_NAME, new ArrayList<String>()));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void TlvSetDeserializeLong_UnalignedLength_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeShort(TlvType.SET_PGP_ID.getValue());
			buf.writeInt(TLV_HEADER_SIZE + 7);
			buf.writeBytes(new byte[7]);
			assertThrows(IllegalArgumentException.class, () -> TlvSetSerializer.deserializeLong(buf, TlvType.SET_PGP_ID));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void TlvSetDeserializeLong_RoundTrip_StillWorks()
	{
		var buf = Unpooled.buffer();
		try
		{
			var input = Set.of(1L, 2L);
			TlvSetSerializer.serializeLong(buf, TlvType.SET_PGP_ID, new java.util.HashSet<>(input));
			assertEquals(input, TlvSetSerializer.deserializeLong(buf, TlvType.SET_PGP_ID));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void TlvReadSize_BogusLength_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeShort(TlvType.STR_NAME.getValue());
			buf.writeInt(Integer.MAX_VALUE);
			assertThrows(IllegalArgumentException.class, () -> TlvUtils.readTlvSize(buf, TlvType.STR_NAME));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void TlvSkip_BogusLength_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeShort(TlvType.STR_NAME.getValue());
			buf.writeInt(Integer.MAX_VALUE);
			assertThrows(IllegalArgumentException.class, () -> TlvUtils.skipTlv(buf));
		}
		finally
		{
			buf.release();
		}
	}

	@Test
	void TlvSkip_TruncatedPayload_Throws()
	{
		var buf = Unpooled.buffer();
		try
		{
			buf.writeShort(TlvType.STR_NAME.getValue());
			buf.writeInt(TLV_HEADER_SIZE + 100);
			buf.writeBytes(new byte[10]);
			assertThrows(IllegalArgumentException.class, () -> TlvUtils.skipTlv(buf));
		}
		finally
		{
			buf.release();
		}
	}

	private static ParameterizedType stringListType()
	{
		return new ParameterizedType()
		{
			@Override
			public Type @NonNull [] getActualTypeArguments()
			{
				return new Type[]{String.class};
			}

			@Override
			public @NonNull Type getRawType()
			{
				return List.class;
			}

			@Override
			public Type getOwnerType()
			{
				return null;
			}
		};
	}

	private static ParameterizedType intStringMapType()
	{
		return new ParameterizedType()
		{
			@Override
			public Type @NonNull [] getActualTypeArguments()
			{
				return new Type[]{Integer.class, String.class};
			}

			@Override
			public @NonNull Type getRawType()
			{
				return Map.class;
			}

			@Override
			public Type getOwnerType()
			{
				return null;
			}
		};
	}
}
