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

import io.xeres.common.rsid.Type;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.support.tray.TrayService;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;

@Component
@FxmlView(value = "/view/main.fxml")
public class MainWindowController implements WindowController
{
	public static final String XERES_DOCS_URL = "https://xeres.io/docs";
	@FXML
	private Label titleLabel;

	@FXML
	private MenuItem addPeer;

	@FXML
	private MenuItem copyOwnId;

	@FXML
	private MenuItem launchWebInterface;

	@FXML
	private MenuItem exitApplication;

	@FXML
	private MenuItem createChatRoom;

	@FXML
	private MenuItem showHelp;

	@FXML
	private MenuItem showAboutWindow;

	@FXML
	private MenuItem showProfilesWindow;

	@FXML
	private MenuItem showPeersWindow;

	@FXML
	private MenuItem showIdentitiesWindow;

	@FXML
	private MenuItem showBroadcastWindow;

	@FXML
	private Menu debug;

	@FXML
	private MenuItem runGc;

	@FXML
	public Button webHelpButton;

	private final LocationClient locationClient;
	private final TrayService trayService;
	private final WindowManager windowManager;
	private final Environment environment;

	public MainWindowController(LocationClient locationClient, TrayService trayService, WindowManager windowManager, Environment environment)
	{
		this.locationClient = locationClient;
		this.trayService = trayService;
		this.windowManager = windowManager;
		this.environment = environment;
	}

	public void initialize()
	{
		addPeer.setOnAction(event -> addPeer(null));
		copyOwnId.setOnAction(event -> copyOwnId());

		launchWebInterface.setOnAction(event -> openUrl(JavaFxApplication.getControlUrl()));

		createChatRoom.setOnAction(event -> windowManager.openChatRoomCreation(titleLabel.getScene().getWindow()));

		showHelp.setOnAction(event -> openUrl(XERES_DOCS_URL));
		webHelpButton.setOnAction(event -> openUrl(XERES_DOCS_URL));

		showAboutWindow.setOnAction(event -> windowManager.openAbout(titleLabel.getScene().getWindow()));

		showBroadcastWindow.setOnAction(event -> windowManager.openBroadcast(titleLabel.getScene().getWindow()));

		showProfilesWindow.setOnAction(event -> windowManager.openProfiles(titleLabel.getScene().getWindow()));

		showIdentitiesWindow.setOnAction(event -> windowManager.openIdentities(titleLabel.getScene().getWindow()));

		showPeersWindow.setOnAction(event -> windowManager.openPeers());

		if (environment.acceptsProfiles(Profiles.of("dev")))
		{
			debug.setVisible(true);
			runGc.setOnAction(event -> System.gc());
		}

		exitApplication.setOnAction(event ->
		{
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
		var rsIdResponse = locationClient.getRSId(OWN_LOCATION_ID, Type.ANY);
		rsIdResponse.subscribe(reply -> Platform.runLater(() -> {
			var clipboard = Clipboard.getSystemClipboard();
			var content = new ClipboardContent();
			content.putString(reply.rsId());
			clipboard.setContent(content);
		}));
	}

	public void addPeer(String rsId)
	{
		windowManager.openAddPeer(titleLabel.getScene().getWindow(), rsId);
	}

	private void openUrl(String url)
	{
		JavaFxApplication.openUrl(url);
	}
}
