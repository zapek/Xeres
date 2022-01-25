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
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;

import java.time.Instant;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;
import static io.xeres.app.xrs.serialization.TlvType.STR_NAME;

public class ChatRoomEventItem extends ChatRoomBounce implements RsSerializable, Cloneable
{
	private byte eventType;
	private String status;
	private int sendTime;

	public ChatRoomEventItem()
	{
		// Required
	}

	public ChatRoomEventItem(ChatRoomEvent event, String status)
	{
		this.eventType = event.getCode();
		this.status = status;
		this.sendTime = (int) Instant.now().getEpochSecond();
	}

	public byte getEventType()
	{
		return eventType;
	}

	public String getStatus()
	{
		return status;
	}

	public int getSendTime()
	{
		return sendTime;
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, eventType);
		size += serialize(buf, STR_NAME, status);
		size += serialize(buf, sendTime);

		size += writeBounceableObject(buf, serializationFlags);

		return size;
	}

	@Override
	public void readObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		eventType = deserializeByte(buf);
		status = (String) deserialize(buf, STR_NAME);
		sendTime = deserializeInt(buf);

		readBounceableObject(buf);
	}

	@Override
	public String toString()
	{
		return "ChatRoomEventItem{" +
				"eventType=" + eventType +
				", status='" + status + '\'' +
				", sendTime=" + sendTime +
				", super=" + super.toString() +
				'}';
	}

	@Override
	public ChatRoomEventItem clone()
	{
		return (ChatRoomEventItem) super.clone();
	}
}
