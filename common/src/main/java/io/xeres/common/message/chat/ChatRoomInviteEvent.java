/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.common.message.chat;

public class ChatRoomInviteEvent
{
	private String locationId;
	private String roomName;
	private String roomTopic;

	public ChatRoomInviteEvent()
	{
		// Needed for JSON
	}

	public ChatRoomInviteEvent(String locationId, String roomName, String roomTopic)
	{
		this.locationId = locationId;
		this.roomName = roomName;
		this.roomTopic = roomTopic;
	}

	public String getRoomName()
	{
		return roomName;
	}

	public void setRoomName(String roomName)
	{
		this.roomName = roomName;
	}

	public String getRoomTopic()
	{
		return roomTopic;
	}

	public void setRoomTopic(String roomTopic)
	{
		this.roomTopic = roomTopic;
	}

	public String getLocationId()
	{
		return locationId;
	}

	public void setLocationId(String locationId)
	{
		this.locationId = locationId;
	}

	@Override
	public String toString()
	{
		return "ChatRoomInviteEvent{" +
				"locationId='" + locationId + '\'' +
				", roomName='" + roomName + '\'' +
				", roomTopic='" + roomTopic + '\'' +
				'}';
	}
}
