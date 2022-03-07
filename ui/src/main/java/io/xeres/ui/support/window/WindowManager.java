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

package io.xeres.ui.support.window;

import io.xeres.common.AppName;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.MainWindowController;
import io.xeres.ui.controller.about.AboutWindowController;
import io.xeres.ui.controller.account.AccountCreationWindowController;
import io.xeres.ui.controller.chat.ChatRoomCreationWindowController;
import io.xeres.ui.controller.chat.ChatRoomInvitationWindowController;
import io.xeres.ui.controller.id.AddRsIdWindowController;
import io.xeres.ui.controller.identity.IdentitiesWindowController;
import io.xeres.ui.controller.messaging.BroadcastWindowController;
import io.xeres.ui.controller.messaging.MessagingWindowController;
import io.xeres.ui.controller.messaging.PeersWindowController;
import io.xeres.ui.controller.profile.ProfilesWindowController;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.rgielen.fxweaver.core.FxWeaver;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

/**
 * Class that tries to overcome the half-assed JavaFX window system.
 */
@Component
public class WindowManager
{
	private final FxWeaver fxWeaver;
	private final ProfileClient profileClient;
	private final MessageClient messageClient;

	public WindowManager(FxWeaver fxWeaver, ProfileClient profileClient, MessageClient messageClient)
	{
		this.fxWeaver = fxWeaver;
		this.profileClient = profileClient;
		this.messageClient = messageClient;
	}

	@PostConstruct
	private void initializeUiWindow()
	{
		UiWindow.setFxWeaver(fxWeaver);
	}

	public void closeAllWindows()
	{
		Platform.runLater(() ->
		{
			var windows = UiWindow.getOpenedWindows();

			// There's a strange side effect here when windows are hidden, apparently JavaFX changes the list, so
			// we make a copy.
			var copyOfWindows = new ArrayList<>(windows);
			copyOfWindows.forEach(Window::hide);
		});
	}

	public void openPeers()
	{
		Platform.runLater(() ->
		{
			var peers = UiWindow.getOpenedWindow(PeersWindowController.class).orElse(null);
			if (peers != null)
			{
				peers.requestFocus();
			}
			else
			{
				UiWindow.builder(PeersWindowController.class)
						.setRememberEnvironment(true)
						.build()
						.open();
			}
		});
	}

	public void openMessaging(String locationId, ChatMessage chatMessage)
	{
		Platform.runLater(() ->
				UiWindow.getOpenedWindow(MessagingWindowController.class, locationId).ifPresentOrElse(window ->
						{
							window.requestFocus();
							((MessagingWindowController) window.getUserData()).showMessage(chatMessage);
						},
						() ->
						{
							var messaging = new MessagingWindowController(profileClient, messageClient, locationId);

							UiWindow.builder("/view/messaging/messaging.fxml", messaging)
									.setLocalId(locationId)
									.setUserData(chatMessage)
									.build()
									.open();
						}));
	}

	public void openAbout(Window parent)
	{
		Platform.runLater(() ->
				UiWindow.builder(AboutWindowController.class)
						.setParent(parent)
						.setTitle("About " + AppName.NAME)
						.build()
						.open());
	}

	public void openChatRoomCreation(Window parent)
	{
		Platform.runLater(() ->
				UiWindow.builder(ChatRoomCreationWindowController.class)
						.setParent(parent)
						.setTitle("Create Chat Room")
						.build()
						.open());
	}

	public void openBroadcast(Window parent)
	{
		Platform.runLater(() ->
				UiWindow.builder(BroadcastWindowController.class)
						.setParent(parent)
						.setTitle("Broadcast")
						.setMinHeight(220)
						.build()
						.open());
	}

	public void openProfiles(Window parent)
	{
		Platform.runLater(() ->
				UiWindow.builder(ProfilesWindowController.class)
						.setParent(parent)
						.setTitle("Profiles")
						.build()
						.open());
	}

	public void openIdentities(Window parent)
	{
		Platform.runLater(() ->
				UiWindow.builder(IdentitiesWindowController.class)
						.setParent(parent)
						.setTitle("Identities")
						.build()
						.open());
	}

	public void openAddPeer(Window parent, String rsId)
	{
		Platform.runLater(() ->
				UiWindow.builder(AddRsIdWindowController.class)
						.setParent(parent)
						.setTitle("Add peer ID")
						.setMinHeight(380)
						.setUserData(rsId)
						.build()
						.open());
	}

	public void openInvite(Window parent, long chatRoom)
	{
		Platform.runLater(() ->
				UiWindow.builder(ChatRoomInvitationWindowController.class)
						.setParent(parent)
						.setTitle("Invite peer")
						.setUserData(chatRoom)
						.build()
						.open());
	}

	public void openMain(Stage stage)
	{
		Platform.runLater(() -> UiWindow.builder(MainWindowController.class)
				.setStage(stage)
				.setMinWidth(600)
				.setMinHeight(400)
				.setRememberEnvironment(true)
				.build()
				.open());
	}

	public void openAccountCreation(Stage stage)
	{
		Platform.runLater(() -> UiWindow.builder(AccountCreationWindowController.class)
				.setStage(stage)
				.setMinWidth(280)
				.setMinHeight(240)
				.build()
				.open());
	}
}
