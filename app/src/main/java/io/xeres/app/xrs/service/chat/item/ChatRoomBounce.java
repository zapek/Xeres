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

package io.xeres.app.xrs.service.chat.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.common.Signature;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.common.id.Id;

import java.util.Set;

import static io.xeres.app.xrs.serialization.TlvType.SIGNATURE;
import static io.xeres.app.xrs.serialization.TlvType.STR_NAME;

public abstract class ChatRoomBounce extends Item
{
	private long roomId;
	private long messageId;
	private String senderNickname;
	private Signature signature;

	protected ChatRoomBounce()
	{
		// Needed
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.CHAT.getPriority();
	}

	int writeBounceableObject(ByteBuf buf, Set<SerializationFlags> flags)
	{
		var size = 0;

		size += Serializer.serialize(buf, roomId);
		size += Serializer.serialize(buf, messageId);
		size += Serializer.serialize(buf, STR_NAME, senderNickname);

		if (!flags.contains(SerializationFlags.SIGNATURE))
		{
			size += Serializer.serialize(buf, SIGNATURE, signature);
		}
		return size;
	}

	void readBounceableObject(ByteBuf buf)
	{
		roomId = Serializer.deserializeLong(buf);
		messageId = Serializer.deserializeLong(buf);
		senderNickname = (String) Serializer.deserialize(buf, STR_NAME);
		signature = (Signature) Serializer.deserialize(buf, SIGNATURE);
	}

	public long getRoomId()
	{
		return roomId;
	}

	public void setRoomId(long roomId)
	{
		this.roomId = roomId;
	}

	public long getMessageId()
	{
		return messageId;
	}

	public void setMessageId(long messageId)
	{
		this.messageId = messageId;
	}

	public String getSenderNickname()
	{
		return senderNickname;
	}

	public void setSenderNickname(String senderNickname)
	{
		this.senderNickname = senderNickname;
	}

	public Signature getSignature()
	{
		return signature;
	}

	public void setSignature(Signature signature)
	{
		this.signature = signature;
	}

	@Override
	public String toString()
	{
		return "ChatRoomBounce{" +
				"roomId=" + Id.toString(roomId) +
				", messageId=" + Id.toString(messageId) +
				", senderNickname='" + senderNickname + '\'' +
				", signature=[something]" +
				'}';
	}
}
