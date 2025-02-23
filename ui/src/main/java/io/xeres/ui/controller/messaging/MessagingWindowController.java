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

package io.xeres.ui.controller.messaging;

import atlantafx.base.controls.Message;
import io.xeres.common.id.Identifier;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.location.Availability;
import io.xeres.common.message.chat.ChatAvatar;
import io.xeres.common.message.chat.ChatBacklog;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.common.rest.file.AddDownloadRequest;
import io.xeres.ui.client.ChatClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.client.ShareClient;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.controller.chat.ChatListView;
import io.xeres.ui.custom.TypingNotificationView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.model.profile.Profile;
import io.xeres.ui.support.chat.ChatCommand;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.uri.FileUri;
import io.xeres.ui.support.uri.FileUriFactory;
import io.xeres.ui.support.uri.Uri;
import io.xeres.ui.support.uri.UriService;
import io.xeres.ui.support.util.ImageUtils;
import io.xeres.ui.support.util.TextInputControlUtils;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import static io.xeres.common.message.chat.ChatConstants.TYPING_NOTIFICATION_DELAY;
import static io.xeres.ui.support.util.UiUtils.getWindow;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@FxmlView(value = "/view/messaging/messaging.fxml")
public class MessagingWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(MessagingWindowController.class);

	private static final int IMAGE_WIDTH_MAX = 800;
	private static final int IMAGE_HEIGHT_MAX = 600;
	private static final int MESSAGE_MAXIMUM_SIZE = 196_000; // XXX: maximum size for normal messages? check if correct

	private static final KeyCodeCombination PASTE_KEY = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
	private static final KeyCodeCombination COPY_KEY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
	private static final KeyCodeCombination CTRL_ENTER = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN);
	private static final KeyCodeCombination SHIFT_ENTER = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN);

	@FXML
	private TextArea send;

	@FXML
	private TypingNotificationView notification;

	@FXML
	private VBox content;

	@FXML
	private Message notice;

	@FXML
	private Button addImage;

	@FXML
	private Button addFile;

	private Availability availability = Availability.AVAILABLE;

	private ChatListView receive;

	private final ProfileClient profileClient;
	private final MarkdownService markdownService;
	private final WindowManager windowManager;
	private final UriService uriService;
	private final ResourceBundle bundle;
	private final Identifier destinationIdentifier;
	private Profile targetProfile;

	private final MessageClient messageClient;
	private final ShareClient shareClient;
	private final ChatClient chatClient;
	private final GeneralClient generalClient;
	private final ImageCache imageCache;

	private Instant lastTypingNotification = Instant.EPOCH;

	private Timeline lastTypingTimeline;

	private ParallelTransition sendAnimation;

	public MessagingWindowController(ProfileClient profileClient, WindowManager windowManager, UriService uriService, MessageClient messageClient, ShareClient shareClient, MarkdownService markdownService, Identifier destinationIdentifier, ResourceBundle bundle, ChatClient chatClient, GeneralClient generalClient, ImageCache imageCache)
	{
		this.profileClient = profileClient;
		this.windowManager = windowManager;
		this.uriService = uriService;
		this.messageClient = messageClient;
		this.shareClient = shareClient;
		this.markdownService = markdownService;
		this.chatClient = chatClient;
		this.bundle = bundle;
		this.destinationIdentifier = destinationIdentifier;
		this.generalClient = generalClient;
		this.imageCache = imageCache;
	}

	@Override
	public void initialize()
	{
		var ownProfileResult = profileClient.getOwn();
		ownProfileResult.doOnSuccess(profile -> Platform.runLater(() -> setupChatListView(profile.getName(), profile.getId())))
				.subscribe();

		send.addEventFilter(KeyEvent.KEY_PRESSED, this::handleInputKeys);
		TextInputControlUtils.addEnhancedInputContextMenu(send, null, this::handlePaste);

		addImage.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("messaging.file-requester.send-picture"));
			fileChooser.getExtensionFilters().addAll(new ExtensionFilter(bundle.getString("file-requester.images"), "*.png", "*.jpg", "*.jpeg", "*.jfif"));
			var selectedFile = fileChooser.showOpenDialog(getWindow(event));
			if (selectedFile != null && selectedFile.canRead())
			{
				CompletableFuture.runAsync(() -> {
					try (var inputStream = new FileInputStream(selectedFile))
					{
						var imageView = new ImageView(new Image(inputStream));
						Platform.runLater(() -> sendImageViewToMessage(imageView));
					}
					catch (IOException e)
					{
						UiUtils.alert(Alert.AlertType.ERROR, MessageFormat.format(bundle.getString("file-requester.error"), selectedFile, e.getMessage()));
					}
				});
			}
		});

		addFile.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("messaging.file-requester.send-file"));
			var selectedFile = fileChooser.showOpenDialog(getWindow(event));
			if (selectedFile != null && selectedFile.canRead())
			{
				sendFile(selectedFile);
			}
		});

		lastTypingTimeline = new Timeline(
				new KeyFrame(Duration.ZERO, event -> notification.setText(MessageFormat.format(bundle.getString("chat.notification.typing"), targetProfile.getName()))),
				new KeyFrame(Duration.seconds(TYPING_NOTIFICATION_DELAY.getSeconds())));
		lastTypingTimeline.setOnFinished(event -> notification.setText(""));

		setupAnimations();
	}

	private void sendMessage(String message)
	{
		if (isEmpty(message))
		{
			return;
		}
		var chatMessage = new ChatMessage(ChatCommand.parseCommands(message));
		messageClient.sendToDestination(destinationIdentifier, chatMessage);
		send.clear();
	}

	private void sendTypingNotificationIfNeeded()
	{
		var now = Instant.now();
		if (java.time.Duration.between(lastTypingNotification, now).compareTo(TYPING_NOTIFICATION_DELAY.minusSeconds(1)) > 0)
		{
			var message = new ChatMessage();
			messageClient.sendToDestination(destinationIdentifier, message);
			lastTypingNotification = now;
		}
	}

	private void setupChatListView(String nickname, long id)
	{
		receive = new ChatListView(nickname, id, markdownService, this::handleUriAction, generalClient, imageCache, send);
		content.getChildren().add(1, receive.getChatView());
		content.setOnDragOver(event -> {
			if (event.getDragboard().hasFiles())
			{
				event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			event.consume();
		});
		content.setOnDragDropped(event -> {
			var files = event.getDragboard().getFiles();
			CollectionUtils.emptyIfNull(files).forEach(this::sendFile);
			event.setDropCompleted(true);
			event.consume();
		});
	}

	private void sendFile(File file)
	{
		shareClient.createTemporaryShare(file.getAbsolutePath())
				.doOnSuccess(result -> sendMessage(FileUriFactory.generate(file.getName(), getFileSize(file.toPath()), Sha1Sum.fromString(result))))
				.subscribe();
	}

	private static long getFileSize(Path path)
	{
		try
		{
			return Files.size(path);
		}
		catch (IOException e)
		{
			log.error("Failed to get the file size of {}", path);
			return 0;
		}
	}

	private void handleUriAction(Uri uri)
	{
		if (uri instanceof FileUri(String name, long size, Sha1Sum hash))
		{
			windowManager.openAddDownload(
					new AddDownloadRequest(name, size, hash, destinationIdentifier instanceof LocationIdentifier ? (LocationIdentifier) destinationIdentifier : null));
		}
		else
		{
			uriService.openUri(uri);
		}
	}

	@Override
	public void onShown()
	{
		if (destinationIdentifier instanceof LocationIdentifier locationIdentifier)
		{
			profileClient.findByLocationIdentifier(locationIdentifier, true).collectList()
					.doOnSuccess(profiles -> {
						targetProfile = profiles.stream().findFirst().orElseThrow();
						Platform.runLater(() ->
						{
							var location = targetProfile.getLocations().getFirst();
							setAvailability(location.isConnected() ? location.getAvailability() : Availability.OFFLINE);
							updateTitle();
							chatClient.getChatBacklog(location.getId()).collectList()
									.doOnSuccess(backlogs -> Platform.runLater(() -> {
										fillBacklog(backlogs); // No need to use userData to pass the incoming message, it's already in the backlog
									}))
									.subscribe();
						});
					})
					.doOnError(UiUtils::showAlertError)
					.subscribe();
		}
		else
		{
			throw new UnsupportedOperationException("Implement distant chat support");
		}

		messageClient.requestAvatar(destinationIdentifier);
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
				if (message.isOwn())
				{
					receive.addOwnMessage(message);
				}
				else
				{
					receive.addUserMessage(targetProfile.getName(), message.getContent());
				}
				lastTypingTimeline.jumpTo(Duration.INDEFINITE);
			}
		}
	}

	private void fillBacklog(List<ChatBacklog> messages)
	{
		messages.forEach(message -> {
			if (message.own())
			{
				receive.addOwnMessage(message.created(), message.message());
			}
			else
			{
				receive.addUserMessage(message.created(), targetProfile.getName(), message.message());
			}
		});
		receive.jumpToBottom(true);
	}

	public void showAvatar(ChatAvatar chatAvatar)
	{
		if (chatAvatar.getImage() != null)
		{
			var avatarImage = new Image(new ByteArrayInputStream(chatAvatar.getImage()));
			var stage = (Stage) getWindow(send);
			stage.getIcons().add(avatarImage);
		}
	}

	public void setAvailability(Availability availability)
	{
		this.availability = availability;
		updateTitle();
	}

	private void updateTitle()
	{
		var stage = (Stage) getWindow(send);
		stage.setTitle(targetProfile.getName() + " @ " + targetProfile.getLocations().getFirst().getName() + getAvailability());
	}

	private String getAvailability()
	{
		return switch (availability)
		{
			case AVAILABLE ->
			{
				setUserOnline(true);
				yield "";
			}
			case AWAY -> " (" + Availability.AWAY + ")";
			case BUSY -> " (" + Availability.BUSY + ")";
			case OFFLINE ->
			{
				setUserOnline(false);
				yield " (" + Availability.OFFLINE + ")";
			}
		};
	}

	private void setUserOnline(boolean online)
	{
		UiUtils.setPresent(notice, !online);
		addImage.setDisable(!online);
		addFile.setDisable(!online);
	}

	private void handleInputKeys(KeyEvent event)
	{
		if (PASTE_KEY.match(event))
		{
			if (handlePaste(send))
			{
				event.consume();
			}
		}
		else if (COPY_KEY.match(event))
		{
			if (receive.copy())
			{
				event.consume();
			}
		}
		else if (CTRL_ENTER.match(event) || SHIFT_ENTER.match(event) && isNotBlank(send.getText()))
		{
			send.insertText(send.getCaretPosition(), "\n");
			sendTypingNotificationIfNeeded();
			event.consume();
		}
		else if (event.getCode() == KeyCode.ENTER)
		{
			if (isNotBlank(send.getText()))
			{
				if (notice.isVisible())
				{
					sendAnimation.play();
				}
				else
				{
					sendMessage(send.getText());
					lastTypingNotification = Instant.EPOCH;
				}
			}
			event.consume();
		}
		else
		{
			sendTypingNotificationIfNeeded();
		}
	}

	private boolean handlePaste(TextInputControl textInputControl)
	{
		var object = ClipboardUtils.getSupportedObjectFromClipboard();
		return switch (object)
		{
			case Image image ->
			{
				sendImageViewToMessage(new ImageView(image));
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

	private void sendImageViewToMessage(ImageView imageView)
	{
		ImageUtils.limitMaximumImageSize(imageView, IMAGE_WIDTH_MAX * IMAGE_HEIGHT_MAX);
		sendMessage("<img src=\"" + ImageUtils.writeImageAsJpegData(imageView.getImage(), MESSAGE_MAXIMUM_SIZE) + "\"/>");
		imageView.setImage(null);
	}

	private void setupAnimations()
	{
		var translateTransition = new TranslateTransition(javafx.util.Duration.millis(50));
		translateTransition.setFromX(-5.0);
		translateTransition.setToX(5.0);
		translateTransition.setCycleCount(6);
		translateTransition.setAutoReverse(true);
		translateTransition.setInterpolator(Interpolator.LINEAR);
		translateTransition.setNode(send);
		translateTransition.setOnFinished(actionEvent -> send.setTranslateX(0.0));

		var fadeTransition = new FadeTransition(javafx.util.Duration.millis(100));
		fadeTransition.setByValue(-1.0);
		fadeTransition.setAutoReverse(true);
		fadeTransition.setCycleCount(4);
		fadeTransition.setInterpolator(Interpolator.EASE_IN);
		fadeTransition.setNode(notice);

		sendAnimation = new ParallelTransition(translateTransition, fadeTransition);
	}
}
