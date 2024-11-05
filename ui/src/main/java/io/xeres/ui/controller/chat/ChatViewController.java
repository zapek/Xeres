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

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.message.chat.*;
import io.xeres.ui.OpenUriEvent;
import io.xeres.ui.client.ChatClient;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.chat.ChatListView.AddUserOrigin;
import io.xeres.ui.custom.TypingNotificationView;
import io.xeres.ui.support.chat.ChatCommand;
import io.xeres.ui.support.chat.NicknameCompleter;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.preference.PreferenceService;
import io.xeres.ui.support.tray.TrayService;
import io.xeres.ui.support.uri.ChatRoomUri;
import io.xeres.ui.support.uri.ChatRoomUriFactory;
import io.xeres.ui.support.uri.UriService;
import io.xeres.ui.support.util.ImageUtils;
import io.xeres.ui.support.util.TextInputControlUtils;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.xeres.common.message.chat.ChatConstants.TYPING_NOTIFICATION_DELAY;
import static io.xeres.ui.support.preference.PreferenceService.CHAT_ROOMS;
import static javafx.scene.control.Alert.AlertType.WARNING;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@FxmlView(value = "/view/chat/chatview.fxml")
public class ChatViewController implements Controller
{
	private static final int PREVIEW_IMAGE_WIDTH_MAX = 320;
	private static final int PREVIEW_IMAGE_HEIGHT_MAX = 240;
	private static final int MESSAGE_MAXIMUM_SIZE = 31000; // XXX: put that on chat service too as we shouldn't forward them. also this is only for chat rooms, not private chats
	private static final KeyCodeCombination TAB_KEY = new KeyCodeCombination(KeyCode.TAB);
	private static final KeyCodeCombination PASTE_KEY = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
	private static final KeyCodeCombination ENTER_KEY = new KeyCodeCombination(KeyCode.ENTER);
	private static final KeyCodeCombination BACKSPACE_KEY = new KeyCodeCombination(KeyCode.BACK_SPACE);
	private static final String SUBSCRIBED_MENU_ID = "subscribed";
	private static final String UNSUBSCRIBED_MENU_ID = "unsubscribed";
	private static final String COPY_LINK_MENU_ID = "copyLink";

	private static final String OPEN_SUBSCRIBED = "OpenSubscribed";
	private static final String OPEN_PRIVATE = "OpenPrivate";
	private static final String OPEN_PUBLIC = "OpenPublic";

	@FXML
	private TreeView<RoomHolder> roomTree;

	@FXML
	private SplitPane splitPane;

	@FXML
	private VBox content;

	@FXML
	private TextField send;

	@FXML
	private VBox sendGroup;

	@FXML
	private TypingNotificationView typingNotification;

	@FXML
	private HBox previewGroup;

	@FXML
	private ImageView imagePreview;

	@FXML
	private Button previewSend;

	@FXML
	private Button previewCancel;

	@FXML
	private VBox userListContent;

	@FXML
	private Button invite;

	@FXML
	private HBox status;

	@FXML
	private Label roomName;

	@FXML
	private Label roomTopic;

	@FXML
	public Button createChatRoom;

	private final MessageClient messageClient;
	private final ChatClient chatClient;
	private final ProfileClient profileClient;
	private final LocationClient locationClient;
	private final WindowManager windowManager;
	private final TrayService trayService;
	private final ResourceBundle bundle;
	private final MarkdownService markdownService;
	private final UriService uriService;
	private final PreferenceService preferenceService;

	private final TreeItem<RoomHolder> subscribedRooms;
	private final TreeItem<RoomHolder> privateRooms;
	private final TreeItem<RoomHolder> publicRooms;

	private String nickname;

	private final NicknameCompleter nicknameCompleter = new NicknameCompleter();
	private ChatRoomInfo selectedRoom;
	private ChatListView selectedChatListView;
	private Node roomInfoView;
	private ChatRoomInfoController chatRoomInfoController;

	private Instant lastTypingNotification = Instant.EPOCH;

	private double[] dividerPositions;

	private Timeline lastTypingTimeline;

	public ChatViewController(MessageClient messageClient, ChatClient chatClient, ProfileClient profileClient, LocationClient locationClient, WindowManager windowManager, TrayService trayService, ResourceBundle bundle, MarkdownService markdownService, UriService uriService, PreferenceService preferenceService)
	{
		this.messageClient = messageClient;
		this.chatClient = chatClient;
		this.profileClient = profileClient;
		this.locationClient = locationClient;
		this.windowManager = windowManager;
		this.trayService = trayService;
		this.bundle = bundle;
		this.markdownService = markdownService;
		this.uriService = uriService;

		subscribedRooms = new TreeItem<>(new RoomHolder(bundle.getString("chat.room.subscribed")));
		privateRooms = new TreeItem<>(new RoomHolder(bundle.getString("enum.roomtype.private")));
		publicRooms = new TreeItem<>(new RoomHolder(bundle.getString("enum.roomtype.public")));
		this.preferenceService = preferenceService;
	}

	@Override
	public void initialize() throws IOException
	{
		profileClient.getOwn().doOnSuccess(profile -> nickname = profile.getName()) // XXX: we shouldn't go further until nickname is set. maybe it should be a parameter
				.subscribe();

		var root = new TreeItem<>(new RoomHolder());
		//noinspection unchecked
		root.getChildren().addAll(subscribedRooms, privateRooms, publicRooms);
		root.setExpanded(true);
		roomTree.setRoot(root);
		roomTree.setShowRoot(false);
		roomTree.setCellFactory(param -> new ChatRoomCell());
		createRoomTreeContextMenu();

		// We need Platform.runLater() because when an entry is moved, the selection can change
		roomTree.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> Platform.runLater(() -> changeSelectedRoom(newValue.getValue().getRoomInfo())));

		roomTree.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2 && isRoomSelected())
			{
				joinChatRoom(selectedRoom);
			}
		});

		send.setOnKeyPressed(event ->
		{
			if (isRoomSelected())
			{
				if (event.getCode() == KeyCode.ENTER && isNotBlank(send.getText()))
				{
					sendChatMessage(send.getText());
					send.clear();
					lastTypingNotification = Instant.EPOCH;
				}
				else
				{
					var now = Instant.now();
					if (Duration.between(lastTypingNotification, now).compareTo(TYPING_NOTIFICATION_DELAY.minusSeconds(1)) > 0)
					{
						var chatMessage = new ChatMessage();
						messageClient.sendToChatRoom(selectedRoom.getId(), chatMessage);
						lastTypingNotification = now;
					}
				}
			}
		});

		var loader = new FXMLLoader(getClass().getResource("/view/chat/chat_roominfo.fxml"), bundle);
		roomInfoView = loader.load();
		chatRoomInfoController = loader.getController();

		lastTypingTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(TYPING_NOTIFICATION_DELAY.getSeconds())));
		lastTypingTimeline.setOnFinished(event -> typingNotification.setText(""));

		VBox.setVgrow(roomInfoView, Priority.ALWAYS);
		switchChatContent(roomInfoView, null);
		sendGroup.setVisible(false);
		setPreviewGroupVisibility(false);

		previewSend.setOnAction(event -> sendImage());
		previewCancel.setOnAction(event -> cancelImage());

		send.addEventHandler(KeyEvent.KEY_PRESSED, this::handleInputKeys);
		TextInputControlUtils.addEnhancedInputContextMenu(send, locationClient);

		invite.setOnAction(event -> windowManager.openInvite(selectedRoom.getId()));

		getChatRoomContext();

		createChatRoom.setOnAction(event -> windowManager.openChatRoomCreation());

		setupTrees();
	}

	private void setupTrees()
	{
		var node = preferenceService.getPreferences().node(CHAT_ROOMS);
		subscribedRooms.setExpanded(node.getBoolean(OPEN_SUBSCRIBED, false));
		privateRooms.setExpanded(node.getBoolean(OPEN_PRIVATE, false));
		publicRooms.setExpanded(node.getBoolean(OPEN_PUBLIC, false));

		subscribedRooms.expandedProperty().addListener((observable, oldValue, newValue) -> node.putBoolean(OPEN_SUBSCRIBED, newValue));
		privateRooms.expandedProperty().addListener((observable, oldValue, newValue) -> node.putBoolean(OPEN_PRIVATE, newValue));
		publicRooms.expandedProperty().addListener((observable, oldValue, newValue) -> node.putBoolean(OPEN_PUBLIC, newValue));
	}

	@EventListener
	public void handleOpenUriEvents(OpenUriEvent event)
	{
		if (event.uri() instanceof ChatRoomUri chatRoomUri)
		{
			var chatRoomId = chatRoomUri.id();

			getAllTreeItem(chatRoomId).ifPresentOrElse(treeItem -> Platform.runLater(() -> roomTree.getSelectionModel().select(treeItem)),
					() -> UiUtils.alert(WARNING, bundle.getString("chat.room.not-found")));
		}
	}

	private void createRoomTreeContextMenu()
	{
		var subscribeItem = new MenuItem(I18nUtils.getString("chat.room.join"));
		subscribeItem.setId(SUBSCRIBED_MENU_ID);
		subscribeItem.setGraphic(new FontIcon(MaterialDesignL.LOCATION_ENTER));
		subscribeItem.setOnAction(event -> joinChatRoom(((RoomHolder) event.getSource()).getRoomInfo()));

		var unsubscribeItem = new MenuItem(I18nUtils.getString("chat.room.leave"));
		unsubscribeItem.setId(UNSUBSCRIBED_MENU_ID);
		unsubscribeItem.setGraphic(new FontIcon(MaterialDesignL.LOCATION_EXIT));
		unsubscribeItem.setOnAction(event -> leaveChatRoom(((RoomHolder) event.getSource()).getRoomInfo()));

		var copyLinkItem = new MenuItem(I18nUtils.getString("copy-link"));
		copyLinkItem.setId(COPY_LINK_MENU_ID);
		copyLinkItem.setGraphic(new FontIcon(MaterialDesignL.LINK_VARIANT));
		copyLinkItem.setOnAction(event -> {
			var clipboardContent = new ClipboardContent();
			var chatRoomInfo = ((RoomHolder) event.getSource()).getRoomInfo();
			clipboardContent.putString(ChatRoomUriFactory.generate(chatRoomInfo.getName(), chatRoomInfo.getId()));
			Clipboard.getSystemClipboard().setContent(clipboardContent);
		});

		var xContextMenu = new XContextMenu<RoomHolder>(subscribeItem, unsubscribeItem, new SeparatorMenuItem(), copyLinkItem);
		xContextMenu.addToNode(roomTree);
		xContextMenu.setOnShowing((contextMenu, roomHolder) -> {
			var chatRoomInfo = roomHolder.getRoomInfo();

			contextMenu.getItems().stream()
					.filter(menuItem -> SUBSCRIBED_MENU_ID.equals(menuItem.getId()))
					.findFirst().ifPresent(menuItem -> menuItem.setDisable(isAlreadyJoined(chatRoomInfo)));

			contextMenu.getItems().stream()
					.filter(menuItem -> UNSUBSCRIBED_MENU_ID.equals(menuItem.getId()))
					.findFirst().ifPresent(menuItem -> menuItem.setDisable(!isAlreadyJoined(chatRoomInfo)));

			return chatRoomInfo.isReal();
		});
	}

	private boolean isAlreadyJoined(ChatRoomInfo chatRoomInfo)
	{
		return subscribedRooms.getChildren().stream()
				.anyMatch(roomHolderTreeItem -> roomHolderTreeItem.getValue().getRoomInfo().equals(chatRoomInfo));
	}

	private void joinChatRoom(ChatRoomInfo chatRoomInfo)
	{
		if (!isAlreadyJoined(chatRoomInfo))
		{
			chatClient.joinChatRoom(chatRoomInfo.getId())
					.subscribe();
		}
	}

	private void leaveChatRoom(ChatRoomInfo chatRoomInfo)
	{
		subscribedRooms.getChildren().stream()
				.filter(roomHolderTreeItem -> roomHolderTreeItem.getValue().getRoomInfo().equals(chatRoomInfo))
				.findAny()
				.ifPresent(roomHolderTreeItem -> chatClient.leaveChatRoom(chatRoomInfo.getId())
						.subscribe());
	}

	private void getChatRoomContext()
	{
		chatClient.getChatRoomContext()
				.doOnSuccess(context -> {
					addRooms(context.chatRoomLists());
					context.chatRoomLists().getSubscribed().forEach(chatRoomInfo -> userJoined(chatRoomInfo.getId(), new ChatRoomUserEvent(context.ownUser().gxsId(), context.ownUser().nickname(), context.ownUser().image())));
				})
				.subscribe();
	}

	public void addRooms(ChatRoomLists chatRoomLists)
	{
		var subscribedTree = subscribedRooms.getChildren();
		var publicTree = publicRooms.getChildren();
		var privateTree = privateRooms.getChildren();

		chatRoomLists.getSubscribed()
				.forEach(roomInfo -> addOrUpdate(subscribedTree, roomInfo));

		// Make sure we don't add rooms that we're already subscribed to
		var unsubscribedRooms = chatRoomLists.getAvailable().stream()
				.filter(roomInfo -> !isInside(subscribedTree, roomInfo))
				.toList();

		syncTreeWithChatRoomList(publicTree, unsubscribedRooms.stream()
				.filter(roomInfo -> roomInfo.getRoomType() == RoomType.PUBLIC)
				.toList());

		syncTreeWithChatRoomList(privateTree, unsubscribedRooms.stream()
				.filter(roomInfo -> roomInfo.getRoomType() == RoomType.PRIVATE)
				.toList());
	}

	private void syncTreeWithChatRoomList(ObservableList<TreeItem<RoomHolder>> tree, List<ChatRoomInfo> list)
	{
		list.forEach(chatRoomInfo -> addOrUpdate(tree, chatRoomInfo));

		var chatRoomIds = list.stream()
				.map(ChatRoomInfo::getId)
				.collect(Collectors.toSet());
		tree.removeIf(roomHolderTreeItem -> !chatRoomIds.contains(roomHolderTreeItem.getValue().getRoomInfo().getId()));
	}

	public void roomJoined(long roomId)
	{
		// Must be idempotent
		moveRoom(roomId, publicRooms, subscribedRooms);
		moveRoom(roomId, privateRooms, subscribedRooms);
	}

	public void roomLeft(long roomId)
	{
		// Must be idempotent
		subscribedRooms.getChildren().stream()
				.filter(roomInfoTreeItem -> roomInfoTreeItem.getValue().getRoomInfo().getId() == roomId)
				.findFirst()
				.ifPresent(roomHolderTreeItem -> {
					subscribedRooms.getChildren().remove(roomHolderTreeItem);
					if (roomHolderTreeItem.getValue().getRoomInfo().getRoomType() == RoomType.PRIVATE)
					{
						privateRooms.getChildren().add(roomHolderTreeItem);
						sortByName(privateRooms.getChildren());
					}
					else
					{
						publicRooms.getChildren().add(roomHolderTreeItem);
						sortByName(publicRooms.getChildren());
					}
					roomHolderTreeItem.getValue().clearChatListView();
				});
	}

	private static void moveRoom(long roomId, TreeItem<RoomHolder> from, TreeItem<RoomHolder> to)
	{
		from.getChildren().stream()
				.filter(roomInfoTreeItem -> roomInfoTreeItem.getValue().getRoomInfo().getId() == roomId)
				.findFirst()
				.ifPresent(roomHolderTreeItem -> {
					from.getChildren().remove(roomHolderTreeItem);
					to.getChildren().add(roomHolderTreeItem);
					sortByName(to.getChildren());
				});
	}

	private static void sortByName(ObservableList<TreeItem<RoomHolder>> children)
	{
		children.sort((o1, o2) -> o1.getValue().getRoomInfo().getName().compareToIgnoreCase(o2.getValue().getRoomInfo().getName()));
	}

	public void userJoined(long roomId, ChatRoomUserEvent event)
	{
		performOnChatListView(roomId, chatListView -> chatListView.addUser(event, AddUserOrigin.JOIN));
	}

	public void userLeft(long roomId, ChatRoomUserEvent event)
	{
		performOnChatListView(roomId, chatListView -> chatListView.removeUser(event));
	}

	public void userKeepAlive(long roomId, ChatRoomUserEvent event)
	{
		performOnChatListView(roomId, chatListView -> chatListView.addUser(event, AddUserOrigin.KEEP_ALIVE));
	}

	public void userTimeout(long roomId, ChatRoomTimeoutEvent event)
	{
		performOnChatListView(roomId, chatListView -> chatListView.timeoutUser(event));
	}

	public void jumpToBottom()
	{
		if (selectedChatListView != null)
		{
			selectedChatListView.jumpToBottom(true);
		}
	}

	private void switchChatContent(Node contentNode, Node userListNode)
	{
		if (content.getChildren().size() > 1)
		{
			content.getChildren().removeFirst();
		}
		content.getChildren().addFirst(contentNode);

		if (userListNode == null)
		{
			if (!isEmpty(userListContent.getChildren()))
			{
				userListContent.getChildren().removeFirst();
			}
			if (splitPane.getItems().contains(userListContent))
			{
				dividerPositions = splitPane.getDividerPositions();
				splitPane.getItems().remove(userListContent);
			}
		}
		else
		{
			if (!isEmpty(userListContent.getChildren()))
			{
				userListContent.getChildren().removeFirst();
			}
			userListContent.getChildren().addFirst(userListNode);

			if (!splitPane.getItems().contains(userListContent))
			{
				splitPane.getItems().add(userListContent);
				splitPane.setDividerPositions(dividerPositions);
			}
		}
		lastTypingTimeline.jumpTo(javafx.util.Duration.INDEFINITE);
	}

	// right now I use a simple implementation. It also has a drawback that it doesn't update the counter
	private static void addOrUpdate(ObservableList<TreeItem<RoomHolder>> tree, ChatRoomInfo chatRoomInfo)
	{
		if (tree.stream()
				.map(TreeItem::getValue)
				.noneMatch(existingRoom -> existingRoom.getRoomInfo().equals(chatRoomInfo)))
		{
			tree.add(new TreeItem<>(new RoomHolder(chatRoomInfo)));
			sortByName(tree);
		}
	}

	private static boolean isInside(ObservableList<TreeItem<RoomHolder>> tree, ChatRoomInfo chatRoomInfo)
	{
		return tree.stream()
				.map(TreeItem::getValue)
				.anyMatch(roomHolder -> roomHolder.getRoomInfo().equals(chatRoomInfo));
	}

	private void changeSelectedRoom(ChatRoomInfo chatRoomInfo)
	{
		selectedRoom = chatRoomInfo;

		getSubscribedTreeItem(chatRoomInfo.getId()).ifPresentOrElse(roomInfoTreeItem -> {
			var chatListView = getChatListViewOrCreate(roomInfoTreeItem);
			selectedChatListView = chatListView;
			switchChatContent(chatListView.getChatView(), chatListView.getUserListView());
			roomName.setText(chatRoomInfo.getName());
			roomTopic.setText(chatRoomInfo.getTopic());
			status.setVisible(true);
			sendGroup.setVisible(true);
			send.requestFocus();
			selectedChatListView.jumpToBottom(true);
			setUnreadMessages(roomInfoTreeItem, false);
		}, () -> {
			chatRoomInfoController.setRoomInfo(chatRoomInfo);
			switchChatContent(roomInfoView, null);
			status.setVisible(false);
			sendGroup.setVisible(false);
			selectedChatListView = null;
		});
		nicknameCompleter.setUsernameFinder(selectedChatListView);
	}

	private boolean isRoomSelected()
	{
		return selectedRoom != null && selectedRoom.getId() != 0L;
	}

	private Optional<TreeItem<RoomHolder>> getSubscribedTreeItem(long roomId)
	{
		return subscribedRooms.getChildren().stream()
				.filter(roomHolderTreeItem -> roomHolderTreeItem.getValue().getRoomInfo().getId() == roomId)
				.findFirst();
	}

	private Optional<TreeItem<RoomHolder>> getAllTreeItem(long roomId)
	{
		return Stream.concat(subscribedRooms.getChildren().stream(), Stream.concat(publicRooms.getChildren().stream(), privateRooms.getChildren().stream()))
				.filter(roomHolderTreeItem -> roomHolderTreeItem.getValue().getRoomInfo().getId() == roomId)
				.findFirst();
	}

	public void showMessage(ChatRoomMessage chatRoomMessage)
	{
		if (chatRoomMessage.isEmpty())
		{
			if (isRoomSelected() && chatRoomMessage.getRoomId() == selectedRoom.getId())
			{
				typingNotification.setText(MessageFormat.format(bundle.getString("chat.notification.typing"), chatRoomMessage.getSenderNickname()));
				lastTypingTimeline.playFromStart();
			}
		}
		else
		{
			performOnChatListView(chatRoomMessage.getRoomId(), chatListView -> {
				chatListView.addUserMessage(chatRoomMessage.getSenderNickname(), chatRoomMessage.getGxsId(), chatRoomMessage.getContent());
				setHighlighted(chatRoomMessage.getContent());
			});
			getSubscribedTreeItem(chatRoomMessage.getRoomId()).ifPresent(roomHolderTreeItem -> {
				if (isRoomSelected() && selectedRoom.getId() != chatRoomMessage.getRoomId())
				{
					setUnreadMessages(roomHolderTreeItem, true);
				}
			});
			if (isRoomSelected() && chatRoomMessage.getRoomId() == selectedRoom.getId())
			{
				lastTypingTimeline.jumpTo(javafx.util.Duration.INDEFINITE);
			}
		}
	}

	private void setUnreadMessages(TreeItem<RoomHolder> roomHolderTreeItem, boolean unread)
	{
		roomHolderTreeItem.getValue().getRoomInfo().setNewMessages(unread);
		roomTree.refresh();
	}

	private void setHighlighted(String message)
	{
		if (message.startsWith(nickname) || message.startsWith("@" + nickname) || message.contains(" " + nickname))
		{
			trayService.setEventIfIconified();
		}
	}

	private void performOnChatListView(long roomId, Consumer<ChatListView> action)
	{
		subscribedRooms.getChildren().stream()
				.map(this::getChatListViewOrCreate)
				.filter(chatListView -> chatListView.getId() == roomId)
				.findFirst()
				.ifPresent(action);
	}

	private ChatListView getChatListViewOrCreate(TreeItem<RoomHolder> roomInfoTreeItem)
	{
		var chatListView = roomInfoTreeItem.getValue().getChatListView();
		if (chatListView == null)
		{
			chatListView = new ChatListView(nickname, roomInfoTreeItem.getValue().getRoomInfo().getId(), markdownService, uriService);
			roomInfoTreeItem.getValue().setChatListView(chatListView);
		}
		return chatListView;
	}

	private void handleInputKeys(KeyEvent event)
	{
		if (TAB_KEY.match(event))
		{
			nicknameCompleter.complete(send.getText(), send.getCaretPosition(), s -> {
				send.setText(s);
				send.positionCaret(s.length());
			});
			event.consume();
			return;
		}

		nicknameCompleter.reset();

		if (PASTE_KEY.match(event))
		{
			var image = Clipboard.getSystemClipboard().getImage();
			if (image != null)
			{
				imagePreview.setImage(image);

				ImageUtils.limitMaximumImageSize(imagePreview, PREVIEW_IMAGE_WIDTH_MAX, PREVIEW_IMAGE_HEIGHT_MAX);

				setPreviewGroupVisibility(true);
				event.consume();
			}
		}
		else if (ENTER_KEY.match(event) && imagePreview.getImage() != null)
		{
			sendImage();
			event.consume();
		}
		else if (BACKSPACE_KEY.match(event) && imagePreview.getImage() != null)
		{
			cancelImage();
			event.consume();
		}
	}

	private void sendImage()
	{
		sendChatMessage("<img src=\"" + ImageUtils.writeImageAsJpegData(imagePreview.getImage(), MESSAGE_MAXIMUM_SIZE) + "\"/>");

		resetPreviewImage();
		jumpToBottom();
	}

	private void cancelImage()
	{
		resetPreviewImage();
	}

	/**
	 * Resets the size so that smaller images aren't magnified.
	 */
	private void resetPreviewImage()
	{
		imagePreview.setImage(null);
		setPreviewGroupVisibility(false);
		imagePreview.setFitWidth(0);
		imagePreview.setFitHeight(0);
	}

	private void sendChatMessage(String message)
	{
		var chatMessage = new ChatMessage(ChatCommand.parseCommands(message));
		messageClient.sendToChatRoom(selectedRoom.getId(), chatMessage);
		selectedChatListView.addOwnMessage(chatMessage);
	}

	private void setPreviewGroupVisibility(boolean visible)
	{
		UiUtils.setPresent(previewGroup, visible);
	}

	public void openInvite(long chatRoomId, ChatRoomInviteEvent event)
	{
		Platform.runLater(() -> UiUtils.alertConfirm(MessageFormat.format(bundle.getString("chat.room.invite.request"), event.getLocationId(), event.getRoomName(), event.getRoomTopic()),
				() -> chatClient.joinChatRoom(chatRoomId).subscribe())
		);
	}
}
