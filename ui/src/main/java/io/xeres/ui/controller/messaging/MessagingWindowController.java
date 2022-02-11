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

package io.xeres.ui.controller.messaging;

import io.xeres.common.id.LocationId;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.controller.chat.ChatListView;
import io.xeres.ui.model.profile.Profile;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static io.xeres.common.message.chat.ChatConstants.TYPING_NOTIFICATION_DELAY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@FxmlView(value = "/view/messaging/messaging.fxml")
public class MessagingWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(MessagingWindowController.class);

	@FXML
	private TextField send;

	@FXML
	private Label notification;

	@FXML
	private VBox content;

	private ChatListView receive;

	private final ProfileClient profileClient;
	private final LocationId locationId;
	private Profile targetProfile;

	private final MessageClient messageClient;

	private Instant lastTypingNotification = Instant.EPOCH;

	private Timeline lastTypingTimeline;

	public MessagingWindowController(ProfileClient profileClient, MessageClient messageClient, String locationId)
	{
		this.profileClient = profileClient;
		this.messageClient = messageClient;
		this.locationId = new LocationId(locationId);
	}

	public void initialize()
	{
		Mono<Profile> ownProfileResult = profileClient.getOwn();
		ownProfileResult.doOnSuccess(profile -> setupChatListView(profile.getName(), profile.getId()))
				.subscribe();

		send.setOnKeyPressed(event ->
		{
			if (event.getCode().equals(KeyCode.ENTER) && isNotBlank(send.getText()))
			{
				var message = new ChatMessage(send.getText());
				messageClient.sendToLocation(locationId, message);
				receive.addOwnMessage(send.getText());
				send.clear();
			}
			else
			{
				var now = Instant.now();
				if (java.time.Duration.between(lastTypingNotification, now).compareTo(TYPING_NOTIFICATION_DELAY) > 0)
				{
					var message = new ChatMessage();
					messageClient.sendToLocation(locationId, message);
					lastTypingNotification = now;
				}
			}
		});

		lastTypingTimeline = new Timeline(new KeyFrame(Duration.seconds(5),
				ae -> notification.setText("")));
	}

	private void setupChatListView(String nickname, long id)
	{
		Platform.runLater(() -> {
			receive = new ChatListView(nickname, id);
			content.getChildren().add(0, receive.getChatView());
		});
	}

	@Override
	public void onShown()
	{
		profileClient.findByLocationId(locationId).collectList()
				.doOnSuccess(profiles -> {
					targetProfile = profiles.stream().findFirst().orElseThrow();
					var stage = (Stage) send.getScene().getWindow();
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
				.doOnError(throwable -> log.error("Error while getting the profiles: {}", throwable.getMessage(), throwable))
				.subscribe();
	}

	public void showMessage(ChatMessage message)
	{
		if (message != null)
		{
			if (message.isEmpty())
			{
				notification.setText(targetProfile.getName() + " is typing...");
				lastTypingTimeline.playFromStart();
			}
			else
			{
				receive.addUserMessage(targetProfile.getName(), message.getContent());
				notification.setText("");
			}
		}
	}
}
