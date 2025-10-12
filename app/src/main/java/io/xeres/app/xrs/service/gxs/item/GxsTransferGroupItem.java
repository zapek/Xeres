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

package io.xeres.app.xrs.service.gxs.item;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.xrs.item.ItemHeader;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.GxsId;

import java.util.EnumSet;
import java.util.Set;

/**
 * This is used to transfer group data within transactions. This is usually
 * backed by a GxsGroupItem which can serialize directly when not used in transactions.
 */
public class GxsTransferGroupItem extends GxsExchange implements RsSerializable
{
	private byte position; // used for splitting up groups
	private GxsId groupId;
	private byte[] group; // actual group data; the service specific data (ie. avatar, etc...))
	private byte[] meta; // binary data for the group meta that is sent to our friends

	@SuppressWarnings("unused")
	public GxsTransferGroupItem()
	{
	}

	public GxsTransferGroupItem(GxsGroupItem gxsGroupItem, int transactionId, RsServiceType serviceType)
	{
		groupId = gxsGroupItem.getGxsId();
		setTransactionId(transactionId);
		setServiceType(serviceType.getType());

		var groupBuf = Unpooled.buffer();
		var itemHeader = new ItemHeader(groupBuf, getServiceType(), gxsGroupItem.getSubType());
		itemHeader.writeHeader();
		var groupSize = gxsGroupItem.writeDataObject(groupBuf, EnumSet.noneOf(SerializationFlags.class));
		itemHeader.writeSize(groupSize);

		var metaBuf = Unpooled.buffer();
		gxsGroupItem.writeMetaObject(metaBuf, EnumSet.noneOf(SerializationFlags.class));

		group = getArray(groupBuf);
		meta = getArray(metaBuf);

		groupBuf.release();
		metaBuf.release();
	}

	public void toGxsGroupItem(GxsGroupItem gxsGroupItem)
	{
		var buf = Unpooled.copiedBuffer(meta, group);

		gxsGroupItem.readMetaObject(buf);
		ItemHeader.readHeader(buf, getServiceType(), gxsGroupItem.getSubType());
		gxsGroupItem.readDataObject(buf);

		buf.release();
	}

	@Override
	public int getSubType()
	{
		return 4;
	}

	public byte getPosition()
	{
		return position;
	}

	public GxsId getGroupId()
	{
		return groupId;
	}

	public byte[] getGroup()
	{
		return group;
	}

	public byte[] getMeta()
	{
		return meta;
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = Serializer.serialize(buf, getTransactionId());
		size += Serializer.serialize(buf, position);
		size += Serializer.serialize(buf, groupId, GxsId.class);
		size += Serializer.serializeTlvBinary(buf, getServiceType(), group);
		size += Serializer.serializeTlvBinary(buf, getServiceType(), meta);
		return size;
	}

	@Override
	public void readObject(ByteBuf buf)
	{
		setTransactionId(Serializer.deserializeInt(buf));
		position = Serializer.deserializeByte(buf);
		groupId = (GxsId) Serializer.deserializeIdentifier(buf, GxsId.class);
		group = Serializer.deserializeTlvBinary(buf, getServiceType());
		meta = Serializer.deserializeTlvBinary(buf, getServiceType());
	}

	@Override
	public GxsTransferGroupItem clone()
	{
		return (GxsTransferGroupItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "GxsTransferGroupItem{" +
				"position=" + position +
				", groupId=" + groupId +
				'}';
	}

	private static byte[] getArray(ByteBuf buf)
	{
		var out = new byte[buf.writerIndex()];
		buf.readBytes(out);
		return out;
	}
}
