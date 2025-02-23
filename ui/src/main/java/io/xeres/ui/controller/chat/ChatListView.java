/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.id.GxsId;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.common.message.chat.ChatRoomMessage;
import io.xeres.common.message.chat.ChatRoomTimeoutEvent;
import io.xeres.common.message.chat.ChatRoomUserEvent;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.asyncimage.ImageCache;
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
import io.xeres.ui.support.window.WindowManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.jsoup.Jsoup;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static io.xeres.common.dto.identity.IdentityConstants.OWN_IDENTITY_ID;
import static io.xeres.ui.support.chat.ChatAction.Type.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class ChatListView implements NicknameCompleter.UsernameFinder
{
	private static final Logger log = LoggerFactory.getLogger(ChatListView.class);

	private static final int SCROLL_BACK_MAX_LINES = 1000;
	private static final int SCROLL_BACK_CLEANUP_THRESHOLD = 100;

	private static final String INFO_MENU_ID = "info";
	private static final String CHAT_MENU_ID = "chat";

	private final ObservableList<ChatLine> messages = FXCollections.observableArrayList();
	private final Map<GxsId, ChatRoomUser> userMap = new HashMap<>();
	private final ObservableList<ChatRoomUser> users = FXCollections.observableArrayList();

	private String nickname;
	private final long id;

	private final AnchorPane anchorPane;
	private final VirtualizedScrollPane<VirtualFlow<ChatLine, ChatListCell>> chatView;
	private final ListView<ChatRoomUser> userListView;
	private final MarkdownService markdownService;
	private final UriAction uriAction;
	private final GeneralClient generalClient;
	private final ImageCache imageCache;
	private final ResourceBundle bundle;
	private final WindowManager windowManager;

	private final ChatListDragSelection dragSelection;

	public enum AddUserOrigin
	{
		JOIN,
		KEEP_ALIVE
	}

	public ChatListView(String nickname, long id, MarkdownService markdownService, UriAction uriAction, GeneralClient generalClient, ImageCache imageCache, WindowManager windowManager, Node focusNode)
	{
		this.nickname = nickname;
		this.id = id;
		this.markdownService = markdownService;
		this.uriAction = uriAction;
		this.generalClient = generalClient;
		this.imageCache = imageCache;
		this.windowManager = windowManager;
		bundle = I18nUtils.getBundle();
		anchorPane = new AnchorPane();

		dragSelection = new ChatListDragSelection(focusNode);

		chatView = createChatView(dragSelection);
		addToAnchorPane(chatView, anchorPane);

		userListView = createUserListView();
	}

	private VirtualizedScrollPane<VirtualFlow<ChatLine, ChatListCell>> createChatView(ChatListDragSelection selection)
	{
		final var view = VirtualFlow.createVertical(messages, ChatListCell::new, VirtualFlow.Gravity.REAR);
		view.setFocusTraversable(false);
		view.getStyleClass().add("chat-list");
		view.addEventFilter(MouseEvent.MOUSE_PRESSED, selection::press);
		view.addEventFilter(MouseEvent.MOUSE_DRAGGED, selection::drag);
		view.addEventFilter(MouseEvent.MOUSE_RELEASED, selection::release);
		return new VirtualizedScrollPane<>(view);
	}

	private ListView<ChatRoomUser> createUserListView()
	{
		final ListView<ChatRoomUser> view;
		view = new ListView<>();
		view.getStyleClass().add("chat-user-list");
		VBox.setVgrow(view, Priority.ALWAYS);

		view.setCellFactory(param -> new ChatUserCell(generalClient, imageCache));
		view.setItems(users);

		createUsersListViewContextMenu(view);
		return view;
	}

	public boolean copy()
	{
		if (dragSelection.isSelected())
		{
			dragSelection.copy();
			return true;
		}
		return false;
	}

	public void addOwnMessage(ChatMessage chatMessage)
	{
		addOwnMessage(Instant.now(), chatMessage.getContent());
	}

	public void addOwnMessage(ChatRoomMessage chatRoomMessage)
	{
		addOwnMessage(Instant.now(), chatRoomMessage.getContent());
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
		addUserMessage(Instant.now(), from, gxsId, message);
	}

	public void addUserMessage(Instant when, String from, GxsId gxsId, String message)
	{
		var chatAction = new ChatAction(SAY, from, gxsId);
		addMessage(when, chatAction, message);
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
					addMessageLine(time, chatAction, image);
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
			var chatRoomUser = new ChatRoomUser(event.getGxsId(), event.getNickname(), event.getIdentityId());
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
		return anchorPane;
	}

	private static void addToAnchorPane(Node chatView, AnchorPane anchorPane)
	{
		// We use an anchor to force the VirtualFlow to be bigger
		// than its default size of 100 x 100. It doesn't behave
		// well in a VBox only.
		anchorPane.getChildren().add(chatView);
		anchorPane.getStyleClass().add("chat-list-pane");
		AnchorPane.setTopAnchor(chatView, 0.0);
		AnchorPane.setLeftAnchor(chatView, 0.0);
		AnchorPane.setRightAnchor(chatView, 0.0);
		AnchorPane.setBottomAnchor(chatView, 0.0);
		VBox.setVgrow(anchorPane, Priority.ALWAYS);
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

	private void addMessageLine(Instant when, ChatAction action, Image image)
	{
		var chatLine = new ChatLine(when, action, new ContentImage(image, chatView));
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
		var infoItem = new MenuItem(bundle.getString("chat.room.user-menu"));
		infoItem.setId(INFO_MENU_ID);
		infoItem.setGraphic(new FontIcon(MaterialDesignA.ACCOUNT_BOX));
		infoItem.setOnAction(event -> {
			var user = (ChatRoomUser) event.getSource();
			uriAction.openUri(new IdentityUri(user.nickname(), user.gxsId(), null));
		});

		var chatItem = new MenuItem(bundle.getString("contact-view.action.chat"));
		chatItem.setId(CHAT_MENU_ID);
		chatItem.setGraphic(new FontIcon(MaterialDesignC.COMMENT));
		chatItem.setOnAction(event -> {
			var user = (ChatRoomUser) event.getSource();
			windowManager.openMessaging(user.gxsId());
		});

		var xContextMenu = new XContextMenu<ChatRoomUser>(chatItem, infoItem);
		xContextMenu.setOnShowing((contextMenu, chatRoomUser) -> {
			contextMenu.getItems().stream()
					.filter(menuItem -> CHAT_MENU_ID.equals(menuItem.getId()))
					.findFirst().ifPresent(menuItem -> menuItem.setDisable(chatRoomUser.identityId() == OWN_IDENTITY_ID));

			return chatRoomUser.gxsId() != null;
		});
		xContextMenu.addToNode(view);
	}
}
