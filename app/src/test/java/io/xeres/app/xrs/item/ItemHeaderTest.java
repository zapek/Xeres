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

package io.xeres.app.xrs.item;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemHeaderTest
{
	@Test
	void ReadHeader_Success()
	{
		var buf = Unpooled.wrappedBuffer(new byte[]{2, 8, 8, 3, 0, 0, 0, 1});

		assertDoesNotThrow(() -> ItemHeader.readHeader(buf, 0x808, 3));
	}

	@Test
	void ReadHeader_WrongVersion()
	{
		var buf = Unpooled.wrappedBuffer(new byte[]{1, 8, 8, 3, 0, 0, 0, 1});

		assertThrows(IllegalArgumentException.class,
				() -> ItemHeader.readHeader(buf, 0x808, 3),
				"Packet version is not 0x2");
	}

	@Test
	void ReadHeader_WrongType()
	{
		var buf = Unpooled.wrappedBuffer(new byte[]{2, 8, 8, 3, 0, 0, 0, 1});

		assertThrows(IllegalArgumentException.class,
				() -> ItemHeader.readHeader(buf, 0x807, 3),
				"Packet type is not 2055");
	}

	@Test
	void ReadHeader_WrongSubtype()
	{
		var buf = Unpooled.wrappedBuffer(new byte[]{2, 8, 8, 3, 0, 0, 0, 1});

		assertThrows(IllegalArgumentException.class,
				() -> ItemHeader.readHeader(buf, 0x808, 4),
				"Packet subtype is not 4");
	}

	@Test
	void Constructor_ShouldAcceptValidParameters()
	{
		var buf = Unpooled.buffer();

		assertDoesNotThrow(() -> new ItemHeader(buf, 0x808, 3));
	}

	@Test
	void WriteHeader_ShouldWriteEightBytesToBuffer()
	{
		var buf = Unpooled.buffer();
		var header = new ItemHeader(buf, 0x808, 3);

		header.writeHeader();

		assertEquals(8, buf.writerIndex());
	}

	@Test
	void WriteHeader_ShouldWriteCorrectHeaderBytes()
	{
		var buf = Unpooled.buffer();
		var header = new ItemHeader(buf, 0x808, 3);

		header.writeHeader();

		assertEquals(2, buf.getByte(0)); // version
		assertEquals(0x0808, buf.getUnsignedShort(1)); // serviceType
		assertEquals(3, buf.getUnsignedByte(3)); // subType
		assertEquals(0, buf.getInt(4)); // size placeholder
	}

	@Test
	void WriteSize_ShouldWriteCorrectTotalSize()
	{
		var buf = Unpooled.buffer();
		var header = new ItemHeader(buf, 0x808, 3);

		header.writeHeader();
		buf.writeZero(12); // simulate data
		header.writeSize(12);

		assertEquals(20, buf.getInt(4)); // 8 (header) + 12 (data)
	}

	@Test
	void GetSubType_ShouldReturnCorrectSubType()
	{
		var buf = Unpooled.wrappedBuffer(new byte[]{2, 8, 8, 5, 0, 0, 0, 1});

		assertEquals(5, ItemHeader.getSubType(buf));
	}
}
