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
import io.xeres.ui.custom.ChatListCell;
import io.xeres.ui.custom.NullSelectionModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// XXX: custom object which contains a ListView and its messages...
public class ChatListView
{
	private final ObservableList<String> messages = FXCollections.observableArrayList();

	private String nickname;
	private final RoomInfo roomInfo;

	private final ListView<String> listView;

	public ChatListView(String nickname, RoomInfo roomInfo)
	{
		this.nickname = nickname;
		this.roomInfo = roomInfo;

		listView = new ListView<>();
		listView.setFocusTraversable(false);
		listView.getStyleClass().add("chatlist");
		VBox.setVgrow(listView, Priority.ALWAYS);

		listView.setCellFactory(ChatListCell::new);
		listView.setItems(messages);
		listView.setSelectionModel(new NullSelectionModel());
		listView.setMouseTransparent(true);
	}

	public void addMessage(String message)
	{
		messages.add("<" + nickname + "> " + message);
	}

	public void addMessage(String from, String message)
	{
		messages.add("<" + from + "> " + message);
	}

	public void setNickname(String nickname)
	{
		this.nickname = nickname;
	}

	public ListView<String> getListView()
	{
		return listView;
	}

	public RoomInfo getRoomInfo()
	{
		return roomInfo;
	}
}
