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

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.chat.RoomFlags;
import io.xeres.common.id.Id;

import java.util.Set;

import static io.xeres.app.xrs.serialization.TlvType.STR_NAME;
import static io.xeres.app.xrs.service.chat.RoomFlags.*;

public class ChatRoomInviteItem extends Item
{
	@RsSerialized
	private long roomId;

	@RsSerialized(tlvType = STR_NAME)
	private String roomName;

	@RsSerialized(tlvType = STR_NAME)
	private String roomTopic;

	@RsSerialized
	private Set<RoomFlags> roomFlags;

	@Override
	public int getServiceType()
	{
		return RsServiceType.CHAT.getType();
	}

	@Override
	public int getSubType()
	{
		return 27;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.INTERACTIVE.getPriority();
	}

	@SuppressWarnings("unused")
	public ChatRoomInviteItem()
	{
	}

	public ChatRoomInviteItem(long roomId, String roomName, String roomTopic, Set<RoomFlags> roomFlags)
	{
		this.roomId = roomId;
		this.roomName = roomName;
		this.roomTopic = roomTopic;
		this.roomFlags = roomFlags;
	}

	public long getRoomId()
	{
		return roomId;
	}

	public String getRoomName()
	{
		return roomName;
	}

	public String getRoomTopic()
	{
		return roomTopic;
	}

	public Set<RoomFlags> getRoomFlags()
	{
		return roomFlags;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean isConnectionChallenge()
	{
		return roomFlags.contains(CHALLENGE);
	}

	public boolean isPublic()
	{
		return roomFlags.contains(PUBLIC);
	}

	public boolean isSigned()
	{
		return roomFlags.contains(PGP_SIGNED);
	}

	@Override
	public String toString()
	{
		return "ChatRoomInviteItem{" +
				"roomId=" + Id.toString(roomId) +
				", roomName='" + roomName + '\'' +
				", roomTopic='" + roomTopic + '\'' +
				", roomFlags=" + roomFlags +
				'}';
	}
}
