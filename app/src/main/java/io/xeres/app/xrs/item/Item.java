/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static io.xeres.app.net.peer.packet.Packet.HEADER_SIZE;

public class Item
{
	private static final Logger log = LoggerFactory.getLogger(Item.class);

	protected ByteBuf buf;
	private ByteBuf backupBuf;
	private RsService service;

	public Item()
	{
		// Needed for instantiation
	}

	public void setIncoming(ByteBuf buf)
	{
		this.buf = buf;
	}

	public void setOutgoing(ByteBufAllocator allocator, int version, RsServiceType service, int subType)
	{
		buf = allocator.buffer();
		buf.writeByte(version);
		buf.writeShort(service.getType());
		buf.writeByte(subType);
		buf.writeInt(HEADER_SIZE);
	}

	public void setSerialization(ByteBufAllocator allocator, int version, RsServiceType service, int subType)
	{
		backupBuf = buf;
		setOutgoing(allocator, version, service, subType);
	}

	public RawItem serializeItem(Set<SerializationFlags> flags)
	{
		var size = 0;

		if (RsSerializable.class.isAssignableFrom(getClass()))
		{
			log.trace("Serializing class {} using writeObject(), flags: {}", getClass().getSimpleName(), flags);
			size = ((RsSerializable) this).writeObject(buf, flags);
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
		assert buf.refCnt() == 1;
		ReferenceCountUtil.release(buf);
	}

	protected void setItemSize(int size)
	{
		buf.setInt(4, size);
	}

	public RsService getService()
	{
		return service;
	}

	public void setService(RsService service)
	{
		this.service = service;
	}
}
