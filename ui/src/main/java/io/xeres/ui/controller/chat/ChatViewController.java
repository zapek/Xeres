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

import io.xeres.common.message.chat.*;
import io.xeres.ui.client.ChatClient;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.Controller;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static io.xeres.common.message.chat.ChatConstants.TYPING_NOTIFICATION_DELAY;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@FxmlView(value = "/view/chat/chatview.fxml")
public class ChatViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(ChatViewController.class);

	@FXML
	private TreeView<RoomHolder> roomTree;

	@FXML
	private SplitPane splitPane;

	@FXML
	private VBox content;

	@FXML
	private TextField send;

	@FXML
	private VBox userListContent;

	private final MessageClient messageClient;
	private final ChatClient chatClient;
	private final ProfileClient profileClient;

	private final TreeItem<RoomHolder> subscribedRooms = new TreeItem<>(new RoomHolder("Subscribed"));
	private final TreeItem<RoomHolder> privateRooms = new TreeItem<>(new RoomHolder("Private"));
	private final TreeItem<RoomHolder> publicRooms = new TreeItem<>(new RoomHolder("Public"));

	private String nickname;

	private RoomInfo selectedRoom;
	private ChatListView selectedChatListView;
	private Node roomInfoView;
	private ChatRoomInfoController chatRoomInfoController;

	private Instant lastTypingNotification = Instant.EPOCH;

	private double[] dividerPositions;

	public ChatViewController(MessageClient messageClient, ChatClient chatClient, ProfileClient profileClient)
	{
		this.messageClient = messageClient;
		this.chatClient = chatClient;
		this.profileClient = profileClient;
	}

	public void initialize() throws IOException
	{
		profileClient.getOwnProfile().doOnSuccess(profile -> nickname = profile.getName())
				.subscribe();

		TreeItem<RoomHolder> root = new TreeItem<>(new RoomHolder());
		//noinspection unchecked
		root.getChildren().addAll(subscribedRooms, privateRooms, publicRooms);
		root.setExpanded(true);
		roomTree.setRoot(root);
		roomTree.setShowRoot(false);
		roomTree.setCellFactory(param -> {
			TreeCell<RoomHolder> cell = new TreeCell<>()
			{
				@Override
				protected void updateItem(RoomHolder roomHolder, boolean empty)
				{
					super.updateItem(roomHolder, empty);
					if (empty)
					{
						setText(null);
					}
					else
					{
						setText(roomHolder.getRoomInfo().getName());
					}
				}
			};
			var cm = createRoomContextMenu(cell);
			cell.setContextMenu(cm);
			return cell;
		});

		// We need Platform.runLater() because when an entry is moved, the selection can change
		roomTree.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> Platform.runLater(() -> changeSelectedRoom(newValue.getValue().getRoomInfo())));

		roomTree.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2 && selectedRoom != null && selectedRoom.getId() != 0)
			{
				chatClient.joinChatRoom(selectedRoom.getId())
						.subscribe(); // XXX: only do that if we're not already subscribed
			}
		});

		send.setOnKeyPressed(event ->
		{
			if (selectedRoom != null && selectedRoom.getId() != 0)
			{
				if (event.getCode().equals(KeyCode.ENTER) && isNotBlank(send.getText()))
				{
					var chatMessage = new ChatMessage(send.getText());
					messageClient.sendToChatRoom(selectedRoom.getId(), chatMessage);
					selectedChatListView.addMessage(send.getText());
					send.clear();
				}
				else
				{
					var now = Instant.now();
					if (Duration.between(lastTypingNotification, now).compareTo(TYPING_NOTIFICATION_DELAY) > 0)
					{
						var chatMessage = new ChatMessage();
						messageClient.sendToChatRoom(selectedRoom.getId(), chatMessage);
						lastTypingNotification = now;
					}
				}
			}
		});

		var loader = new FXMLLoader(getClass().getResource("/view/chat/chat_roominfo.fxml"));
		roomInfoView = loader.load();
		chatRoomInfoController = loader.getController();

		VBox.setVgrow(roomInfoView, Priority.ALWAYS);
		switchChatContent(roomInfoView, null);
		send.setVisible(false);
	}

	private ContextMenu createRoomContextMenu(TreeCell<RoomHolder> cell)
	{
		// XXX: isn't there a better way to find which object the context menu is called upon?
		var cm = new ContextMenu();

		var subscribeItem = new MenuItem("Join");
		subscribeItem.setOnAction(event -> {
			var roomInfo = cell.getTreeItem().getValue().getRoomInfo();
			boolean found = subscribedRooms.getChildren().stream()
					.noneMatch(roomHolderTreeItem -> roomHolderTreeItem.getValue().getRoomInfo().equals(roomInfo));

			if (!found)
			{
				chatClient.joinChatRoom(selectedRoom.getId())
						.subscribe(); // XXX: only do that if we're not already subscribed. also doOnSuccess(), etc... sometimes the id of the roomInfo is wrong... of course! because we set the context menu BEFORE the room id is refreshed into it
			}
		});

		var unsubscribeItem = new MenuItem("Leave");
		unsubscribeItem.setOnAction(event -> {
			var roomInfo = cell.getTreeItem().getValue().getRoomInfo();
			subscribedRooms.getChildren().stream()
					.filter(roomHolderTreeItem -> roomHolderTreeItem.getValue().getRoomInfo().equals(roomInfo))
					.findAny()
					.ifPresent(roomHolderTreeItem -> chatClient.leaveChatRoom(roomInfo.getId())
							.subscribe());
		});
		cm.getItems().addAll(subscribeItem, unsubscribeItem);
		return cm;
	}

	public void addRooms(List<RoomInfo> newRooms)
	{
		ObservableList<TreeItem<RoomHolder>> subscribedTree = subscribedRooms.getChildren();
		ObservableList<TreeItem<RoomHolder>> publicTree = publicRooms.getChildren();
		ObservableList<TreeItem<RoomHolder>> privateTree = privateRooms.getChildren();

		// Make sure we don't add rooms that we're already subscribed to
		List<RoomInfo> unsubscribedRooms = newRooms.stream()
				.filter(roomInfo -> !isInside(subscribedTree, roomInfo))
				.toList();

		unsubscribedRooms.stream()
				.filter(roomInfo -> roomInfo.getRoomType() == RoomType.PUBLIC)
				.sorted(Comparator.comparing(RoomInfo::getName))
				.forEach(roomInfo -> addOrUpdate(publicTree, roomInfo));

		unsubscribedRooms.stream()
				.filter(roomInfo -> roomInfo.getRoomType() == RoomType.PRIVATE)
				.sorted(Comparator.comparing(RoomInfo::getName))
				.forEach(roomInfo -> addOrUpdate(privateTree, roomInfo));
	}

	public void roomJoined(long roomId)
	{
		// Must be idempotent
		publicRooms.getChildren().stream()
				.filter(roomInfoTreeItem -> roomInfoTreeItem.getValue().getRoomInfo().getId() == roomId)
				.findFirst()
				.ifPresent(roomHolderTreeItem -> {
					publicRooms.getChildren().remove(roomHolderTreeItem);
					subscribedRooms.getChildren().add(roomHolderTreeItem);
				});
		// XXX: also do it for private
	}

	public void roomLeft(long roomId)
	{
		// Must be idempotent
		subscribedRooms.getChildren().stream()
				.filter(roomInfoTreeItem -> roomInfoTreeItem.getValue().getRoomInfo().getId() == roomId)
				.findFirst()
				.ifPresent(roomHolderTreeItem -> {
					subscribedRooms.getChildren().remove(roomHolderTreeItem);
					publicRooms.getChildren().add(roomHolderTreeItem); // XXX: could be public too...
					roomHolderTreeItem.getValue().clearChatListView();
				});
		// XXX: remove ourselves from the room
	}

	public void userJoined(long roomId, ChatRoomUserEvent event)
	{
		performOnChatListView(roomId, chatListView -> chatListView.addUser(event));
	}

	public void userLeft(long roomId, ChatRoomUserEvent event)
	{
		performOnChatListView(roomId, chatListView -> chatListView.removeUser(event));
	}

	public void userKeepAlive(long roomId, ChatRoomUserEvent event)
	{
		performOnChatListView(roomId, chatListView -> chatListView.addUser(event)); // XXX: use this to know if a user is "idle"
	}

	private void switchChatContent(Node contentNode, Node userListNode)
	{
		if (content.getChildren().size() > 1)
		{
			content.getChildren().remove(0);
		}
		content.getChildren().add(0, contentNode);

		if (userListNode == null)
		{
			if (!isEmpty(userListContent.getChildren()))
			{
				userListContent.getChildren().remove(0);
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
				userListContent.getChildren().remove(0);
			}
			userListContent.getChildren().add(0, userListNode);

			if (!splitPane.getItems().contains(userListContent))
			{
				splitPane.getItems().add(userListContent);
				splitPane.setDividerPositions(dividerPositions);
			}
		}
	}

	// XXX: also we should merge/refresh... (ie. new rooms added, older rooms removed, etc...). merging properly is very difficult it seems
	// right now I use a simple implementation. It also has a drawback that it doesn't sort new entries and doesn't update the counter
	private void addOrUpdate(ObservableList<TreeItem<RoomHolder>> tree, RoomInfo roomInfo)
	{
		if (tree.stream()
				.map(TreeItem::getValue)
				.noneMatch(existingRoom -> existingRoom.getRoomInfo().equals(roomInfo)))
		{
			tree.add(new TreeItem<>(new RoomHolder(roomInfo)));
		}
	}

	private boolean isInside(ObservableList<TreeItem<RoomHolder>> tree, RoomInfo roomInfo)
	{
		return tree.stream()
				.map(TreeItem::getValue)
				.anyMatch(roomHolder -> roomHolder.getRoomInfo().equals(roomInfo));
	}

	private void changeSelectedRoom(RoomInfo roomInfo)
	{
		selectedRoom = roomInfo;

		Optional<TreeItem<RoomHolder>> roomTreeItem = subscribedRooms.getChildren().stream()
				.filter(roomInfoTreeItem -> roomInfoTreeItem.getValue().getRoomInfo().equals(roomInfo))
				.findFirst();

		if (roomTreeItem.isPresent())
		{
			TreeItem<RoomHolder> roomInfoTreeItem = roomTreeItem.get();
			var chatListView = getChatListViewOrCreate(roomInfoTreeItem);
			selectedChatListView = chatListView;
			switchChatContent(chatListView.getChatView(), chatListView.getUserListView());
			send.setVisible(true);
		}
		else
		{
			chatRoomInfoController.setRoomInfo(roomInfo);
			switchChatContent(roomInfoView, null);
			send.setVisible(false);
			selectedChatListView = null;
		}
	}

	public void showMessage(ChatRoomMessage chatRoomMessage)
	{
		if (chatRoomMessage.isEmpty())
		{
			// XXX: show a typing notification somewhere
		}
		else
		{
			performOnChatListView(chatRoomMessage.getRoomId(), chatListView -> chatListView.addMessage(chatRoomMessage.getSenderNickname(), chatRoomMessage.getContent()));
		}
	}

	// XXX: concurrent modification. it happens because some events come from the STOMP thread and others from the service
	private void performOnChatListView(long roomId, Consumer<ChatListView> action)
	{
		subscribedRooms.getChildren().stream()
				.map(this::getChatListViewOrCreate)
				.filter(chatListView -> chatListView.getRoomInfo().getId() == roomId)
				.findFirst()
				.ifPresent(action);
	}

	private ChatListView getChatListViewOrCreate(TreeItem<RoomHolder> roomInfoTreeItem)
	{
		var chatListView = roomInfoTreeItem.getValue().getChatListView();
		if (chatListView == null)
		{
			chatListView = new ChatListView(nickname, roomInfoTreeItem.getValue().getRoomInfo());
			roomInfoTreeItem.getValue().setChatListView(chatListView);
		}
		return chatListView;
	}
}
