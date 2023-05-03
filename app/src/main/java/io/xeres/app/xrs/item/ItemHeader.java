package io.xeres.app.xrs.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.serialization.Serializer;

public class ItemHeader
{
	private final ByteBuf buf;
	private final int serviceType;
	private int size;
	private int sizeOffset;

	public ItemHeader(ByteBuf buf, int serviceType)
	{
		this.buf = buf;
		this.serviceType = serviceType;
	}

	public void writeHeader()
	{
		size += Serializer.serialize(buf, (byte) 2);
		size += Serializer.serialize(buf, (short) serviceType);
		size += Serializer.serialize(buf, (byte) 2); // XXX: 2 or 3? see readFakeHeader(), but maybe that's different
		sizeOffset = buf.writerIndex();
		size += Serializer.serialize(buf, 0); // the size is written at the end when calling writeSize()
	}

	public void writeSize(int dataSize)
	{
		size += dataSize;
		buf.setInt(sizeOffset, size);
	}
}
