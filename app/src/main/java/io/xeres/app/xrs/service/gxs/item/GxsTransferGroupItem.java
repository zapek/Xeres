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

package io.xeres.app.xrs.service.gxs.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.service.RsService;
import io.xeres.common.id.GxsId;

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

	public GxsTransferGroupItem()
	{
		// Needed
	}

	public GxsTransferGroupItem(GxsId groupId, byte[] group, byte[] meta, int transactionId, RsService service)
	{
		this.groupId = groupId;
		this.group = group;
		this.meta = meta;
		setTransactionId(transactionId);
		setService(service);
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
		size += Serializer.serializeTlvBinary(buf, getService().getServiceType().getType(), group);
		size += Serializer.serializeTlvBinary(buf, getService().getServiceType().getType(), meta);
		return size;
	}

	@Override
	public void readObject(ByteBuf buf)
	{
		setTransactionId(Serializer.deserializeInt(buf));
		position = Serializer.deserializeByte(buf);
		groupId = (GxsId) Serializer.deserializeIdentifier(buf, GxsId.class);
		group = Serializer.deserializeTlvBinary(buf, getService().getServiceType().getType());
		meta = Serializer.deserializeTlvBinary(buf, getService().getServiceType().getType());
	}

	@Override
	public String toString()
	{
		return "GxsTransferGroupItem{" +
				"position=" + position +
				", groupId=" + groupId +
				'}';
	}
}
