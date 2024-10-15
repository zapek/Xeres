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

import io.xeres.common.id.GxsId;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.common.message.chat.ChatRoomTimeoutEvent;
import io.xeres.common.message.chat.ChatRoomUserEvent;
import io.xeres.ui.custom.ChatListCell;
import io.xeres.ui.support.chat.ChatAction;
import io.xeres.ui.support.chat.ChatLine;
import io.xeres.ui.support.chat.ChatParser;
import io.xeres.ui.support.chat.NicknameCompleter;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentImage;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.markdown.MarkdownService.ParsingMode;
import io.xeres.ui.support.markdown.UriAction;
import io.xeres.ui.support.uri.IdentityUri;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.jsoup.Jsoup;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static io.xeres.ui.support.chat.ChatAction.Type.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class ChatListView implements NicknameCompleter.UsernameFinder
{
	private static final int SCROLL_BACK_MAX_LINES = 2000;
	private static final int SCROLL_BACK_CLEANUP_THRESHOLD = 100;

	private static final String INFO_MENU_ID = "info";

	private final ObservableList<ChatLine> messages = FXCollections.observableArrayList();
	private final Map<GxsId, ChatRoomUser> userMap = new HashMap<>();
	private final ObservableList<ChatRoomUser> users = FXCollections.observableArrayList();

	private String nickname;
	private final long id;

	private final VirtualizedScrollPane<VirtualFlow<ChatLine, ChatListCell>> chatView;
	private final ListView<ChatRoomUser> userListView;
	private final MarkdownService markdownService;
	private final UriAction uriAction;

	public enum AddUserOrigin
	{
		JOIN,
		KEEP_ALIVE
	}

	public ChatListView(String nickname, long id, MarkdownService markdownService, UriAction uriAction)
	{
		this.nickname = nickname;
		this.id = id;
		this.markdownService = markdownService;
		this.uriAction = uriAction;

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

		view.setCellFactory(param -> new ChatUserCell());
		view.setItems(users);

		createUsersListViewContextMenu(view);
		return view;
	}

	public void addOwnMessage(ChatMessage chatMessage)
	{
		addOwnMessage(Instant.now(), chatMessage.getContent());
	}

	public void addOwnMessage(Instant when, String message)
	{
		var chatAction = new ChatAction(SAY_OWN, nickname, null);
		addMessage(when, chatAction, message);
	}

	public void addUserMessage(String from, String message)
	{
		addUserMessage(Instant.now(), from, message);
	}

	public void addUserMessage(Instant when, String from, String message)
	{
		var chatAction = new ChatAction(SAY, from, null);
		addMessage(when, chatAction, message);
	}

	public void addUserMessage(String from, GxsId gxsId, String message)
	{
		var chatAction = new ChatAction(SAY, from, gxsId);
		addMessage(Instant.now(), chatAction, message);
	}

	private void addMessage(Instant time, ChatAction chatAction, String message)
	{
		message = removeEmtpyImageTag(message);

		var img = Jsoup.parse(message).selectFirst("img");

		if (img != null)
		{
			var data = img.absUrl("src");
			if (isNotEmpty(data) && data.startsWith("data:")) // the core only allows 'data' already but better safe than sorry
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
			var content = markdownService.parse(message, EnumSet.of(ParsingMode.ONE_LINER), uriAction);
			var chatLine = new ChatLine(time, chatAction, content.toArray(new Content[0]));
			addMessageLine(chatLine);
		}
	}

	/**
	 * Removes the empty img tag that is added by Retroshare when sending a file URL.
	 *
	 * @param message the message
	 * @return the cleaned up message
	 */
	private static String removeEmtpyImageTag(String message)
	{
		if (message.startsWith("<img>") && message.length() > 5)
		{
			message = message.substring(5);
		}
		return message;
	}

	public void addUser(ChatRoomUserEvent event, AddUserOrigin addUserOrigin)
	{
		if (!userMap.containsKey(event.getGxsId()))
		{
			var chatRoomUser = new ChatRoomUser(event.getGxsId(), event.getNickname(), buildImageView(event.getImage()));
			users.add(chatRoomUser);
			userMap.put(event.getGxsId(), chatRoomUser);
			users.sort((o1, o2) -> o1.nickname().compareToIgnoreCase(o2.nickname()));
			if (addUserOrigin == AddUserOrigin.JOIN && !nickname.equals(event.getNickname()))
			{
				addMessageLine(new ChatAction(JOIN, event.getNickname(), event.getGxsId()));
			}
		}
	}

	private static ImageView buildImageView(byte[] imageData)
	{
		if (imageData != null)
		{
			return new ImageView(new Image(new ByteArrayInputStream(imageData)));
		}
		return null;
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
			if (!event.isSplit() && userSaidSomethingRecently(event.getGxsId()))
			{
				// Only display this if the user said something 5-10 minutes ago, so that we know that the conversation is "dead". Displaying it all the time is too verbose
				addMessageLine(new ChatAction(TIMEOUT, chatRoomUser.nickname(), event.getGxsId()));
			}
		}
	}

	private boolean userSaidSomethingRecently(GxsId gxsId)
	{
		var now = Instant.now();

		for (var i = messages.size() - 1; i >= 0; i--)
		{
			var message = messages.get(i);
			if (message.getInstant().isBefore(now.minus(10, ChronoUnit.MINUTES)))
			{
				break;
			}

			if (message.hasSaid(gxsId))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public String getUsername(String prefix, int index)
	{
		var prefixLower = prefix.toLowerCase(Locale.ROOT);
		var matchingUsers = users.stream()
				.filter(chatRoomUser -> !chatRoomUser.nickname().equals(nickname) && (isEmpty(prefix) || chatRoomUser.nickname().toLowerCase(Locale.ROOT).startsWith(prefixLower)))
				.toList();

		if (matchingUsers.isEmpty())
		{
			return null;
		}
		return matchingUsers.get(index % matchingUsers.size()).nickname();
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
		anchor.getStyleClass().add("chat-list-pane");
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
		trimScrollBackIfNeeded();
	}

	private void addMessageLine(ChatAction action, Image image)
	{
		var chatLine = new ChatLine(Instant.now(), action, new ContentImage(image));
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

	private void trimScrollBackIfNeeded()
	{
		if (messages.size() >= SCROLL_BACK_MAX_LINES)
		{
			messages.remove(0, SCROLL_BACK_CLEANUP_THRESHOLD);
		}
	}

	private void createUsersListViewContextMenu(Node view)
	{
		var infoItem = new MenuItem("Information");
		infoItem.setId(INFO_MENU_ID);
		infoItem.setGraphic(new FontIcon(FontAwesomeSolid.INFO_CIRCLE));
		infoItem.setOnAction(event -> {
			var user = (ChatRoomUser) event.getSource();
			if (user.gxsId() != null)
			{
				uriAction.openUri(new IdentityUri(user.nickname(), user.gxsId(), null));
			}
		});

		var xContextMenu = new XContextMenu<ChatRoomUser>(infoItem);
		xContextMenu.addToNode(view);
	}
}
