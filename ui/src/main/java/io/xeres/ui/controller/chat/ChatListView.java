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

package io.xeres.ui.controller.chat;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.id.GxsId;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.common.message.chat.ChatRoomMessage;
import io.xeres.common.message.chat.ChatRoomTimeoutEvent;
import io.xeres.common.message.chat.ChatRoomUserEvent;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.preview.PreviewClient;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.support.chat.ChatAction;
import io.xeres.ui.support.chat.ChatLine;
import io.xeres.ui.support.chat.ChatParser;
import io.xeres.ui.support.chat.NicknameCompleter;
import io.xeres.ui.support.contentline.ContentImage;
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.contentline.ContentUriPreview;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.markdown.UriAction;
import io.xeres.ui.support.uri.ExternalUri;
import io.xeres.ui.support.uri.IdentityUri;
import io.xeres.ui.support.util.ImageViewUtils;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
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
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static io.xeres.common.dto.identity.IdentityConstants.OWN_IDENTITY_ID;
import static io.xeres.ui.support.chat.ChatAction.Type.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class ChatListView implements NicknameCompleter.UsernameFinder
{
	private static final int SCROLL_BACK_MAX_LINES = 1000;
	private static final int SCROLL_BACK_CLEANUP_THRESHOLD = 100;

	private static final Duration PREVIEW_WINDOW = Duration.ofSeconds(30);

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
	private final PreviewClient previewClient;

	private final ChatListDragSelection dragSelection;

	private final ChatListViewContextMenu contextMenu;

	public enum AddUserOrigin
	{
		JOIN,
		KEEP_ALIVE
	}

	public ChatListView(String nickname, long id, MarkdownService markdownService, UriAction uriAction, GeneralClient generalClient, PreviewClient previewClient, ImageCache imageCache, WindowManager windowManager, Node focusNode)
	{
		this.nickname = nickname;
		this.id = id;
		this.markdownService = markdownService;
		this.uriAction = uriAction;
		this.generalClient = generalClient;
		this.previewClient = previewClient;
		this.imageCache = imageCache;
		this.windowManager = windowManager;
		bundle = I18nUtils.getBundle();
		anchorPane = new AnchorPane();

		dragSelection = new ChatListDragSelection(focusNode);

		chatView = createChatView(dragSelection);
		addToAnchorPane(chatView, anchorPane);

		userListView = createUserListView();

		contextMenu = new ChatListViewContextMenu();

		// Make sure we stick to the bottom even when we resize the chatview (user typing multiple lines, other user offline, ...)
		anchorPane.heightProperty().addListener((_, _, _) -> jumpToBottom(false));
	}

	public void installClearHistoryContextMenu(Runnable action)
	{
		contextMenu.installClearHistoryMenu(_ -> UiUtils.showAlertConfirm(bundle.getString("chat.room.clear-history"), () -> {
			action.run();
			messages.clear();
		}));
	}

	private VirtualizedScrollPane<VirtualFlow<ChatLine, ChatListCell>> createChatView(ChatListDragSelection selection)
	{
		final var view = VirtualFlow.createVertical(messages, ChatListCell::new, VirtualFlow.Gravity.REAR);
		view.setFocusTraversable(false);
		view.getStyleClass().add("chat-list");
		view.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			contextMenu.hide();
			if (!e.isSecondaryButtonDown())
			{
				selection.press(e);
				contextMenu.removeSelectionMenu();
			}
		});
		view.addEventFilter(MouseEvent.MOUSE_DRAGGED, selection::drag);
		view.addEventFilter(MouseEvent.MOUSE_RELEASED, selection::release);
		view.setOnContextMenuRequested(event -> {
			if (selection.isSelected())
			{
				contextMenu.installSelectionMenu(_ -> selection.copy());
			}
			contextMenu.show(view, event.getScreenX(), event.getScreenY());
			event.consume();
		});

		return new VirtualizedScrollPane<>(view);
	}

	private ListView<ChatRoomUser> createUserListView()
	{
		final ListView<ChatRoomUser> view;
		view = new ListView<>();
		view.getStyleClass().add("chat-user-list");
		VBox.setVgrow(view, Priority.ALWAYS);

		view.setCellFactory(_ -> new ChatUserCell(generalClient, imageCache));
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
		jumpToBottom(true); // Always move to the bottom for our own message
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
				if (!image.isError() && !ImageViewUtils.isExaggeratedAspectRatio(image))
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
			var contents = markdownService.parse(message, Set.of(), uriAction);
			var chatLine = new ChatLine(time, chatAction, contents);
			addMessageLine(chatLine);
			scanForPreview(time, chatLine);
		}
	}

	private void scanForPreview(Instant messageArrival, ChatLine chatLine)
	{
		if (previewClient == null || Duration.between(messageArrival, Instant.now()).compareTo(PREVIEW_WINDOW) > 0) // Don't preview "old" URLs
		{
			return;
		}

		chatLine.getChatContents().stream()
				.filter(content -> content instanceof ContentUri cUri && cUri.getUri().startsWith("https://"))
				.findFirst()
				.map(content -> ((ContentUri) content).getUri())
				.ifPresent(url -> previewClient.getPreview(url)
						.doOnSuccess(preview -> Platform.runLater(() -> {
							assert preview != null;
							if (!preview.hasInfo())
							{
								return;
							}
							var index = messages.indexOf(chatLine);
							if (index >= 0)
							{
								var contents = new ArrayList<>(chatLine.getChatContents());
								for (var i = 0; i < contents.size(); i++)
								{
									if (contents.get(i) instanceof ContentUri contentUri)
									{
										contents.set(i, new ContentUriPreview(new ExternalUri(url), preview.title(), preview.description(), preview.site(), preview.thumbnailUrl(), preview.thumbnailWidth(), preview.thumbnailHeight(), thumbUrl -> previewClient.getImage(thumbUrl).block(), contentUri.getAction()));
										break;
									}
								}
								var newChatLine = chatLine.withContent(contents);
								messages.set(index, newChatLine);
								jumpToBottom(false);
							}
						}))
						.subscribe());
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

	ListView<ChatRoomUser> getUserListView()
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
		var chatLine = new ChatLine(when, action, List.of(new ContentImage(image, chatView)));
		addMessageLine(chatLine);
	}

	private void addMessageLine(ChatAction action)
	{
		var chatLine = new ChatLine(Instant.now(), action, List.of());
		addMessageLine(chatLine);
	}

	/**
	 * Jumps to the bottom of the chat listview.
	 *
	 * @param force always jumps, otherwise it will only jump if it was already at the bottom at the last layout
	 */
	public void jumpToBottom(boolean force)
	{
		if (force || messages.size() - chatView.getContent().getLastVisibleIndex() <= 2)
		{
			chatView.getContent().showAsFirst(messages.size());
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
		chatItem.setGraphic(new FontIcon(MaterialDesignM.MESSAGE_ARROW_RIGHT));
		chatItem.setOnAction(event -> {
			var user = (ChatRoomUser) event.getSource();
			windowManager.openMessaging(user.gxsId());
		});

		var xContextMenu = new XContextMenu<ChatRoomUser>(chatItem, infoItem);
		xContextMenu.setOnShowing((cm, chatRoomUser) -> {
			if (chatRoomUser == null)
			{
				return false;
			}
			cm.getItems().stream()
					.filter(menuItem -> CHAT_MENU_ID.equals(menuItem.getId()))
					.findFirst().ifPresent(menuItem -> menuItem.setDisable(chatRoomUser.identityId() == OWN_IDENTITY_ID));

			return chatRoomUser.gxsId() != null;
		});
		xContextMenu.addToNode(view);
	}
}
