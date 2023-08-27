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

package io.xeres.ui.support.window;

import io.xeres.common.AppName;
import io.xeres.common.message.chat.ChatAvatar;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.common.rest.forum.PostRequest;
import io.xeres.common.rest.location.RSIdResponse;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.MainWindowController;
import io.xeres.ui.controller.about.AboutWindowController;
import io.xeres.ui.controller.account.AccountCreationWindowController;
import io.xeres.ui.controller.chat.ChatRoomCreationWindowController;
import io.xeres.ui.controller.chat.ChatRoomInvitationWindowController;
import io.xeres.ui.controller.forum.ForumCreationWindowController;
import io.xeres.ui.controller.forum.ForumEditorViewController;
import io.xeres.ui.controller.id.AddRsIdWindowController;
import io.xeres.ui.controller.identity.IdentitiesWindowController;
import io.xeres.ui.controller.messaging.BroadcastWindowController;
import io.xeres.ui.controller.messaging.MessagingWindowController;
import io.xeres.ui.controller.messaging.PeersWindowController;
import io.xeres.ui.controller.profile.ProfilesWindowController;
import io.xeres.ui.controller.qrcode.CameraWindowController;
import io.xeres.ui.controller.qrcode.QrCodeWindowController;
import io.xeres.ui.controller.settings.SettingsWindowController;
import io.xeres.ui.model.profile.Profile;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.rgielen.fxweaver.core.FxWeaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Class that tries to overcome the half-assed JavaFX window system.
 */
@Component
public class WindowManager
{
	private static final Logger log = LoggerFactory.getLogger(WindowManager.class);

	private final FxWeaver fxWeaver;
	private final ProfileClient profileClient;
	private final MessageClient messageClient;
	private final ResourceBundle bundle;

	private UiWindow mainWindow;

	public WindowManager(FxWeaver fxWeaver, ProfileClient profileClient, MessageClient messageClient, ResourceBundle bundle)
	{
		this.fxWeaver = fxWeaver;
		this.profileClient = profileClient;
		this.messageClient = messageClient;
		this.bundle = bundle;
	}

	@PostConstruct
	private void initializeUiWindow()
	{
		UiWindow.setFxWeaver(fxWeaver, bundle);
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
							if (chatMessage == null || !chatMessage.isEmpty()) // Don't open a window for a typing notification, we're not psychic (but do open when we double click)
							{
								var messaging = new MessagingWindowController(profileClient, messageClient, locationId, bundle);

								UiWindow.builder("/view/messaging/messaging.fxml", messaging)
										.setLocalId(locationId)
										.setUserData(chatMessage)
										.build()
										.open();
							}
						}));
	}

	public void sendMessaging(String locationId, ChatAvatar chatAvatar)
	{
		Platform.runLater(() ->
				UiWindow.getOpenedWindow(MessagingWindowController.class, locationId).ifPresent(window ->
						((MessagingWindowController) window.getUserData()).showAvatar(chatAvatar)
				)
		);
	}

	public void openAbout(Window parent)
	{
		Platform.runLater(() ->
				UiWindow.builder(AboutWindowController.class)
						.setParent(parent)
						.setTitle(MessageFormat.format(bundle.getString("about.window-title"), AppName.NAME))
						.build()
						.open());
	}

	public void openQrCode(Window parent, RSIdResponse rsIdResponse)
	{
		Platform.runLater(() ->
				UiWindow.builder(QrCodeWindowController.class)
						.setParent(parent)
						.setTitle(bundle.getString("qrcode.window-title"))
						.setUserData(rsIdResponse)
						.build()
						.open());
	}

	public void openCamera(Window parent, AddRsIdWindowController parentController)
	{
		Platform.runLater(() ->
				UiWindow.builder(CameraWindowController.class)
						.setParent(parent)
						.setTitle(bundle.getString("camera.window-title"))
						.setResizeable(false)
						.setUserData(parentController)
						.build()
						.open());
	}

	public void openChatRoomCreation(Window parent)
	{
		Platform.runLater(() ->
				UiWindow.builder(ChatRoomCreationWindowController.class)
						.setParent(parent)
						.setTitle(bundle.getString("chat.room.create.window-title"))
						.build()
						.open());
	}

	public void openBroadcast(Window parent)
	{
		Platform.runLater(() ->
				UiWindow.builder(BroadcastWindowController.class)
						.setParent(parent)
						.setTitle("Broadcast")
						.build()
						.open());
	}

	public void openProfiles(Window parent)
	{
		Platform.runLater(() ->
				UiWindow.builder(ProfilesWindowController.class)
						.setParent(parent)
						.setTitle(bundle.getString("profiles.window-title"))
						.build()
						.open());
	}

	public void openIdentities(Window parent)
	{
		Platform.runLater(() ->
				UiWindow.builder(IdentitiesWindowController.class)
						.setParent(parent)
						.setTitle(bundle.getString("identities.window-title"))
						.build()
						.open());
	}

	public void openSettings(Window parent)
	{
		Platform.runLater(() ->
				UiWindow.builder(SettingsWindowController.class)
						.setParent(parent)
						.setTitle(bundle.getString("settings.window-title"))
						.build()
						.open());
	}

	public void openAddPeer(Window parent, String rsId)
	{
		Platform.runLater(() ->
				UiWindow.builder(AddRsIdWindowController.class)
						.setParent(parent)
						.setTitle(bundle.getString("rsid.add.window-title"))
						.setUserData(rsId)
						.build()
						.open());
	}

	public void openInvite(Window parent, long chatRoom)
	{
		Platform.runLater(() ->
				UiWindow.builder(ChatRoomInvitationWindowController.class)
						.setParent(parent)
						.setTitle(bundle.getString("chat.room.invite.window-title"))
						.setUserData(chatRoom)
						.build()
						.open());
	}

	public void openForumEditor(Window parent, PostRequest postRequest)
	{
		Platform.runLater(() ->
				UiWindow.builder(ForumEditorViewController.class)
						.setParent(parent) // XXX: needs to become multi modal to avoid blocking (useful to browse other posts while we write)
						.setTitle("New message")
						.setUserData(postRequest)
						.build()
						.open());
	}

	public void openForumCreation(Window parent)
	{
		Platform.runLater(() ->
				UiWindow.builder(ForumCreationWindowController.class)
						.setParent(parent)
						.setTitle("Create forum")
						.build()
						.open());
	}

	public void openMain(Stage stage, Profile profile, boolean iconified)
	{
		Platform.runLater(() -> {

			if (mainWindow != null && !iconified)
			{
				mainWindow.open();
			}
			else
			{
				mainWindow = UiWindow.builder(MainWindowController.class)
						.setStage(stage)
						.setRememberEnvironment(true)
						.setTitle(profile != null ? (AppName.NAME + " - " + profile.getName() + " @ " + profile.getLocations().stream().findFirst().orElseThrow().getName()) : null)
						.build();

				if (!iconified)
				{
					mainWindow.open();
				}
			}
		});
	}

	public Stage getMainStage()
	{
		return mainWindow.stage;
	}

	public void openAccountCreation(Stage stage)
	{
		Platform.runLater(() -> UiWindow.builder(AccountCreationWindowController.class)
				.setStage(stage)
				.build()
				.open());
	}

	/**
	 * Calculates the window's decoration sizes (aka the windows borders). To do that, a dummy scene is created and put on an invisible
	 * window, which is opened, the insets are calculated then the window is closed.<br>
	 * This only works if Platform.setExplicitExit() is false.
	 *
	 * @param stage the primary stage
	 */
	public void calculateWindowDecorationSizes(Stage stage)
	{
		if (Platform.isImplicitExit())
		{
			throw new IllegalStateException("implicit exit must not be set for window decoration calculation to work");
		}

		var root = new Region();
		stage.setScene(new Scene(root));
		stage.setOpacity(0.0);
		stage.show();

		var insets = getInsets(stage);

		stage.hide();
		stage.setOpacity(1.0);

		UiWindow.setWindowDecorationSizes(insets.get().getTop(), insets.get().getBottom(), insets.get().getLeft(), insets.get().getRight());
	}

	private static ObjectBinding<Insets> getInsets(Stage stage)
	{
		var scene = stage.getScene();

		return Bindings.createObjectBinding(() -> new Insets(scene.getY(),
						stage.getWidth() - scene.getWidth() - scene.getX(),
						stage.getHeight() - scene.getHeight() - scene.getY(),
						scene.getX()),
				scene.xProperty(),
				scene.yProperty(),
				scene.widthProperty(),
				scene.heightProperty(),
				stage.widthProperty(),
				stage.heightProperty());
	}
}
