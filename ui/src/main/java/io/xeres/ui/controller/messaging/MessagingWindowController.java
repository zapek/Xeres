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

package io.xeres.ui.controller.messaging;

import io.xeres.common.id.LocationId;
import io.xeres.common.message.chat.ChatAvatar;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.controller.chat.ChatListView;
import io.xeres.ui.custom.TypingNotificationView;
import io.xeres.ui.model.profile.Profile;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.util.ImageUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ResourceBundle;

import static io.xeres.common.message.chat.ChatConstants.TYPING_NOTIFICATION_DELAY;
import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;
import static io.xeres.ui.support.util.UiUtils.getWindow;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@FxmlView(value = "/view/messaging/messaging.fxml")
public class MessagingWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(MessagingWindowController.class);

	private static final int IMAGE_WIDTH_MAX = 640;
	private static final int IMAGE_HEIGHT_MAX = 480;
	private static final int MESSAGE_MAXIMUM_SIZE = 196000; // XXX: maximum size for normal messages? check if correct

	private static final KeyCodeCombination PASTE_KEY = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);

	@FXML
	private TextField send;

	@FXML
	private TypingNotificationView notification;

	@FXML
	private VBox content;

	@FXML
	private ImageView ownAvatar;

	@FXML
	private ImageView targetAvatar;

	private ChatListView receive;

	private final ProfileClient profileClient;
	private final MarkdownService markdownService;
	private final ResourceBundle bundle;
	private final LocationId locationId;
	private Profile targetProfile;

	private final MessageClient messageClient;

	private Instant lastTypingNotification = Instant.EPOCH;

	private Timeline lastTypingTimeline;

	public MessagingWindowController(ProfileClient profileClient, MessageClient messageClient, MarkdownService markdownService, String locationId, ResourceBundle bundle)
	{
		this.profileClient = profileClient;
		this.messageClient = messageClient;
		this.markdownService = markdownService;
		this.bundle = bundle;
		this.locationId = new LocationId(locationId);
	}

	@Override
	public void initialize()
	{
		var ownProfileResult = profileClient.getOwn();
		ownProfileResult.doOnSuccess(profile -> Platform.runLater(() -> setupChatListView(profile.getName(), profile.getId())))
				.subscribe();

		send.setOnKeyPressed(event ->
		{
			if (event.getCode().equals(KeyCode.ENTER) && isNotBlank(send.getText()))
			{
				sendMessage(send.getText());
				lastTypingNotification = Instant.EPOCH;
			}
			else
			{
				var now = Instant.now();
				if (java.time.Duration.between(lastTypingNotification, now).compareTo(TYPING_NOTIFICATION_DELAY.minusSeconds(1)) > 0)
				{
					var message = new ChatMessage();
					messageClient.sendToLocation(locationId, message);
					lastTypingNotification = now;
				}
			}
		});

		send.addEventHandler(KeyEvent.KEY_PRESSED, this::handleInputKeys);

		lastTypingTimeline = new Timeline(
				new KeyFrame(Duration.ZERO, event -> notification.setText(MessageFormat.format(bundle.getString("chat.notification.typing"), targetProfile.getName()))),
				new KeyFrame(Duration.seconds(TYPING_NOTIFICATION_DELAY.getSeconds())));
		lastTypingTimeline.setOnFinished(event -> notification.setText(""));
	}

	private void sendMessage(String message)
	{
		var chatMessage = new ChatMessage(message);
		messageClient.sendToLocation(locationId, chatMessage);
		receive.addOwnMessage(message);
		send.clear();
	}

	private void setupChatListView(String nickname, long id)
	{
		receive = new ChatListView(nickname, id, markdownService);
		content.getChildren().addFirst(receive.getChatView());
		content.setOnDragOver(event -> {
			if (event.getDragboard().hasFiles())
			{
				event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			event.consume();
		});
		content.setOnDragDropped(event -> {
			var files = event.getDragboard().getFiles();
			CollectionUtils.emptyIfNull(files).forEach(file -> log.debug("File dropped: {}", file.getName())); // XXX: do something more useful
			event.setDropCompleted(true);
			event.consume();
		});
	}

	@Override
	public void onShown()
	{
		profileClient.findByLocationId(locationId).collectList()
				.doOnSuccess(profiles -> {
					targetProfile = profiles.stream().findFirst().orElseThrow();
					var stage = (Stage) getWindow(send);
					Platform.runLater(() ->
					{
						stage.setTitle(targetProfile.getName()); // XXX: add the location name? yes but we need to retrieve the location then
						var chatMessage = (ChatMessage) send.getScene().getRoot().getUserData();
						if (chatMessage != null)
						{
							showMessage(chatMessage);
							send.getScene().getRoot().setUserData(null);
						}
					});
				})
				.doOnError(UiUtils::showAlertError)
				.subscribe();

		ownAvatar.setImage(new Image(JavaFxApplication.getControlUrl() + IDENTITIES_PATH + "/" + 1L + "/image", true));
		messageClient.requestAvatar(locationId);
	}

	public void showMessage(ChatMessage message)
	{
		if (message != null)
		{
			if (message.isEmpty())
			{
				lastTypingTimeline.playFromStart();
			}
			else
			{
				receive.addUserMessage(targetProfile.getName(), message.getContent());
				lastTypingTimeline.jumpTo(Duration.INDEFINITE);
			}
		}
	}

	public void showAvatar(ChatAvatar chatAvatar)
	{
		if (chatAvatar.getImage() != null)
		{
			targetAvatar.setImage(new Image(new ByteArrayInputStream(chatAvatar.getImage())));
		}
	}

	private void handleInputKeys(KeyEvent event)
	{
		if (PASTE_KEY.match(event))
		{
			var image = Clipboard.getSystemClipboard().getImage();
			if (image != null)
			{
				var imageView = new ImageView(image);
				ImageUtils.limitMaximumImageSize(imageView, IMAGE_WIDTH_MAX, IMAGE_HEIGHT_MAX);
				sendMessage("<img src=\"" + ImageUtils.writeImageAsJpegData(imageView.getImage(), MESSAGE_MAXIMUM_SIZE) + "\"/>");
				imageView.setImage(null);
				event.consume();
			}
		}
	}
}
