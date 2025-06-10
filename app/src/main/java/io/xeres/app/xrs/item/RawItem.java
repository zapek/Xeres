/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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
import io.netty.util.ReferenceCountUtil;
import io.xeres.app.database.model.gxs.GxsMetaAndData;
import io.xeres.app.net.peer.packet.Packet;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.service.DefaultItem;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.xeres.app.net.peer.packet.Packet.HEADER_SIZE;

public class RawItem
{
	private static final Logger log = LoggerFactory.getLogger(RawItem.class);

	private int priority = ItemPriority.DEFAULT.getPriority();
	protected ByteBuf buf;

	public RawItem()
	{
	}

	public RawItem(Packet packet)
	{
		priority = packet.getPriority();
		buf = packet.getItemBuffer();
	}

	public RawItem(ByteBuf buf, int priority)
	{
		this.buf = buf;
		this.priority = priority;
	}

	public void deserialize(Item item)
	{
		item.setIncoming(buf);

		buf.skipBytes(HEADER_SIZE);

		if (item instanceof DefaultItem)
		{
			buf.skipBytes(getItemSize());
		}
		else if (GxsMetaAndData.class.isAssignableFrom(item.getClass()))
		{
			// This cannot be deserialized because the data is before the metadata, and the data can vary in length (optional fields at the end). It would only be possible if the data was last.
			throw new IllegalArgumentException("Cannot deserialize a GxsMetaAndData item");
		}
		else if (RsSerializable.class.isAssignableFrom(item.getClass()))
		{
			// If the object implements RsSerializable, which is more flexible, use it
			log.trace("Deserializing class {} using readObject()", item.getClass().getSimpleName());
			Serializer.deserializeRsSerializable(buf, (RsSerializable) item);
		}
		else
		{
			// Otherwise, use the more convenient @RsSerialized notations (recommended)
			log.trace("Deserializing class {} using annotations", item.getClass().getSimpleName());
			Serializer.deserializeAnnotatedFields(buf, item);
		}

		// Check if the size matches
		if (buf.readerIndex() != getItemSize())
		{
			throw new IllegalArgumentException("Size mismatch, size in header: " + getItemSize() + ", actual read size: " + buf.readerIndex() + ", (Version: " + getPacketVersion() + ", Service: " + getPacketService() + ", SubType: " + getPacketSubType() + ")");
		}
	}

	public int getPacketVersion()
	{
		return buf.getUnsignedByte(0);
	}

	public int getPacketService()
	{
		return buf.getUnsignedShort(1);
	}

	public int getPacketSubType()
	{
		return buf.getUnsignedByte(3);
	}

	private int getItemSize()
	{
		return buf.getInt(4);
	}

	public int getSize()
	{
		return getItemSize() + HEADER_SIZE;
	}

	public ByteBuf getBuffer()
	{
		return buf;
	}

	public int getPriority()
	{
		return priority;
	}

	public void dispose()
	{
		ReferenceCountUtil.release(buf);
	}

	@Override
	public String toString()
	{
		String bufOut = null;
		var size = 0;
		if (buf != null)
		{
			buf.markReaderIndex();
			buf.readerIndex(0);
			var out = new byte[buf.writerIndex()];
			size = buf.writerIndex();
			buf.readBytes(out);
			buf.resetReaderIndex();
			bufOut = new String(Hex.encode(out));
		}

		return "RawItem{" +
				"priority=" + priority +
				", buf=" + bufOut +
				", size=" + size +
				'}';
	}
}
