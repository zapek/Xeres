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

package io.xeres.ui.controller.chat;

import io.xeres.common.message.chat.RoomInfo;

public class RoomHolder
{
	private ChatListView chatListView;
	private final RoomInfo roomInfo;

	public RoomHolder(ChatListView chatListView, RoomInfo roomInfo)
	{
		this.chatListView = chatListView;
		this.roomInfo = roomInfo;
	}

	public RoomHolder(String name)
	{
		this.roomInfo = new RoomInfo(name);
	}

	public RoomHolder(RoomInfo roomInfo)
	{
		this.roomInfo = roomInfo;
	}

	public RoomHolder()
	{
		this.roomInfo = new RoomInfo("");
	}

	public void setChatListView(ChatListView chatListView)
	{
		this.chatListView = chatListView;
	}

	public void clearChatListView()
	{
		this.chatListView = null;
	}

	public ChatListView getChatListView()
	{
		return chatListView;
	}

	public RoomInfo getRoomInfo()
	{
		return roomInfo;
	}

	@Override
	public String toString()
	{
		return roomInfo.getName();
	}
}
