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

package io.xeres.app.xrs.service.chat.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.chat.RoomFlags;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;

import java.util.Map;
import java.util.Set;

public class SubscribedChatRoomConfigItem extends Item
{
	@RsSerialized
	private long roomId;

	@RsSerialized
	private String roomName;

	@RsSerialized
	private String roomTopic;

	@RsSerialized
	private Set<LocationIdentifier> participatingLocations; // XXX: do we serialize Sets yet? no, see #19

	@RsSerialized
	private GxsId gxsId;

	@RsSerialized
	private Set<RoomFlags> flags;

	@RsSerialized
	private Map<GxsId, Long> gxsIds;

	@RsSerialized
	private long lastActivity;

	@Override
	public int getServiceType()
	{
		return RsServiceType.CHAT.getType();
	}

	@Override
	public int getSubType()
	{
		return 29;
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

	public Set<LocationIdentifier> getParticipatingLocations()
	{
		return participatingLocations;
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public Set<RoomFlags> getFlags()
	{
		return flags;
	}

	public Map<GxsId, Long> getGxsIds()
	{
		return gxsIds;
	}

	public long getLastActivity()
	{
		return lastActivity;
	}

	@Override
	public SubscribedChatRoomConfigItem clone()
	{
		return (SubscribedChatRoomConfigItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "SubscribedChatRoomConfigItem{" +
				"roomId=" + roomId +
				", roomName='" + roomName + '\'' +
				", roomTopic='" + roomTopic + '\'' +
				", participatingLocations=" + participatingLocations +
				", gxsId=" + gxsId +
				", flags=" + flags +
				", gxsIds=" + gxsIds +
				", lastActivity=" + lastActivity +
				'}';
	}
}
