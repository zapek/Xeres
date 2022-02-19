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
import io.xeres.common.message.chat.ChatRoomTimeoutEvent;
import io.xeres.common.message.chat.ChatRoomUserEvent;
import io.xeres.ui.custom.ChatListCell;
import io.xeres.ui.support.chat.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static io.xeres.ui.support.chat.ChatAction.Type.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class ChatListView
{
	private static final Logger log = LoggerFactory.getLogger(ChatListView.class);

	private final ObservableList<ChatLine> messages = FXCollections.observableArrayList();
	private final Map<GxsId, ChatRoomUser> userMap = new HashMap<>();
	private final ObservableList<ChatRoomUser> users = FXCollections.observableArrayList();

	private String nickname;
	private final long id;

	private final VirtualizedScrollPane<VirtualFlow<ChatLine, ChatListCell>> chatView;
	private final ListView<ChatRoomUser> userListView;

	enum AddUserOrigin
	{
		JOIN,
		KEEP_ALIVE
	}

	public ChatListView(String nickname, long id)
	{
		this.nickname = nickname;
		this.id = id;

		chatView = createChatView();
		userListView = createUserListView();
	}

	private VirtualizedScrollPane<VirtualFlow<ChatLine, ChatListCell>> createChatView()
	{
		final var view = VirtualFlow.createVertical(messages, ChatListCell::new, VirtualFlow.Gravity.REAR);
		view.setFocusTraversable(false);
		view.getStyleClass().add("chat-list");
		return new VirtualizedScrollPane<>(view);
	}

	private ListView<ChatRoomUser> createUserListView()
	{
		final ListView<ChatRoomUser> view;
		view = new ListView<>();
		view.getStyleClass().add("chat-user-list");
		VBox.setVgrow(view, Priority.ALWAYS);

		view.setCellFactory(ChatUserCell::new);
		view.setItems(users);
		return view;
	}

	public void addOwnMessage(String message)
	{
		var chatAction = new ChatAction(SAY_OWN, nickname, null);
		addMessage(chatAction, message);
	}

	public void addUserMessage(String from, String message)
	{
		var chatAction = new ChatAction(SAY, from, null);
		addMessage(chatAction, message);
	}

	private void addMessage(ChatAction chatAction, String message)
	{
		var img = Jsoup.parse(message).selectFirst("img");

		if (img != null)
		{
			var data = img.absUrl("src");
			if (isNotEmpty(data))
			{
				var image = new Image(data);
				if (!image.isError())
				{
					addMessageLine(chatAction, image);
				}
			}
		}
		else
		{
			if (ChatParser.isActionMe(message))
			{
				message = ChatParser.parseActionMe(message, chatAction.getNickname());
				chatAction.setType(ACTION);
			}
			var chatContents = ChatParser.parse(message);
			var chatLine = new ChatLine(Instant.now(), chatAction, chatContents.toArray(ChatContent[]::new));
			addMessageLine(chatLine);
		}
	}

	public void addUser(ChatRoomUserEvent event, AddUserOrigin addUserOrigin)
	{
		if (!userMap.containsKey(event.getGxsId()))
		{
			var chatRoomUser = new ChatRoomUser(event.getGxsId(), event.getNickname());
			users.add(chatRoomUser);
			userMap.put(event.getGxsId(), chatRoomUser);
			users.sort((o1, o2) -> o1.nickname().compareToIgnoreCase(o2.nickname()));
			if (addUserOrigin == AddUserOrigin.JOIN && !nickname.equals(event.getNickname()))
			{
				addMessageLine(new ChatAction(JOIN, event.getNickname(), event.getGxsId()));
			}
		}
	}

	public void removeUser(ChatRoomUserEvent event)
	{
		var chatRoomUser = userMap.remove(event.getGxsId());

		if (chatRoomUser != null)
		{
			users.remove(chatRoomUser);
			addMessageLine(new ChatAction(LEAVE, event.getNickname(), event.getGxsId()));
		}
	}

	public void timeoutUser(ChatRoomTimeoutEvent event)
	{
		var chatRoomUser = userMap.remove(event.getGxsId());

		if (chatRoomUser != null)
		{
			users.remove(chatRoomUser);
			if (!event.isSplit())
			{
				addMessageLine(new ChatAction(TIMEOUT, chatRoomUser.nickname(), event.getGxsId()));
			}
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
		// We use an anchor to force the VirtualFlow to be bigger
		// than its default size of 100 x 100. It doesn't behave
		// well in a VBox only.
		var anchor = new AnchorPane(chatView);
		AnchorPane.setTopAnchor(chatView, 0.0);
		AnchorPane.setLeftAnchor(chatView, 0.0);
		AnchorPane.setRightAnchor(chatView, 0.0);
		AnchorPane.setBottomAnchor(chatView, 0.0);
		VBox.setVgrow(anchor, Priority.ALWAYS);
		return anchor;
	}

	public Node getUserListView()
	{
		return userListView;
	}

	public long getId()
	{
		return id;
	}

	private void addMessageLine(ChatLine line)
	{
		messages.add(line);
		jumpToBottom(false);
	}

	private void addMessageLine(ChatAction action, Image image)
	{
		var chatLine = new ChatLine(Instant.now(), action, new ChatContentImage(image));
		addMessageLine(chatLine);
	}

	private void addMessageLine(ChatAction action)
	{
		var chatLine = new ChatLine(Instant.now(), action);
		addMessageLine(chatLine);
	}

	public void jumpToBottom(boolean force)
	{
		var lastIndex = messages.size() - 1;
		if (force || chatView.getContent().getLastVisibleIndex() == lastIndex - 1) // XXX: why -1?!
		{
			chatView.getContent().showAsFirst(lastIndex);
		}
	}
}
