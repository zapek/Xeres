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

package io.xeres.ui.controller;

import io.xeres.common.rest.location.RSIdResponse;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.support.tray.TrayService;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;

@Component
@FxmlView(value = "/view/main.fxml")
public class MainWindowController implements WindowController
{
	@FXML
	private Label titleLabel;

	@FXML
	private MenuItem addFriend;

	@FXML
	private MenuItem copyOwnId;

	@FXML
	private MenuItem launchWebInterface;

	@FXML
	private MenuItem exitApplication;

	@FXML
	private MenuItem createChatRoom;

	@FXML
	private MenuItem showAboutWindow;

	@FXML
	private MenuItem showProfilesWindow;

	@FXML
	private MenuItem showFriendsWindow;

	@FXML
	private MenuItem showBroadcastWindow;

	private final LocationClient locationClient;
	private final TrayService trayService;
	private final WindowManager windowManager;

	public MainWindowController(LocationClient locationClient, TrayService trayService, WindowManager windowManager)
	{
		this.locationClient = locationClient;
		this.trayService = trayService;
		this.windowManager = windowManager;
	}

	public void initialize()
	{
		// Friends
		addFriend.setOnAction(event -> addFriend());
		copyOwnId.setOnAction(event -> copyOwnId());

		launchWebInterface.setOnAction(event -> launchBrowser());

		createChatRoom.setOnAction(event -> windowManager.openChatRoomCreation(titleLabel.getScene().getWindow()));

		showAboutWindow.setOnAction(event -> windowManager.openAbout(titleLabel.getScene().getWindow()));

		showBroadcastWindow.setOnAction(event -> windowManager.openBroadcast(titleLabel.getScene().getWindow()));

		showProfilesWindow.setOnAction(event -> windowManager.openProfiles(titleLabel.getScene().getWindow()));

		showFriendsWindow.setOnAction(event -> windowManager.openFriends());

		exitApplication.setOnAction(event ->
		{
			UiUtils.closeWindow(titleLabel); // the event can contain a MenuItem which is not a Node

			windowManager.closeAllWindows();

			if (trayService.hasSystemTray())
			{
				Platform.exit();
			}
		});
	}

	@Override
	public void onShown()
	{
		trayService.addSystemTray((Stage) titleLabel.getScene().getWindow());
	}

	private void copyOwnId()
	{
		Mono<RSIdResponse> certificate = locationClient.getRSId(OWN_LOCATION_ID);
		certificate.subscribe(reply -> Platform.runLater(() -> {
			var clipboard = Clipboard.getSystemClipboard();
			var content = new ClipboardContent();
			content.putString(reply.RSId());
			clipboard.setContent(content);
		}));
	}

	private void addFriend()
	{
		windowManager.openAddFriend(titleLabel.getScene().getWindow());
	}

	private void launchBrowser()
	{
		JavaFxApplication.openUrl(JavaFxApplication.getControlUrl());
	}
}
