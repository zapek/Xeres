/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.serialization.Serializer;

public class ItemHeader
{
	private final ByteBuf buf;
	private final int serviceType;
	private final int subType;
	private int size;
	private int sizeOffset;

	public ItemHeader(ByteBuf buf, int serviceType, int subType)
	{
		this.buf = buf;
		this.serviceType = serviceType;
		this.subType = subType;
	}

	public int writeHeader()
	{
		size = Serializer.serialize(buf, (byte) 2);
		size += Serializer.serialize(buf, (short) serviceType);
		size += Serializer.serialize(buf, (byte) subType);
		sizeOffset = buf.writerIndex();
		size += Serializer.serialize(buf, 0); // the size is written at the end when calling writeSize()
		return size;
	}

	public int writeSize(int dataSize)
	{
		size += dataSize;
		buf.setInt(sizeOffset, size);
		return size;
	}

	public static void readHeader(ByteBuf buf, int serviceType, int subType)
	{
		if (buf.readByte() != 2)
		{
			throw new IllegalArgumentException("Packet version is not 0x2");
		}
		if (buf.readUnsignedShort() != serviceType)
		{
			throw new IllegalArgumentException("Packet type is not " + serviceType);
		}
		if (buf.readUnsignedByte() != subType)
		{
			throw new IllegalArgumentException("Packet subtype is not " + subType);
		}
		buf.readInt(); // size
	}

	public static int getSubType(ByteBuf buf)
	{
		return buf.getUnsignedByte(3);
	}
}
