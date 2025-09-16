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

import io.xeres.common.id.Sha1Sum;
import io.xeres.common.message.chat.*;
import io.xeres.ui.OpenUriEvent;
import io.xeres.ui.client.*;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.chat.ChatListView.AddUserOrigin;
import io.xeres.ui.custom.InputAreaGroup;
import io.xeres.ui.custom.TypingNotificationView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.custom.event.FileSelectedEvent;
import io.xeres.ui.custom.event.ImageSelectedEvent;
import io.xeres.ui.custom.event.StickerSelectedEvent;
import io.xeres.ui.support.chat.ChatCommand;
import io.xeres.ui.support.chat.NicknameCompleter;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.preference.PreferenceUtils;
import io.xeres.ui.support.sound.SoundService;
import io.xeres.ui.support.sound.SoundService.SoundType;
import io.xeres.ui.support.tray.TrayService;
import io.xeres.ui.support.uri.ChatRoomUri;
import io.xeres.ui.support.uri.FileUriFactory;
import io.xeres.ui.support.uri.UriService;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.util.image.ImageUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.xeres.common.message.chat.ChatConstants.TYPING_NOTIFICATION_DELAY;
import static io.xeres.ui.support.preference.PreferenceUtils.CHAT_ROOMS;
import static javafx.scene.control.Alert.AlertType.WARNING;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@FxmlView(value = "/view/chat/chatview.fxml")
public class ChatViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(ChatViewController.class);

	private static final int PREVIEW_IMAGE_WIDTH_MAX = 320;
	private static final int PREVIEW_IMAGE_HEIGHT_MAX = 240;

	private static final int STICKER_WIDTH_MAX = 192;
	private static final int STICKER_HEIGHT_MAX = 192;

	private static final int MESSAGE_MAXIMUM_SIZE = 31000; // XXX: put that on chat service too as we shouldn't forward them. also this is only for chat rooms, not private chats
	private static final KeyCodeCombination TAB_KEY = new KeyCodeCombination(KeyCode.TAB);
	private static final KeyCodeCombination PASTE_KEY = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
	private static final KeyCodeCombination COPY_KEY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
	private static final KeyCodeCombination CTRL_ENTER = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN);
	private static final KeyCodeCombination SHIFT_ENTER = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN);
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
	private InputAreaGroup send;

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
	private final GeneralClient generalClient;
	private final ImageCache imageCache;
	private final SoundService soundService;
	private final ShareClient shareClient;

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

	public ChatViewController(MessageClient messageClient, ChatClient chatClient, ProfileClient profileClient, LocationClient locationClient, WindowManager windowManager, TrayService trayService, ResourceBundle bundle, MarkdownService markdownService, UriService uriService, GeneralClient generalClient, ImageCache imageCache, SoundService soundService, ShareClient shareClient)
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
		this.generalClient = generalClient;
		this.imageCache = imageCache;
		this.soundService = soundService;
		this.shareClient = shareClient;

		subscribedRooms = new TreeItem<>(new RoomHolder(bundle.getString("chat.room.subscribed")));
		privateRooms = new TreeItem<>(new RoomHolder(bundle.getString("enum.roomtype.private")));
		publicRooms = new TreeItem<>(new RoomHolder(bundle.getString("enum.roomtype.public")));
	}

	@Override
	public void initialize()
	{
		profileClient.getOwn().doOnSuccess(profile -> Platform.runLater(() -> initializeReally(profile.getName())))
				.subscribe();
	}

	private void initializeReally(String nickname)
	{
		this.nickname = nickname;

		var root = new TreeItem<>(new RoomHolder());
		//noinspection unchecked
		root.getChildren().addAll(subscribedRooms, privateRooms, publicRooms);
		root.setExpanded(true);
		roomTree.setRoot(root);
		roomTree.setShowRoot(false);
		roomTree.setCellFactory(_ -> new ChatRoomCell());
		createRoomTreeContextMenu();

		// We need Platform.runLater() because when an entry is moved, the selection can change
		roomTree.getSelectionModel().selectedItemProperty()
				.addListener((_, _, newValue) -> Platform.runLater(() -> changeSelectedRoom(newValue.getValue().getRoomInfo())));

		UiUtils.setOnPrimaryMouseDoubleClicked(roomTree, _ -> {
			if (isRoomSelected())
			{
				joinChatRoom(selectedRoom);
			}
		});

		var loader = new FXMLLoader(getClass().getResource("/view/chat/chat_roominfo.fxml"), bundle);
		try
		{
			roomInfoView = loader.load();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		chatRoomInfoController = loader.getController();

		lastTypingTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(TYPING_NOTIFICATION_DELAY.getSeconds())));
		lastTypingTimeline.setOnFinished(_ -> typingNotification.setText(""));

		VBox.setVgrow(roomInfoView, Priority.ALWAYS);
		switchChatContent(roomInfoView, null);
		sendGroup.setVisible(false);
		setPreviewGroupVisibility(false);

		previewSend.setOnAction(_ -> sendImage());
		previewCancel.setOnAction(_ -> cancelImage());

		// Handle the events even if the InputArea widget isn't selected
		content.addEventHandler(KeyEvent.KEY_PRESSED, this::handleInputKeys);

		send.addKeyFilter(this::handleInputKeys);
		send.addEnhancedContextMenu(this::handlePaste, locationClient);

		send.addEventHandler(StickerSelectedEvent.STICKER_SELECTED, event -> CompletableFuture.runAsync(() -> {
			try
			{
				var bufferedImage = ImageIO.read(event.getPath().toFile());
				Platform.runLater(() -> sendStickerToMessageOptimized(bufferedImage));
			}
			catch (IOException e)
			{
				log.error("Couldn't send the sticker: {}", e.getMessage());
			}
		}));

		send.addEventHandler(ImageSelectedEvent.IMAGE_SELECTED, event -> {
			if (event.getFile().canRead())
			{
				CompletableFuture.runAsync(() -> {
					try (var inputStream = new FileInputStream(event.getFile()))
					{
						var image = new Image(inputStream);
						Platform.runLater(() -> setPreviewImage(image));
					}
					catch (IOException e)
					{
						UiUtils.alert(Alert.AlertType.ERROR, MessageFormat.format(bundle.getString("file-requester.error"), event.getFile(), e.getMessage()));
					}
				});
			}
		});

		send.addEventHandler(FileSelectedEvent.FILE_SELECTED, event -> {
			if (event.getFile().canRead())
			{
				sendFile(event.getFile());
			}
		});

		invite.setOnAction(_ -> windowManager.openInvite(selectedRoom.getId()));

		getChatRoomContext();

		createChatRoom.setOnAction(_ -> windowManager.openChatRoomCreation());

		setupTrees();
	}

	private void sendFile(File file)
	{
		shareClient.createTemporaryShare(file.getAbsolutePath())
				.doOnSuccess(result -> sendChatMessage(FileUriFactory.generate(file.getName(), getFileSize(file.toPath()), Sha1Sum.fromString(result.hash()))))
				.subscribe();
	}

	// XXX: duplicate..
	private static long getFileSize(Path path)
	{
		try
		{
			return Files.size(path);
		}
		catch (IOException _)
		{
			log.error("Failed to get the file size of {}", path);
			return 0;
		}
	}

	private void setupTrees()
	{
		var node = PreferenceUtils.getPreferences().node(CHAT_ROOMS);
		subscribedRooms.setExpanded(node.getBoolean(OPEN_SUBSCRIBED, false));
		privateRooms.setExpanded(node.getBoolean(OPEN_PRIVATE, false));
		publicRooms.setExpanded(node.getBoolean(OPEN_PUBLIC, false));

		subscribedRooms.expandedProperty().addListener((_, _, newValue) -> node.putBoolean(OPEN_SUBSCRIBED, newValue));
		privateRooms.expandedProperty().addListener((_, _, newValue) -> node.putBoolean(OPEN_PRIVATE, newValue));
		publicRooms.expandedProperty().addListener((_, _, newValue) -> node.putBoolean(OPEN_PUBLIC, newValue));
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
		var subscribeItem = new MenuItem(bundle.getString("chat.room.join"));
		subscribeItem.setId(SUBSCRIBED_MENU_ID);
		subscribeItem.setGraphic(new FontIcon(MaterialDesignL.LOCATION_ENTER));
		subscribeItem.setOnAction(event -> joinChatRoom(((RoomHolder) event.getSource()).getRoomInfo()));

		var unsubscribeItem = new MenuItem(bundle.getString("chat.room.leave"));
		unsubscribeItem.setId(UNSUBSCRIBED_MENU_ID);
		unsubscribeItem.setGraphic(new FontIcon(MaterialDesignL.LOCATION_EXIT));
		unsubscribeItem.setOnAction(event -> leaveChatRoom(((RoomHolder) event.getSource()).getRoomInfo()));

		var copyLinkItem = new MenuItem(bundle.getString("copy-link"));
		copyLinkItem.setId(COPY_LINK_MENU_ID);
		copyLinkItem.setGraphic(new FontIcon(MaterialDesignL.LINK_VARIANT));
		copyLinkItem.setOnAction(event -> {
			var chatRoomInfo = ((RoomHolder) event.getSource()).getRoomInfo();
			ClipboardUtils.copyTextToClipboard(new ChatRoomUri(chatRoomInfo.getName(), chatRoomInfo.getId()).toUriString());
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
				.ifPresent(_ -> chatClient.leaveChatRoom(chatRoomInfo.getId())
						.subscribe());
	}

	private void getChatRoomContext()
	{
		chatClient.getChatRoomContext()
				.doOnSuccess(context -> {
					addRooms(context.chatRoomLists());
					context.chatRoomLists().getSubscribed().forEach(chatRoomInfo -> userJoined(chatRoomInfo.getId(), new ChatRoomUserEvent(context.ownUser().gxsId(), context.ownUser().nickname(), context.ownUser().identityId())));
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
				if (chatRoomMessage.isOwn())
				{
					chatListView.addOwnMessage(chatRoomMessage);
				}
				else
				{
					chatListView.addUserMessage(chatRoomMessage.getSenderNickname(), chatRoomMessage.getGxsId(), chatRoomMessage.getContent());
					setHighlighted(chatRoomMessage.getContent());
				}
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
			soundService.play(SoundType.HIGHLIGHT);
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
			var chatRoomId = roomInfoTreeItem.getValue().getRoomInfo().getId();
			chatListView = new ChatListView(nickname, chatRoomId, markdownService, uriService, generalClient, imageCache, windowManager, send);
			chatListView.installClearHistoryContextMenu(() -> chatClient.deleteChatRoomBacklog(chatRoomId)
					.subscribe());
			var finalChatListView = chatListView;
			chatClient.getChatRoomBacklog(chatRoomId).collectList()
					.doOnSuccess(backlogs -> Platform.runLater(() -> fillBacklog(finalChatListView, backlogs)))
					.subscribe();
			roomInfoTreeItem.getValue().setChatListView(chatListView);
		}
		return chatListView;
	}

	private void fillBacklog(ChatListView chatListView, List<ChatRoomBacklog> messages)
	{
		messages.forEach(message -> {
			if (message.gxsId() == null)
			{
				chatListView.addOwnMessage(message.created(), message.message());
			}
			else
			{
				chatListView.addUserMessage(message.created(), message.nickname(), message.gxsId(), message.message());
			}
		});
		chatListView.jumpToBottom(true);
	}

	private void handleInputKeys(KeyEvent event)
	{
		if (TAB_KEY.match(event))
		{
			nicknameCompleter.complete(send.getTextInputControl().getText(), send.getTextInputControl().getCaretPosition(), s -> {
				send.getTextInputControl().setText(s);
				send.getTextInputControl().positionCaret(s.length());
			});
			event.consume();
			return;
		}

		nicknameCompleter.reset();

		if (PASTE_KEY.match(event))
		{
			if (handlePaste(send.getTextInputControl()))
			{
				event.consume();
			}
		}
		else if (COPY_KEY.match(event))
		{
			if (selectedChatListView != null && selectedChatListView.copy())
			{
				event.consume();
			}
		}
		else if (CTRL_ENTER.match(event) || SHIFT_ENTER.match(event) && isNotBlank(send.getTextInputControl().getText()))
		{
			send.getTextInputControl().insertText(send.getTextInputControl().getCaretPosition(), "\n");
			sendTypingNotificationIfNeeded();
			event.consume();
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
		else if (event.getCode() == KeyCode.ENTER)
		{
			if (isRoomSelected() && isNotBlank(send.getTextInputControl().getText()))
			{
				sendChatMessage(send.getTextInputControl().getText());
				send.clear();
				lastTypingNotification = Instant.EPOCH;
			}
			event.consume();
		}
		else
		{
			sendTypingNotificationIfNeeded();
		}
	}

	private void sendTypingNotificationIfNeeded()
	{
		var now = Instant.now();
		if (Duration.between(lastTypingNotification, now).compareTo(TYPING_NOTIFICATION_DELAY.minusSeconds(1)) > 0)
		{
			var chatMessage = new ChatMessage();
			messageClient.sendToChatRoom(selectedRoom.getId(), chatMessage);
			lastTypingNotification = now;
		}
	}

	private boolean handlePaste(TextInputControl textInputControl)
	{
		var object = ClipboardUtils.getSupportedObjectFromClipboard();
		return switch (object)
		{
			case Image image ->
			{
				setPreviewImage(image);
				yield true;
			}
			case String string ->
			{
				textInputControl.insertText(textInputControl.getCaretPosition(), string);
				yield true;
			}
			default -> false;
		};
	}

	private void sendImage()
	{
		sendChatMessage("<img src=\"" + ImageUtils.writeImageAsJpegData(imagePreview.getImage(), MESSAGE_MAXIMUM_SIZE) + "\"/>");

		resetPreviewImage();
		jumpToBottom();
	}

	private void sendStickerToMessageOptimized(BufferedImage image)
	{
		image = ImageUtils.limitMaximumImageSize(image, STICKER_WIDTH_MAX * STICKER_HEIGHT_MAX);
		sendChatMessage("<img src=\"" + ImageUtils.writeImageAsPngData(image, MESSAGE_MAXIMUM_SIZE) + "\"/>");
	}

	private void cancelImage()
	{
		resetPreviewImage();
	}

	private void setPreviewImage(Image image)
	{
		imagePreview.setImage(image);

		ImageUtils.limitMaximumImageSize(imagePreview, PREVIEW_IMAGE_WIDTH_MAX * PREVIEW_IMAGE_HEIGHT_MAX);

		setPreviewGroupVisibility(true);
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
	}

	private void setPreviewGroupVisibility(boolean visible)
	{
		UiUtils.setPresent(previewGroup, visible);
	}

	public void openInvite(long chatRoomId, ChatRoomInviteEvent event)
	{
		Platform.runLater(() -> UiUtils.alertConfirm(MessageFormat.format(bundle.getString("chat.room.invite.request"), event.getLocationIdentifier(), event.getRoomName(), event.getRoomTopic()),
				() -> chatClient.joinChatRoom(chatRoomId).subscribe())
		);
	}
}
