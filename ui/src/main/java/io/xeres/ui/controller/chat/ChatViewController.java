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

import io.xeres.common.dto.identity.IdentityConstants;
import io.xeres.common.message.chat.*;
import io.xeres.ui.client.ChatClient;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.Controller;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
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

	private static final int IMAGE_WIDTH_MAX = 640;
	private static final int IMAGE_HEIGHT_MAX = 480;
	private static final int IMAGE_MAXIMUM_SIZE = 196000; // XXX: maximum size for normal messages?
	private static final int MESSAGE_MAXIMUM_SIZE = 31000; // XXX: put that on chat service too as we shouldn't forward them. also this is only for chat rooms, not private chats

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
	private Label typingNotification;

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

	private Timeline lastTypingTimeline;

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
		roomTree.setCellFactory(ChatRoomCell::new);
		roomTree.addEventHandler(ChatRoomContextMenu.JOIN, event -> joinChatRoom(event.getTreeItem().getValue().getRoomInfo()));
		roomTree.addEventHandler(ChatRoomContextMenu.LEAVE, event -> {
			var roomInfo = event.getTreeItem().getValue().getRoomInfo();
			subscribedRooms.getChildren().stream()
					.filter(roomHolderTreeItem -> roomHolderTreeItem.getValue().getRoomInfo().equals(roomInfo))
					.findAny()
					.ifPresent(roomHolderTreeItem -> chatClient.leaveChatRoom(roomInfo.getId())
							.subscribe());
		});

		// We need Platform.runLater() because when an entry is moved, the selection can change
		roomTree.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> Platform.runLater(() -> changeSelectedRoom(newValue.getValue().getRoomInfo())));

		roomTree.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2 && selectedRoom != null && selectedRoom.getId() != 0)
			{
				joinChatRoom(selectedRoom);
			}
		});

		send.setOnKeyPressed(event ->
		{
			if (selectedRoom != null && selectedRoom.getId() != 0)
			{
				if (event.getCode().equals(KeyCode.ENTER) && isNotBlank(send.getText()))
				{
					sendChatMessage(send.getText());
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
		sendGroup.setVisible(false);
		setPreviewGroupVisibility(false);

		previewSend.setOnAction(event -> sendImage());
		previewCancel.setOnAction(event -> cancelImage());

		lastTypingTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(5),
				ae -> typingNotification.setText("")));

		send.addEventHandler(KeyEvent.KEY_PRESSED, this::handleInputKeys);
	}

	private void joinChatRoom(RoomInfo roomInfo)
	{
		boolean found = subscribedRooms.getChildren().stream()
				.anyMatch(roomHolderTreeItem -> roomHolderTreeItem.getValue().getRoomInfo().equals(roomInfo));

		if (!found)
		{
			chatClient.joinChatRoom(selectedRoom.getId())
					.subscribe(); // XXX: sometimes the id of the roomInfo is wrong... of course! because we set the context menu BEFORE the room id is refreshed into it
		}
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
		typingNotification.setText("");
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
			sendGroup.setVisible(true);
		}
		else
		{
			chatRoomInfoController.setRoomInfo(roomInfo);
			switchChatContent(roomInfoView, null);
			sendGroup.setVisible(false);
			selectedChatListView = null;
		}
	}

	public void showMessage(ChatRoomMessage chatRoomMessage)
	{
		if (chatRoomMessage.isEmpty())
		{
			if (chatRoomMessage.getRoomId() == selectedRoom.getId())
			{
				typingNotification.setText(chatRoomMessage.getSenderNickname() + " is typing...");
				lastTypingTimeline.playFromStart();
			}
		}
		else
		{
			performOnChatListView(chatRoomMessage.getRoomId(), chatListView -> chatListView.addMessage(chatRoomMessage.getSenderNickname(), chatRoomMessage.getContent()));
		}
	}

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

	private final KeyCodeCombination TAB_KEY = new KeyCodeCombination(KeyCode.TAB);
	private final KeyCodeCombination PASTE_KEY = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
	private final KeyCodeCombination ENTER_KEY = new KeyCodeCombination(KeyCode.ENTER);
	private final KeyCodeCombination BACKSPACE_KEY = new KeyCodeCombination(KeyCode.BACK_SPACE);
	private int completionIndex;

	private void handleInputKeys(KeyEvent event)
	{
		if (TAB_KEY.match(event))
		{
			var line = send.getText();
			String suggestedNickname = null;
			if (line.length() == 0)
			{
				// empty line, insert the first nickname
				suggestedNickname = selectedChatListView.getUsername("", completionIndex);
			}
			else
			{
				if (send.getCaretPosition() <= IdentityConstants.NAME_LENGTH_MAX)
				{
					int separator = line.indexOf(":");
					if (separator == -1)
					{
						separator = line.length();
					}
					suggestedNickname = selectedChatListView.getUsername(line.substring(0, separator), completionIndex);
				}
			}
			if (suggestedNickname != null)
			{
				send.setText(suggestedNickname + ": ");
				send.positionCaret(suggestedNickname.length() + 2);
			}
			completionIndex++;
			event.consume(); // XXX: find a way to tab into another field (shift + tab currently does)
		}
		else if (PASTE_KEY.match(event))
		{
			Image image = Clipboard.getSystemClipboard().getImage();
			if (image != null)
			{
				imagePreview.setImage(image);

				limitMaximumImageSize(imagePreview);

				setPreviewGroupVisibility(true);
				event.consume();
			}
		}
		else if (ENTER_KEY.match(event))
		{
			if (imagePreview.getImage() != null)
			{
				sendImage();
				event.consume();
			}
		}
		else if (BACKSPACE_KEY.match(event))
		{
			if (imagePreview.getImage() != null)
			{
				cancelImage();
				event.consume();
			}
		}
		else
		{
			completionIndex = 0;
		}
	}

	private void sendImage()
	{
		sendChatMessage("<img src=\"" + writeImageAsJpegData(imagePreview.getImage()) + "\"/>");

		imagePreview.setImage(null);
		setPreviewGroupVisibility(false);

		// Reset the size so that smaller images aren't magnified
		imagePreview.setFitWidth(0);
		imagePreview.setFitHeight(0);
	}

	private void cancelImage()
	{
		imagePreview.setImage(null);
		setPreviewGroupVisibility(false);
	}

	private void sendChatMessage(String message)
	{
		var chatMessage = new ChatMessage(message);
		messageClient.sendToChatRoom(selectedRoom.getId(), chatMessage);
		selectedChatListView.addMessage(message);
		send.clear();
	}

	private void setPreviewGroupVisibility(boolean visible)
	{
		previewGroup.setVisible(visible);
		previewGroup.setManaged(visible);
	}

	private static String writeImageAsPngData(Image image)
	{
		var out = new ByteArrayOutputStream();
		try
		{
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "PNG", out);
			if (out.size() > IMAGE_MAXIMUM_SIZE) // XXX: this size might be exceeded frequently. also we don't check for the maximum size. do like the jpeg version
			{
				log.warn("PNG size too big: {}, expect problems", out.size());
			}
		}
		catch (IOException e)
		{
			log.error("Couldn't save image as PNG: {}", e.getMessage());
		}
		return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
	}

	private static String writeImageAsJpegData(Image image)
	{
		try
		{
			byte[] out;
			var quality = 0.7f;
			BufferedImage bufferedImage = stripAlphaIfNeeded(SwingFXUtils.fromFXImage(image, null));
			do
			{
				out = compressBufferedImageToJpegArray(bufferedImage, quality);
				quality -= 0.1;
			}
			while (Math.ceil((double) out.length / 3) * 4 > MESSAGE_MAXIMUM_SIZE - 200 && quality > 0); // 200 bytes to be safe as the message might contain tags and so on

			return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(out);
		}
		catch (IOException e)
		{
			log.error("Couldn't save image as JPEG: {}", e.getMessage());
			return "";
		}
	}

	private static byte[] compressBufferedImageToJpegArray(BufferedImage image, float quality) throws IOException
	{
		var jpegWriter = ImageIO.getImageWritersByFormatName("JPEG").next();
		var jpegWriteParam = jpegWriter.getDefaultWriteParam();
		jpegWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpegWriteParam.setCompressionQuality(quality);

		var out = new ByteArrayOutputStream();

		ImageOutputStream ios = ImageIO.createImageOutputStream(out);
		jpegWriter.setOutput(ios);
		IIOImage outputImage = new IIOImage(image, null, null);
		jpegWriter.write(null, outputImage, jpegWriteParam);
		byte[] result = out.toByteArray();
		jpegWriter.dispose();
		return result;
	}

	private static BufferedImage stripAlphaIfNeeded(BufferedImage originalImage)
	{
		if (originalImage.getTransparency() == Transparency.OPAQUE)
		{
			return originalImage;
		}

		int w = originalImage.getWidth();
		int h = originalImage.getHeight();
		BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		int[] rgb = originalImage.getRGB(0, 0, w, h, null, 0, w);
		newImage.setRGB(0, 0, w, h, rgb, 0, w);
		return newImage;
	}

	private static void limitMaximumImageSize(ImageView imageView)
	{
		var width = imageView.getImage().getWidth();
		var height = imageView.getImage().getHeight();

		if (width > IMAGE_WIDTH_MAX || height > IMAGE_HEIGHT_MAX)
		{
			ImageView scaleImageView = new ImageView(imageView.getImage());
			if (width > height)
			{
				scaleImageView.setFitWidth(IMAGE_WIDTH_MAX);
			}
			else
			{
				scaleImageView.setFitHeight(IMAGE_HEIGHT_MAX);
			}
			scaleImageView.setPreserveRatio(true);
			scaleImageView.setSmooth(true);
			imageView.setImage(scaleImageView.snapshot(null, null));
		}
	}
}
