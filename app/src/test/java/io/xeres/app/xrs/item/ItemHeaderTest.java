package io.xeres.app.xrs.item;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
