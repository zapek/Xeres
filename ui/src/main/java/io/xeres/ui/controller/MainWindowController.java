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

import io.xeres.common.dto.identity.IdentityConstants;
import io.xeres.common.rest.notification.NatStatus;
import io.xeres.common.rsid.Type;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.client.IdentityClient;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.chat.ChatViewController;
import io.xeres.ui.custom.led.LedControl;
import io.xeres.ui.support.tray.TrayService;
import io.xeres.ui.support.util.TooltipUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;

@Component
@FxmlView(value = "/view/main.fxml")
public class MainWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(MainWindowController.class);

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
	private MenuItem changeOwnIdentityPicture;

	@FXML
	private MenuItem showBroadcastWindow;

	@FXML
	private MenuItem showSettingsWindow;

	@FXML
	private Menu debug;

	@FXML
	private MenuItem runGc;

	@FXML
	public Button webHelpButton;

	@FXML
	public Label numberOfConnections;

	@FXML
	public LedControl natStatus;

	@FXML
	public LedControl dhtStatus;

	private final ChatViewController chatViewController;

	private final LocationClient locationClient;
	private final TrayService trayService;
	private final WindowManager windowManager;
	private final Environment environment;
	private final IdentityClient identityClient;
	private final NotificationClient notificationClient;

	private int currentUsers;
	private int totalUsers;

	public MainWindowController(ChatViewController chatViewController, LocationClient locationClient, TrayService trayService, WindowManager windowManager, Environment environment, IdentityClient identityClient, NotificationClient notificationClient)
	{
		this.chatViewController = chatViewController;
		this.locationClient = locationClient;
		this.trayService = trayService;
		this.windowManager = windowManager;
		this.environment = environment;
		this.identityClient = identityClient;
		this.notificationClient = notificationClient;
	}

	@Override
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

		showSettingsWindow.setOnAction(even -> windowManager.openSettings(titleLabel.getScene().getWindow()));

		changeOwnIdentityPicture.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle("Select Avatar Picture");
			fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.jfif"));
			var selectedFile = fileChooser.showOpenDialog(titleLabel.getScene().getWindow());
			if (selectedFile != null && selectedFile.canRead())
			{
				identityClient.uploadIdentityImage(IdentityConstants.OWN_IDENTITY_ID, selectedFile)
						.subscribe();
			}
		});

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

		setupStatusNotifications();
	}

	@Override
	public void onShown()
	{
		trayService.addSystemTray((Stage) titleLabel.getScene().getWindow());
		chatViewController.jumpToBottom();
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

	private void setupStatusNotifications()
	{
		// Apparently the LED is not happy if we don't turn it on first here.
		natStatus.setState(true);
		dhtStatus.setState(true);

		notificationClient.getNotifications()
				.doOnComplete(() -> log.debug("Notification connection closed"))
				.doOnError(throwable -> log.debug("Notification error: {}", throwable.getMessage()))
				.doOnNext(sse -> Platform.runLater(() -> {
					if (sse.data() != null)
					{
						var newCurrentUsers = sse.data().currentUsers();
						var newTotalUsers = sse.data().totalUsers();
						var newNatStatus = sse.data().natStatus();
						var newDhtInfo = sse.data().dhtInfo();

						if (newCurrentUsers != null)
						{
							currentUsers = newCurrentUsers;
						}
						if (newTotalUsers != null)
						{
							totalUsers = newTotalUsers;
						}

						numberOfConnections.setText(this.currentUsers + "/" + this.totalUsers);

						if (newNatStatus != null)
						{
							natStatus.setColor(newNatStatus == NatStatus.UPNP ? Color.GREENYELLOW : Color.ORANGE);
							switch (newNatStatus)
							{
								case UNKNOWN -> TooltipUtils.install(natStatus, "Status is still unknown.");
								case FIREWALLED -> TooltipUtils.install(natStatus, "The client is not reachable from connections initiated from the Internet.");
								case UPNP -> TooltipUtils.install(natStatus, "UPNP is active and the client is fully reachable from the Internet.");
							}
						}

						if (newDhtInfo != null)
						{
							switch (newDhtInfo.dhtStatus())
							{
								case OFF ->
								{
									dhtStatus.setState(false);
									TooltipUtils.install(dhtStatus, "DHT is disabled.");
								}
								case INITIALIZING ->
								{
									dhtStatus.setState(true);
									dhtStatus.setColor(Color.ORANGE);
									TooltipUtils.install(dhtStatus, "DHT is currently initializing. This can take a while.");
								}
								case RUNNING ->
								{
									dhtStatus.setState(true);
									dhtStatus.setColor(Color.GREENYELLOW);
									if (newDhtInfo.numPeers() == 0)
									{
										TooltipUtils.install(dhtStatus, "DHT is working properly, the client's IP is advertised to its peers.");
									}
									else
									{
										TooltipUtils.install(dhtStatus, "Number of peers: " + newDhtInfo.numPeers() + "\n" +
												"Received packets: " + newDhtInfo.receivedPackets() + " (" + newDhtInfo.receivedBytes() / 1024 + " KB)\n" +
												"Sent packets: " + newDhtInfo.sentPackets() + " (" + newDhtInfo.sentBytes() / 1024 + " KB)\n" +
												"Key count: " + newDhtInfo.keyCount() + "\n" +
												"Item count: " + newDhtInfo.itemCount());
									}
								}
							}
						}
					}
				}))
				.subscribe();
	}
}
