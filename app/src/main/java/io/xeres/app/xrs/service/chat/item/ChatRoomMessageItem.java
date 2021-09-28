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
import io.xeres.app.xrs.serialization.FieldSize;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.chat.ChatFlags;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;
import static io.xeres.app.xrs.serialization.TlvType.STR_MSG;
import static io.xeres.app.xrs.service.chat.ChatFlags.LOBBY;
import static io.xeres.app.xrs.service.chat.ChatFlags.PRIVATE;

public class ChatRoomMessageItem extends ChatRoomBounce implements RsSerializable
{
	private Set<ChatFlags> flags;
	private int sendTime;
	private String message;
	private long parentMessageId;

	public ChatRoomMessageItem()
	{
		// Needed
	}

	public ChatRoomMessageItem(String message)
	{
		this.flags = EnumSet.of(LOBBY, PRIVATE);
		this.sendTime = (int) Instant.now().getEpochSecond();
		this.message = message;
		this.parentMessageId = 0L;
	}

	public Set<ChatFlags> getFlags()
	{
		return flags;
	}

	public int getSendTime()
	{
		return sendTime;
	}

	public String getMessage()
	{
		return message;
	}

	public long getParentMessageId()
	{
		return parentMessageId;
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, flags, FieldSize.INTEGER);
		size += serialize(buf, sendTime);
		size += serialize(buf, STR_MSG, message);
		size += serialize(buf, parentMessageId);

		size += writeBounceableObject(buf, serializationFlags);

		return size;
	}

	@Override
	public void readObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		flags = deserializeEnumSet(buf, ChatFlags.class, FieldSize.INTEGER);
		sendTime = deserializeInt(buf);
		message = (String) deserialize(buf, STR_MSG);
		parentMessageId = deserializeLong(buf);

		readBounceableObject(buf);
	}
}
