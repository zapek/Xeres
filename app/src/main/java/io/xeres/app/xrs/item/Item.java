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
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.xeres.app.database.model.gxs.GxsMetaAndData;
import io.xeres.app.xrs.serialization.GxsMetaAndDataResult;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.gxs.item.DynamicServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static io.xeres.app.net.peer.packet.Packet.HEADER_SIZE;

public abstract class Item implements Cloneable
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

		// Handle items that are shared between service and hence have no intrinsic service type
		if (DynamicServiceType.class.isAssignableFrom(getClass()))
		{
			((DynamicServiceType) this).setServiceType(service.getServiceType().getType());
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
			var result = new GxsMetaAndDataResult();
			size += Serializer.serializeGxsMetaAndDataItem(buf, (GxsMetaAndData) this, flags, result);

			// RS sets this as the size for GxsMetaAndData
			setItemSize(result.getDataSize() + HEADER_SIZE);
		}
		else if (RsSerializable.class.isAssignableFrom(getClass()))
		{
			log.trace("Serializing class {} using writeObject(), flags: {}", getClass().getSimpleName(), flags);
			size += Serializer.serializeRsSerializable(buf, (RsSerializable) this, flags);
			setItemSize(size + HEADER_SIZE);
		}
		else
		{
			log.trace("Serializing class {} using annotations", getClass().getSimpleName());
			size += Serializer.serializeAnnotatedFields(buf, this);
			setItemSize(size + HEADER_SIZE);
		}
		log.debug("==> {} ({})", getClass().getSimpleName(), size + HEADER_SIZE);

		var rawItem = new RawItem(buf, getPriority());
		log.trace("Serialized buffer ==> {}", rawItem);
		if (flags.contains(SerializationFlags.SIGNATURE) || flags.contains(SerializationFlags.SIZE))
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

	/**
	 * Get the item's serialized size. This is always set for incoming items from their deserialization until they're disposed. For outgoing items, the value is undefined until the
	 * item has been serialized.
	 *
	 * @return the size of the item in its serialized form
	 */
	public int getItemSize()
	{
		return buf.getInt(4);
	}

	protected void setItemSize(int size)
	{
		buf.setInt(4, size);
	}

	/**
	 * To clone an item's subclass. Override the clone() method so that it returns the right type (so that calling clone()
	 * on the subclass, will not return the superclass' type, which is {@link #Item}). There's no need to implement the {@link Cloneable} method in the subclass and
	 * there's no need to deep copy any field either as the only use of clone() in an item's subclass is for sending it to multiple recipient
	 * and nothing will modify any mutable data.
	 *
	 * @return an Item's clone
	 */
	@Override
	public Item clone()
	{
		try
		{
			var clone = (Item) super.clone();
			clone.buf = null;
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			throw new AssertionError();
		}
	}
}
