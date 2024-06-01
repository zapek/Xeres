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

package io.xeres.ui.controller.chat;

import io.xeres.common.message.chat.ChatRoomInfo;

public class RoomHolder
{
	private ChatListView chatListView;
	private final ChatRoomInfo chatRoomInfo;

	public RoomHolder(ChatListView chatListView, ChatRoomInfo chatRoomInfo)
	{
		this.chatListView = chatListView;
		this.chatRoomInfo = chatRoomInfo;
	}

	public RoomHolder(String name)
	{
		chatRoomInfo = new ChatRoomInfo(name);
	}

	public RoomHolder(ChatRoomInfo chatRoomInfo)
	{
		this.chatRoomInfo = chatRoomInfo;
	}

	public RoomHolder()
	{
		chatRoomInfo = new ChatRoomInfo("");
	}

	public void setChatListView(ChatListView chatListView)
	{
		this.chatListView = chatListView;
	}

	public void clearChatListView()
	{
		chatListView = null;
	}

	public ChatListView getChatListView()
	{
		return chatListView;
	}

	public ChatRoomInfo getRoomInfo()
	{
		return chatRoomInfo;
	}

	@Override
	public String toString()
	{
		return chatRoomInfo.getName();
	}
}
