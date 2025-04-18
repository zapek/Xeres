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
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Identifier;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.location.Availability;
import io.xeres.common.message.chat.ChatAvatar;
import io.xeres.common.message.chat.ChatBacklog;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.common.rest.file.AddDownloadRequest;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.*;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.controller.chat.ChatListView;
import io.xeres.ui.custom.InputAreaGroup;
import io.xeres.ui.custom.TypingNotificationView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.custom.event.FileSelectedEvent;
import io.xeres.ui.custom.event.ImageSelectedEvent;
import io.xeres.ui.custom.event.StickerSelectedEvent;
import io.xeres.ui.support.chat.ChatCommand;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.uri.FileUri;
import io.xeres.ui.support.uri.FileUriFactory;
import io.xeres.ui.support.uri.Uri;
import io.xeres.ui.support.uri.UriService;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.util.image.ImageUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputControl;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;
import static io.xeres.ui.support.util.UiUtils.getWindow;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@FxmlView(value = "/view/messaging/messaging.fxml")
public class MessagingWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(MessagingWindowController.class);

	private static final int IMAGE_WIDTH_MAX = 800;
	private static final int IMAGE_HEIGHT_MAX = 600;
	private static final int STICKER_WIDTH_MAX = 256;
	private static final int STICKER_HEIGHT_MAX = 256;
	private static final int MESSAGE_MAXIMUM_SIZE = 196_000; // XXX: maximum size for normal messages? check if correct (I think it's more... like 300_000 but there's all kind of problems with that (websockets size, etc...) and RS closes the connection

	private static final KeyCodeCombination PASTE_KEY = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
	private static final KeyCodeCombination COPY_KEY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
	private static final KeyCodeCombination CTRL_ENTER = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN);
	private static final KeyCodeCombination SHIFT_ENTER = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN);

	@FXML
	private InputAreaGroup send;

	@FXML
	private TypingNotificationView notification;

	@FXML
	private VBox content;

	@FXML
	private Message notice;

	private Availability availability = Availability.AVAILABLE;

	private ChatListView receive;

	private final ProfileClient profileClient;
	private final IdentityClient identityClient;
	private final MarkdownService markdownService;
	private final WindowManager windowManager;
	private final UriService uriService;
	private final ResourceBundle bundle;
	private final Destination destination;

	private final MessageClient messageClient;
	private final ShareClient shareClient;
	private final ChatClient chatClient;
	private final GeneralClient generalClient;
	private final ImageCache imageCache;

	private Instant lastTypingNotification = Instant.EPOCH;

	private Timeline lastTypingTimeline;

	private ParallelTransition sendAnimation;

	private final boolean isIncoming;

	public MessagingWindowController(ProfileClient profileClient, IdentityClient identityClient, WindowManager windowManager, UriService uriService, MessageClient messageClient, ShareClient shareClient, MarkdownService markdownService, Identifier destinationIdentifier, ResourceBundle bundle, ChatClient chatClient, GeneralClient generalClient, ImageCache imageCache, boolean isIncoming)
	{
		this.profileClient = profileClient;
		this.identityClient = identityClient;
		this.windowManager = windowManager;
		this.uriService = uriService;
		this.messageClient = messageClient;
		this.shareClient = shareClient;
		this.markdownService = markdownService;
		this.chatClient = chatClient;
		this.bundle = bundle;
		this.generalClient = generalClient;
		this.imageCache = imageCache;
		destination = new Destination(destinationIdentifier);
		this.isIncoming = isIncoming;
	}

	@Override
	public void initialize()
	{
		var ownProfileResult = profileClient.getOwn();
		ownProfileResult.doOnSuccess(profile -> Platform.runLater(() -> setupChatListView(profile.getName(), profile.getId())))
				.subscribe();

		send.addKeyFilter(this::handleInputKeys);
		send.addEnhancedContextMenu(this::handlePaste);

		send.addEventHandler(StickerSelectedEvent.STICKER_SELECTED, event -> {
			event.consume();
			CompletableFuture.runAsync(() -> {
				try (var inputStream = new FileInputStream(event.getPath().toFile()))
				{
					var imageView = new ImageView(new Image(inputStream));
					Platform.runLater(() -> sendStickerToMessage(imageView));
				}
				catch (IOException e)
				{
					log.error("Couldn't send the sticker: {}", e.getMessage());
				}
			});
		});

		send.addEventHandler(ImageSelectedEvent.IMAGE_SELECTED, event -> {
			if (event.getFile().canRead())
			{
				CompletableFuture.runAsync(() -> {
					try (var inputStream = new FileInputStream(event.getFile()))
					{
						var imageView = new ImageView(new Image(inputStream));
						Platform.runLater(() -> sendImageViewToMessage(imageView));
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

		lastTypingTimeline = new Timeline(
				new KeyFrame(Duration.ZERO, event -> notification.setText(MessageFormat.format(bundle.getString("chat.notification.typing"), destination.getName()))),
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
		messageClient.sendToDestination(destination.getIdentifier(), chatMessage);
		send.clear();
	}

	private void sendTypingNotificationIfNeeded()
	{
		var now = Instant.now();
		if (java.time.Duration.between(lastTypingNotification, now).compareTo(TYPING_NOTIFICATION_DELAY.minusSeconds(1)) > 0)
		{
			var message = new ChatMessage();
			messageClient.sendToDestination(destination.getIdentifier(), message);
			lastTypingNotification = now;
		}
	}

	private void setupChatListView(String nickname, long id)
	{
		receive = new ChatListView(nickname, id, markdownService, this::handleUriAction, generalClient, imageCache, windowManager, send);
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
					new AddDownloadRequest(name, size, hash, destination.getIdentifier() instanceof LocationIdentifier locationIdentifier ? locationIdentifier : null));
		}
		else
		{
			uriService.openUri(uri);
		}
	}

	@Override
	public void onShown()
	{
		if (destination.getIdentifier() instanceof LocationIdentifier locationIdentifier)
		{
			profileClient.findByLocationIdentifier(locationIdentifier, true).collectList()
					.doOnSuccess(profiles -> {
						var profile = profiles.stream().findFirst().orElseThrow();
						Platform.runLater(() ->
						{
							var location = profile.getLocations().getFirst();
							setAvailability(location.isConnected() ? location.getAvailability() : Availability.OFFLINE);
							destination.setName(profile.getName());
							destination.setPlace(location.getName());
							updateTitle();
							receive.setOnClearHistory(() -> chatClient.deleteChatBacklog(location.getId()).subscribe());
							chatClient.getChatBacklog(location.getId()).collectList()
									.doOnSuccess(backlogs -> Platform.runLater(() -> fillBacklog(backlogs))) // No need to use userData to pass the incoming message, it's already in the backlog
									.subscribe();
						});
					})
					.doOnError(UiUtils::showAlertError)
					.subscribe();
			messageClient.requestAvatar(destination.getIdentifier());
		}
		else if (destination.getIdentifier() instanceof GxsId gxsId)
		{
			identityClient.findByGxsId(gxsId).collectList()
					.doOnSuccess(gxsIds -> {
						var identity = gxsIds.stream().findFirst().orElseThrow();
						Platform.runLater(() -> {
							destination.setName(identity.getName());
							updateTitle();
							fetchIdentityImage(identity.hasImage() ? identity.getId() : 0L, identity.getGxsId());
							receive.setOnClearHistory(() -> chatClient.deleteDistantChatBacklog(identity.getId()).subscribe());
							chatClient.getDistantChatBacklog(identity.getId()).collectList()
									.doOnSuccess(backlogs -> Platform.runLater(() -> fillBacklog(backlogs))) // Incoming message already in the backlog
									.subscribe();
							if (isIncoming)
							{
								setAvailability(Availability.AVAILABLE);
							}
							else
							{
								chatClient.createDistantChat(identity.getId())
										.doOnSuccess(chat -> Platform.runLater(() -> {
											setAvailability(Availability.OFFLINE);
											notification.setProgress(bundle.getString("messaging.tunneling"));
										}))
										.doOnError(WebClientResponseException.class, e -> {
											if (e.getStatusCode() == HttpStatus.CONFLICT)
											{
												Platform.runLater(() -> setAvailability(Availability.OFFLINE));
											}
										})
										.subscribe();
							}
						});
					})
					.doOnError(UiUtils::showAlertError)
					.subscribe();

			UiUtils.getWindow(send).setOnCloseRequest(event -> {
				if (availability != Availability.OFFLINE)
				{
					UiUtils.alertConfirm(bundle.getString("messaging.closing-tunnel.confirm"), () -> UiUtils.getWindow(send).hide());
					event.consume();
				}
			});
		}
	}

	private void fetchIdentityImage(long identityId, GxsId gxsId)
	{
		String url;

		if (identityId != 0L)
		{
			url = RemoteUtils.getControlUrl() + IDENTITIES_PATH + "/" + identityId + "/image";
		}
		else
		{
			url = RemoteUtils.getControlUrl() + IDENTITIES_PATH + "/image?gxsId=" + gxsId;
		}
		generalClient.getImage(url)
				.doOnSuccess(imageData -> Platform.runLater(() -> setWindowIcon(imageData)))
				.subscribe();
	}

	private void setWindowIcon(byte[] imageData)
	{
		var icon = new Image(new ByteArrayInputStream(imageData));
		var stage = (Stage) getWindow(send);
		stage.getIcons().add(icon);
	}

	@Override
	public void onHidden()
	{
		if (destination.getIdentifier() instanceof GxsId gxsId)
		{
			identityClient.findByGxsId(gxsId).collectList()
					.doOnSuccess(gxsIds -> {
						var identity = gxsIds.stream().findFirst().orElseThrow();
						Platform.runLater(() -> chatClient.closeDistantChat(identity.getId())
								.subscribe());
					})
					.subscribe();
		}
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
					receive.addUserMessage(destination.getName(), message.getContent());
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
				receive.addUserMessage(message.created(), destination.getName(), message.message());
			}
		});
		receive.jumpToBottom(true);
	}

	public void showAvatar(ChatAvatar chatAvatar)
	{
		if (chatAvatar.getImage() != null)
		{
			setWindowIcon(chatAvatar.getImage());
		}
	}

	public void setAvailability(Availability availability)
	{
		this.availability = availability;
		updateTitle();
		notification.stopProgress();
	}

	private void updateTitle()
	{
		var stage = (Stage) getWindow(send);
		stage.setTitle(destination.getName() + (destination.hasPlace() ? (" @ " + destination.getPlace()) : "") + " " + getAvailability());
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
			case AWAY -> "(" + Availability.AWAY + ")";
			case BUSY -> "(" + Availability.BUSY + ")";
			case OFFLINE ->
			{
				setUserOnline(false);
				yield "(" + Availability.OFFLINE + ")";
			}
		};
	}

	private void setUserOnline(boolean online)
	{
		UiUtils.setPresent(notice, !online);
		send.setDisable(!online);
	}

	private void handleInputKeys(KeyEvent event)
	{
		if (PASTE_KEY.match(event))
		{
			if (handlePaste(send.getTextInputControl()))
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
		else if (CTRL_ENTER.match(event) || SHIFT_ENTER.match(event) && isNotBlank(send.getTextInputControl().getText()))
		{
			send.getTextInputControl().insertText(send.getTextInputControl().getCaretPosition(), "\n");
			sendTypingNotificationIfNeeded();
			event.consume();
		}
		else if (event.getCode() == KeyCode.ENTER)
		{
			if (isNotBlank(send.getTextInputControl().getText()))
			{
				if (notice.isVisible())
				{
					sendAnimation.play();
				}
				else
				{
					sendMessage(send.getTextInputControl().getText());
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

	private void sendStickerToMessage(ImageView imageView)
	{
		ImageUtils.limitMaximumImageSize(imageView, STICKER_WIDTH_MAX * STICKER_HEIGHT_MAX);
		sendMessage("<img src=\"" + ImageUtils.writeImageAsPngData(imageView.getImage(), MESSAGE_MAXIMUM_SIZE) + "\"/>");
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
