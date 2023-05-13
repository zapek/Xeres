/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;

import java.util.Set;

public class GxsTransferMessageItem extends GxsExchange implements RsSerializable
{
	private byte position;
	private GxsId groupId;
	private MessageId messageId;
	private byte[] message;
	private byte[] meta;

	@SuppressWarnings("unused")
	public GxsTransferMessageItem()
	{
	}

	public GxsTransferMessageItem(GxsId groupId, MessageId messageId, byte[] message, byte[] meta, int transactionId, int serviceType)
	{
		this.groupId = groupId;
		this.messageId = messageId;
		this.message = message;
		this.meta = meta;
		setTransactionId(transactionId);
		setServiceType(serviceType);
	}

	@Override
	public int getSubType()
	{
		return 32;
	}

	public byte getPosition()
	{
		return position;
	}

	public GxsId getGroupId()
	{
		return groupId;
	}

	public MessageId getMessageId()
	{
		return messageId;
	}

	public byte[] getMessage()
	{
		return message;
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
		size += Serializer.serialize(buf, messageId, MessageId.class);
		size += Serializer.serialize(buf, groupId, GxsId.class);
		size += Serializer.serializeTlvBinary(buf, getServiceType(), message);
		size += Serializer.serializeTlvBinary(buf, getServiceType(), meta);
		return size;
	}

	@Override
	public void readObject(ByteBuf buf)
	{
		setTransactionId(Serializer.deserializeInt(buf));
		position = Serializer.deserializeByte(buf);
		messageId = (MessageId) Serializer.deserializeIdentifier(buf, MessageId.class);
		groupId = (GxsId) Serializer.deserializeIdentifier(buf, GxsId.class);
		message = Serializer.deserializeTlvBinary(buf, getServiceType());
		meta = Serializer.deserializeTlvBinary(buf, getServiceType());
	}

	@Override
	public String toString()
	{
		return "GxsTransferMessageItem{" +
				"position=" + position +
				", groupId=" + groupId +
				", messageId=" + messageId +
				'}';
	}
}
