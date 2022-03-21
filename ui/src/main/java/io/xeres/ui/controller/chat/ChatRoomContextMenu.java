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

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.control.TreeItem;

public class ChatRoomContextMenu extends Event
{
	public static final EventType<ChatRoomContextMenu> ALL = new EventType<>("CHAT_ROOM_CONTEXT_MENU_ALL");
	public static final EventType<ChatRoomContextMenu> JOIN = new EventType<>(ALL, "CHAT_ROOM_CONTEXT_MENU_JOIN");
	public static final EventType<ChatRoomContextMenu> LEAVE = new EventType<>(ALL, "CHAT_ROOM_CONTEXT_MENU_LEAVE");

	private final transient TreeItem<RoomHolder> treeItem;

	public ChatRoomContextMenu(EventType<ChatRoomContextMenu> eventType, TreeItem<RoomHolder> treeItem)
	{
		super(eventType);
		this.treeItem = treeItem;
	}

	public TreeItem<RoomHolder> getTreeItem()
	{
		return treeItem;
	}
}
