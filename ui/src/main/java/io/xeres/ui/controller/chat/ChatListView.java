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

import io.xeres.common.id.GxsId;
import io.xeres.common.message.chat.ChatRoomUserEvent;
import io.xeres.common.message.chat.RoomInfo;
import io.xeres.ui.custom.ChatListCell;
import io.xeres.ui.custom.NullSelectionModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class ChatListView
{
	private static final Logger log = LoggerFactory.getLogger(ChatListView.class);

	private final ObservableList<ChatLine> messages = FXCollections.observableArrayList();
	private final Map<GxsId, ChatRoomUser> userMap = new HashMap<>();
	private final ObservableList<ChatRoomUser> users = FXCollections.observableArrayList();

	private String nickname;
	private final RoomInfo roomInfo;

	private final ListView<ChatLine> chatView;
	private final ListView<ChatRoomUser> userListView;

	public ChatListView(String nickname, RoomInfo roomInfo)
	{
		this.nickname = nickname;
		this.roomInfo = roomInfo;

		chatView = createChatView();
		userListView = createUserListView();
	}

	private ListView<ChatLine> createChatView()
	{
		final ListView<ChatLine> view;
		view = new ListView<>();
		view.setFocusTraversable(false);
		view.getStyleClass().add("chatlist");
		VBox.setVgrow(view, Priority.ALWAYS);

		view.setCellFactory(ChatListCell::new);
		view.setItems(messages);
		view.setSelectionModel(new NullSelectionModel<>());
		return view;
	}

	private ListView<ChatRoomUser> createUserListView()
	{
		final ListView<ChatRoomUser> view;
		view = new ListView<>();
		view.getStyleClass().add("chatuserlist");
		VBox.setVgrow(view, Priority.ALWAYS);

		view.setCellFactory(ChatUserCell::new);
		view.setItems(users);
		return view;
	}

	public void addMessage(String message)
	{
		addMessage(nickname, message); // XXX: this will decode PNG images twice but well...
	}

	public void addMessage(String from, String message)
	{
		var img = Jsoup.parse(message).selectFirst("img");
		Image image = null;

		if (img != null)
		{
			var data = img.absUrl("src");
			if (isNotEmpty(data))
			{
				image = new Image(data);
			}
		}
		addMessageLine("<" + from + "> " + ((image != null && !image.isError()) ? "" : message), image);
	}

	public void addUser(ChatRoomUserEvent user)
	{
		if (!userMap.containsKey(user.getGxsId()))
		{
			var chatRoomUser = new ChatRoomUser(user.getGxsId(), user.getNickname());
			users.add(chatRoomUser);
			userMap.put(user.getGxsId(), chatRoomUser);
			users.sort((o1, o2) -> o1.nickname().compareToIgnoreCase(o2.nickname()));
			if (!nickname.equals(user.getNickname()))
			{
				addMessageLine("--> " + user.getNickname() + " (" + user.getGxsId() + ")");
			}
		}
	}

	public void removeUser(ChatRoomUserEvent user)
	{
		var chatRoomUser = userMap.remove(user.getGxsId());

		if (chatRoomUser != null)
		{
			users.remove(chatRoomUser);
			addMessageLine("<-- " + user.getNickname() + " (" + user.getGxsId() + ")");
		}
	}

	public String getUsername(String prefix, int index)
	{
		var prefixLower = prefix.toLowerCase(Locale.ENGLISH);
		if (isEmpty(prefix))
		{
			return users.get(index % users.size()).nickname();
		}
		else
		{
			var matchingUsers = users.stream()
					.filter(chatRoomUser -> !chatRoomUser.nickname().equals(nickname) && chatRoomUser.nickname().toLowerCase(Locale.ENGLISH).startsWith(prefixLower))
					.toList();

			if (matchingUsers.isEmpty())
			{
				return null;
			}
			return matchingUsers.get(index % matchingUsers.size()).nickname();
		}
	}

	public void setNickname(String nickname)
	{
		this.nickname = nickname;
	}

	public Node getChatView()
	{
		return chatView;
	}

	public Node getUserListView()
	{
		return userListView;
	}

	public RoomInfo getRoomInfo()
	{
		return roomInfo;
	}

	private void addMessageLine(ChatLine line)
	{
		messages.add(line);
		chatView.scrollTo(line);
	}

	private void addMessageLine(String line, Image image)
	{
		var chatLine = new ChatLine(line, image);
		addMessageLine(chatLine);
	}

	private void addMessageLine(String line)
	{
		addMessageLine(line, null);
	}
}
