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

import java.util.ArrayList;
import java.util.List;

public class ChatRoomLists
{
	private List<ChatRoomInfo> subscribedRooms = new ArrayList<>();
	private List<ChatRoomInfo> availableRooms = new ArrayList<>();

	public void addSubscribed(ChatRoomInfo chatRoomInfo)
	{
		subscribedRooms.add(chatRoomInfo);
	}

	public void addAvailable(ChatRoomInfo chatRoomInfo)
	{
		availableRooms.add(chatRoomInfo);
	}

	@SuppressWarnings("unused") // Needed for JSON serialization
	public void setSubscribedRooms(List<ChatRoomInfo> subscribedRooms)
	{
		this.subscribedRooms = subscribedRooms;
	}

	@SuppressWarnings("unused") // Needed for JSON serialization
	public void setAvailableRooms(List<ChatRoomInfo> availableRooms)
	{
		this.availableRooms = availableRooms;
	}

	public List<ChatRoomInfo> getSubscribedRooms()
	{
		return subscribedRooms;
	}

	public List<ChatRoomInfo> getAvailableRooms()
	{
		return availableRooms;
	}
}
