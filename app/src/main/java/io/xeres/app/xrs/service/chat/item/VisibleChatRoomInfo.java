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

package io.xeres.app.xrs.service.chat.item;

import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.chat.RoomFlags;
import io.xeres.common.id.Id;

import java.util.Set;

import static io.xeres.app.xrs.serialization.TlvType.STR_NAME;

public class VisibleChatRoomInfo
{
	@RsSerialized
	private long id;

	@RsSerialized(tlvType = STR_NAME)
	private String name;

	@RsSerialized(tlvType = STR_NAME)
	private String topic;

	@RsSerialized
	private int count;

	@RsSerialized
	private Set<RoomFlags> flags;

	public VisibleChatRoomInfo()
	{
		// Required
	}

	public VisibleChatRoomInfo(long id, String name, String topic, int count, Set<RoomFlags> roomFlags)
	{
		this.id = id;
		this.name = name;
		this.topic = topic;
		this.count = count;
		this.flags = roomFlags;
	}

	public long getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public String getTopic()
	{
		return topic;
	}

	public int getCount()
	{
		return count;
	}

	public Set<RoomFlags> getFlags()
	{
		return flags;
	}

	@Override
	public String toString()
	{
		return "VisibleChatRoomInfo{" +
				"id=" + Id.toStringLowerCase(id) +
				", name='" + name + '\'' +
				", topic='" + topic + '\'' +
				", count=" + count +
				", flags=" + flags +
				'}';
	}
}
