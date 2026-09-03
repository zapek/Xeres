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
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.serialization.TlvSerializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class TlvUtilsTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvUtils.class);
	}

	@Test
	void ReadTlvSize_Valid()
	{
		var buf = Unpooled.buffer();
		buf.writeShort(TlvType.STR_NAME.getValue());
		buf.writeInt(TLV_HEADER_SIZE + 5);

		assertEquals(5, readTlvSize(buf, TlvType.STR_NAME));

		buf.release();
	}

	@Test
	void ReadTlvSize_WithIntType_Valid()
	{
		var buf = Unpooled.buffer();
		buf.writeShort(TlvType.STR_NAME.getValue());
		buf.writeInt(TLV_HEADER_SIZE + 5);

		assertEquals(5, readTlvSize(buf, TlvType.STR_NAME.getValue()));

		buf.release();
	}

	@Test
	void ReadTlvSize_WrongType_ThrowsException()
	{
		var buf = Unpooled.buffer();
		buf.writeShort(TlvType.STR_NAME.getValue());
		buf.writeInt(TLV_HEADER_SIZE + 5);

		assertThrows(IllegalArgumentException.class, () -> readTlvSize(buf, TlvType.STR_MSG));

		buf.release();
	}

	@Test
	void ReadTlvSize_TooSmall_ThrowsException()
	{
		var buf = Unpooled.buffer();
		buf.writeShort(TlvType.STR_NAME.getValue());
		buf.writeInt(2);

		assertThrows(IllegalArgumentException.class, () -> readTlvSize(buf, TlvType.STR_NAME));

		buf.release();
	}

	@Test
	void PeekTlvType_Valid()
	{
		var buf = Unpooled.buffer();
		buf.writeShort(TlvType.STR_NAME.getValue());
		buf.writeInt(TLV_HEADER_SIZE + 5);

		assertEquals(TlvType.STR_NAME, peekTlvType(buf));

		buf.release();
	}

	@Test
	void PeekTlvType_TooSmall_ReturnsNull()
	{
		var buf = Unpooled.buffer();
		buf.writeShort(TlvType.STR_NAME.getValue());

		assertNull(peekTlvType(buf));

		buf.release();
	}

	@Test
	void SkipTlv_Valid()
	{
		var buf = Unpooled.buffer();
		buf.writeShort(TlvType.STR_NAME.getValue());
		buf.writeInt(TLV_HEADER_SIZE + 5);
		buf.writeBytes(new byte[5]);

		skipTlv(buf);
		assertEquals(0, buf.readableBytes());

		buf.release();
	}

	@Test
	void SkipTlv_TooSmall_ThrowsException()
	{
		var buf = Unpooled.buffer();
		buf.writeShort(TlvType.STR_NAME.getValue());

		assertThrows(IllegalArgumentException.class, () -> skipTlv(buf));

		buf.release();
	}

	@Test
	void PrepareAndActuallyWriteTlvSize()
	{
		var buf = Unpooled.buffer();

		var offset = prepareWriteTlvSize(buf, TlvType.STR_NAME);
		var size = actuallyWriteTlvSize(buf, offset, 10);

		assertEquals(TLV_HEADER_SIZE + 10, size);
		assertEquals(TLV_HEADER_SIZE + 10, buf.getInt(2));

		buf.release();
	}
}
