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
import io.netty.buffer.Unpooled;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.xrs.item.ItemHeader;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;

import java.util.EnumSet;
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

	public GxsTransferMessageItem(GxsMessageItem gxsMessageItem, int transactionId, RsServiceType serviceType)
	{
		groupId = gxsMessageItem.getGxsId();
		messageId = gxsMessageItem.getMessageId();
		setTransactionId(transactionId);
		setServiceType(serviceType.getType());

		// XXX: sign the message. add the signature stuff to GxsMessageItem
		var messageBuf = Unpooled.buffer();
		var itemHeader = new ItemHeader(messageBuf, getServiceType(), gxsMessageItem.getSubType());
		itemHeader.writeHeader();
		var messageSize = gxsMessageItem.writeDataObject(messageBuf, EnumSet.noneOf(SerializationFlags.class));
		itemHeader.writeSize(messageSize);

		var metaBuf = Unpooled.buffer();
		gxsMessageItem.writeMetaObject(metaBuf, EnumSet.noneOf(SerializationFlags.class));

		message = getArray(messageBuf);
		meta = getArray(metaBuf);

		messageBuf.release();
		metaBuf.release();
	}

	public void toGxsMessageItem(GxsMessageItem gxsMessageItem)
	{
		var buf = Unpooled.copiedBuffer(meta, message);

		gxsMessageItem.readMetaObject(buf);
		ItemHeader.readHeader(buf, getServiceType(), gxsMessageItem.getSubType());
		gxsMessageItem.readDataObject(buf);

		buf.release();
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

	private static byte[] getArray(ByteBuf buf)
	{
		var out = new byte[buf.writerIndex()];
		buf.readBytes(out);
		return out;
	}
}
