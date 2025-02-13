/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceRegistry;

import java.util.EnumSet;

public final class ItemUtils
{
	private ItemUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Serializes an item to get its serialized size.
	 * @param item    the item
	 * @param service the service
	 * @return the total serialized size in bytes
	 */
	public static int getItemSerializedSize(Item item, RsService service)
	{
		item.setSerialization(Unpooled.buffer().alloc(), service);
		var rawItem = item.serializeItem(EnumSet.of(SerializationFlags.SIZE));
		var size = rawItem.getSize();
		rawItem.getBuffer().release();
		return size;
	}

	/**
	 * Serializes an item to make a signature out of it.
	 * @param item    the item
	 * @param service the service
	 * @return a byte array
	 */
	public static byte[] serializeItemForSignature(Item item, RsService service)
	{
		item.setSerialization(Unpooled.buffer().alloc(), service);
		var buf = item.serializeItem(EnumSet.of(SerializationFlags.SIGNATURE)).getBuffer();
		var data = new byte[buf.writerIndex()];
		buf.getBytes(0, data);
		buf.release();
		return data;
	}

	/**
	 * Serializes an item. Do not use this within a netty pipeline.
	 *
	 * @param item    the item
	 * @param service the service
	 * @return a byte array
	 */
	public static byte[] serializeItem(Item item, RsService service)
	{
		item.setSerialization(Unpooled.buffer().alloc(), service);
		var buf = item.serializeItem(EnumSet.noneOf(SerializationFlags.class)).getBuffer();
		var data = new byte[buf.writerIndex()];
		buf.getBytes(0, data);
		buf.release();
		return data;
	}

	/**
	 * Deserializes an item. Do not use this within a netty pipeline.
	 *
	 * @param data     the byte array of the item
	 * @param registry the registry to build the item
	 * @return the item, not null
	 */
	public static Item deserializeItem(byte[] data, RsServiceRegistry registry)
	{
		var rawItem = new RawItem(Unpooled.wrappedBuffer(data), ItemPriority.DEFAULT.getPriority());
		var item = registry.buildIncomingItem(rawItem);
		rawItem.deserialize(item);
		rawItem.dispose();
		return item;
	}
}
