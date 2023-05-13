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

package io.xeres.app.xrs.item;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.xeres.app.database.model.gxs.GxsMetaAndData;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.gxs.item.GxsExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static io.xeres.app.net.peer.packet.Packet.HEADER_SIZE;

public abstract class Item
{
	private static final Logger log = LoggerFactory.getLogger(Item.class);

	private static final int VERSION = 2;

	protected ByteBuf buf;
	private ByteBuf backupBuf;

	public abstract int getServiceType();

	public abstract int getSubType();

	protected Item()
	{
		// Needed for instantiation
	}

	public void setIncoming(ByteBuf buf)
	{
		this.buf = buf;
	}

	public void setOutgoing(ByteBufAllocator allocator, RsService service)
	{
		buf = allocator.buffer();
		buf.writeByte(VERSION);

		// GxsExchange are shared, hence have no intrinsic service type
		if (GxsExchange.class.isAssignableFrom(getClass()))
		{
			((GxsExchange) this).setServiceType(service.getServiceType().getType());
		}
		buf.writeShort(getServiceType());
		buf.writeByte(getSubType());
		buf.writeInt(HEADER_SIZE);
	}

	public void setSerialization(ByteBufAllocator allocator, RsService service)
	{
		backupBuf = buf;
		setOutgoing(allocator, service);
	}

	public RawItem serializeItem(Set<SerializationFlags> flags)
	{
		var size = 0;

		if (GxsMetaAndData.class.isAssignableFrom(getClass()))
		{
			log.trace("Serializing class {} using GxsGroupItem system, flags: {}", getClass().getSimpleName(), flags);
			size += Serializer.serializeGxsMetaAndDataItem(buf, (GxsMetaAndData) this, getServiceType(), flags);
		}
		else if (RsSerializable.class.isAssignableFrom(getClass()))
		{
			log.trace("Serializing class {} using writeObject(), flags: {}", getClass().getSimpleName(), flags);
			size += Serializer.serializeRsSerializable(buf, (RsSerializable) this, flags);
		}
		else
		{
			log.trace("Serializing class {} using annotations", getClass().getSimpleName());
			size += Serializer.serializeAnnotatedFields(buf, this);
		}
		log.debug("==> {} ({})", getClass().getSimpleName(), size + HEADER_SIZE);
		setItemSize(size + HEADER_SIZE);

		var rawItem = new RawItem(buf, getPriority());
		if (flags.contains(SerializationFlags.SIGNATURE))
		{
			buf = backupBuf;
			backupBuf = null;
		}
		return rawItem;
	}

	public int getPriority()
	{
		return ItemPriority.DEFAULT.getPriority();
	}

	public void dispose()
	{
		if (buf != null)
		{
			assert buf.refCnt() == 1 : "buffer refCount is " + buf.refCnt();
			ReferenceCountUtil.release(buf);
		}
	}

	protected void setItemSize(int size)
	{
		buf.setInt(4, size);
	}
}
