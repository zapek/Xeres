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

	public void writeHeader()
	{
		size += Serializer.serialize(buf, (byte) 2);
		size += Serializer.serialize(buf, (short) serviceType);
		size += Serializer.serialize(buf, (byte) subType);
		sizeOffset = buf.writerIndex();
		size += Serializer.serialize(buf, 0); // the size is written at the end when calling writeSize()
	}

	public void writeSize(int dataSize)
	{
		size += dataSize;
		buf.setInt(sizeOffset, size);
	}

	public static void readHeader(ByteBuf buf, int serviceType, int subType)
	{
		if (buf.readByte() != 2)
		{
			throw new IllegalArgumentException("Packet version is not 0x2");
		}
		if (buf.readShort() != serviceType)
		{
			throw new IllegalArgumentException("Packet type is not " + serviceType);
		}
		if (buf.readByte() != subType)
		{
			throw new IllegalArgumentException("Packet subtype is not " + subType);
		}
		buf.readInt(); // size
	}
}
