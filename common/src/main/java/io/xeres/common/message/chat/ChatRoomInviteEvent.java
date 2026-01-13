/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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
	private String locationIdentifier;
	private String roomName;
	private String roomTopic;

	@SuppressWarnings("unused") // Needed for JSON
	public ChatRoomInviteEvent()
	{
	}

	public ChatRoomInviteEvent(String locationIdentifier, String roomName, String roomTopic)
	{
		this.locationIdentifier = locationIdentifier;
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

	public String getLocationIdentifier()
	{
		return locationIdentifier;
	}

	public void setLocationIdentifier(String locationIdentifier)
	{
		this.locationIdentifier = locationIdentifier;
	}

	@Override
	public String toString()
	{
		return "ChatRoomInviteEvent{" +
				"locationIdentifier='" + locationIdentifier + '\'' +
				", roomName='" + roomName + '\'' +
				", roomTopic='" + roomTopic + '\'' +
				'}';
	}
}
