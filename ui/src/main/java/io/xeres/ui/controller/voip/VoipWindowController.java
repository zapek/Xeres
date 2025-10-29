/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.voip;

import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.message.voip.VoipAction;
import io.xeres.common.message.voip.VoipMessage;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.support.contact.ContactUtils;
import io.xeres.ui.support.sound.SoundPlayerService;
import io.xeres.ui.support.util.DateUtils;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.media.AudioClip;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.time.LocalTime;
import java.util.Objects;
import java.util.ResourceBundle;

import static io.xeres.ui.support.sound.SoundPlayerService.SoundType;

@Component
@FxmlView(value = "/view/voip/voip.fxml")
public class VoipWindowController implements WindowController
{
	public record Parameters(String destination, VoipMessage message)
	{
	}

	private enum Status
	{
		INCOMING_CALL,
		OUTGOING_CALL,
		IN_CALL,
		ENDED
	}

	@FXML
	private AsyncImageView imageView;

	@FXML
	private Label nameLabel;

	@FXML
	private Label statusLabel;

	@FXML
	private Label timerLabel;

	@FXML
	private Button messageButton;

	@FXML
	private Button recallButton;

	@FXML
	private Button closeButton;

	@FXML
	private Button answerButton;

	@FXML
	private Button rejectButton;

	private final MessageClient messageClient;
	private final GeneralClient generalClient;
	private final ProfileClient profileClient;
	private final WindowManager windowManager;
	private final ResourceBundle bundle;
	private final SoundPlayerService soundPlayerService;

	private LocationIdentifier destinationIdentifier;
	private Status status;
	private final TimeCounter timeCounter;
	private AudioClip audioClip;

	public VoipWindowController(MessageClient messageClient, GeneralClient generalClient, ProfileClient profileClient, WindowManager windowManager, ResourceBundle bundle, SoundPlayerService soundPlayerService)
	{
		this.messageClient = messageClient;
		this.generalClient = generalClient;
		this.profileClient = profileClient;
		this.windowManager = windowManager;
		this.bundle = bundle;
		this.soundPlayerService = soundPlayerService;

		timeCounter = new TimeCounter(duration -> timerLabel.setText(DateUtils.TIME_DISPLAY_WITH_SECONDS.format(LocalTime.ofSecondOfDay(duration.getSeconds() % (24 * 3600)))));
	}

	@Override
	public void initialize()
	{
		imageView.setLoader(url -> generalClient.getImage(url).block());

		answerButton.setOnAction(_ -> {
			messageClient.sendToDestination(destinationIdentifier, new VoipMessage(VoipAction.ACKNOWLEDGE));
			status = Status.IN_CALL;
			updateState();
		});
		rejectButton.setOnAction(_ -> {
			messageClient.sendToDestination(destinationIdentifier, new VoipMessage(VoipAction.CLOSE));
			status = Status.ENDED;
			updateState();
		});
		messageButton.setOnAction(_ -> windowManager.openMessaging(destinationIdentifier));
		recallButton.setOnAction(_ -> {
			messageClient.sendToDestination(destinationIdentifier, new VoipMessage(VoipAction.RING));
			status = Status.OUTGOING_CALL;
			updateState();
		});
		closeButton.setOnAction(_ -> UiUtils.getWindow(nameLabel).hide());
	}

	@Override
	public void onShown()
	{
		var userData = (Parameters) Objects.requireNonNull(UiUtils.getUserData(answerButton), "missing Parameters userdata");
		destinationIdentifier = LocationIdentifier.fromString(userData.destination);
		profileClient.findByLocationIdentifier(destinationIdentifier, false).collectList()
				.publishOn(Schedulers.boundedElastic())
				.doOnSuccess(profiles -> profileClient.findContactsForProfile(profiles.getFirst().getId()).collectList()
						.doOnSuccess(contacts -> Platform.runLater(() -> {
							var contact = contacts.getFirst();
							nameLabel.setText(contact.name());
							imageView.setUrl(ContactUtils.getIdentityImageUrl(contact));
						}))
						.subscribe())
				.subscribe();

		if (userData.message == null)
		{
			messageClient.sendToDestination(destinationIdentifier, new VoipMessage(VoipAction.RING));
			status = Status.OUTGOING_CALL;
		}
		else
		{
			status = Status.INCOMING_CALL;
		}
		updateState();
		setupImagePresence();

		UiUtils.getWindow(nameLabel).setOnCloseRequest(event -> {
			if (status != Status.ENDED)
			{
				UiUtils.alertConfirm(bundle.getString("voip.action.window-quit"), () -> {
					stopRingingSound();
					messageClient.sendToDestination(destinationIdentifier, new VoipMessage(VoipAction.CLOSE));
					UiUtils.getWindow(nameLabel).hide();
				});
				event.consume();
			}
		});
	}

	public void doAction(String identifier, VoipMessage voipMessage)
	{
		destinationIdentifier = LocationIdentifier.fromString(identifier);
		switch (voipMessage.getAction())
		{
			case RING -> status = Status.INCOMING_CALL;
			case ACKNOWLEDGE -> status = Status.IN_CALL;
			case CLOSE -> status = Status.ENDED;
		}
		updateState();
	}

	private void updateState()
	{
		switch (status)
		{
			case INCOMING_CALL ->
			{
				statusLabel.setText(bundle.getString("voip.status.incoming"));
				rejectButton.setText(bundle.getString("voip.action.reject"));
				timerLabel.setVisible(false);
				UiUtils.setAbsent(messageButton);
				UiUtils.setAbsent(recallButton);
				UiUtils.setAbsent(closeButton);
				UiUtils.setPresent(answerButton);
				UiUtils.setPresent(rejectButton);
				playRingingSound();
			}
			case OUTGOING_CALL ->
			{
				statusLabel.setText(bundle.getString("voip.status.calling"));
				timerLabel.setVisible(false);
				UiUtils.setAbsent(messageButton);
				UiUtils.setAbsent(recallButton);
				UiUtils.setAbsent(closeButton);
				UiUtils.setAbsent(answerButton);
				UiUtils.setPresent(rejectButton);
				playRingingSound();
			}
			case IN_CALL ->
			{
				statusLabel.setText(bundle.getString("voip.status.ongoing"));
				timeCounter.start();
				rejectButton.setText(bundle.getString("voip.action.hangup"));
				timerLabel.setVisible(true);
				UiUtils.setAbsent(answerButton);
				UiUtils.setPresent(rejectButton);
				stopRingingSound();
			}
			case ENDED ->
			{
				statusLabel.setText(bundle.getString("voip.status.ended"));
				timeCounter.stop();
				UiUtils.setPresent(messageButton);
				UiUtils.setPresent(recallButton);
				UiUtils.setPresent(closeButton);
				UiUtils.setAbsent(answerButton);
				UiUtils.setAbsent(rejectButton);
				stopRingingSound();
			}
		}
	}

	// Remove the contact image when the window is resized small
	// so that we can save up space.
	private void setupImagePresence()
	{
		var scene = imageView.getScene();

		BooleanBinding showImage = scene.widthProperty().greaterThan(300)
				.and(scene.heightProperty().greaterThan(280));

		imageView.managedProperty().bind(showImage);
		imageView.visibleProperty().bind(showImage);
	}

	private void playRingingSound()
	{
		stopRingingSound();
		audioClip = soundPlayerService.playRepeated(SoundType.RINGING);
	}

	private void stopRingingSound()
	{
		if (audioClip != null)
		{
			audioClip.stop();
			audioClip = null;
		}
	}
}
