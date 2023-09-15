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

package io.xeres.ui.controller;

import io.xeres.common.dto.identity.IdentityConstants;
import io.xeres.common.rest.notification.status.DhtInfo;
import io.xeres.common.rest.notification.status.NatStatus;
import io.xeres.common.rsid.Type;
import io.xeres.common.util.ByteUnitUtils;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.client.ConfigClient;
import io.xeres.ui.client.IdentityClient;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.chat.ChatViewController;
import io.xeres.ui.custom.ReadOnlyTextField;
import io.xeres.ui.custom.led.LedControl;
import io.xeres.ui.custom.led.LedStatus;
import io.xeres.ui.support.tray.TrayService;
import io.xeres.ui.support.util.TooltipUtils;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import net.harawata.appdirs.AppDirsFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.io.File;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;
import static io.xeres.ui.support.util.UiUtils.getWindow;

@Component
@FxmlView(value = "/view/main.fxml")
public class MainWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(MainWindowController.class);

	private static final String XERES_DOCS_URL = "https://xeres.io/docs";
	private static final String XERES_BUGS_URL = "https://github.com/zapek/Xeres/issues/new/choose";
	private static final String XERES_FORUMS_URL = "https://github.com/zapek/Xeres/discussions";
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
	private MenuItem showHelp;

	@FXML
	private MenuItem reportBug;

	@FXML
	private MenuItem forums;

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
	private MenuItem exportBackup;

	@FXML
	private MenuItem showSettingsWindow;

	@FXML
	private Menu debug;

	@FXML
	private MenuItem runGc;

	@FXML
	private MenuItem h2Console;

	@FXML
	private ReadOnlyTextField shortId;

	@FXML
	private Button copyShortIdButton;

	@FXML
	private Button showQrCodeButton;

	@FXML
	public Button addFriendButton;

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
	private final ConfigClient configClient;
	private final NotificationClient notificationClient;
	private final ResourceBundle bundle;

	private int currentUsers;
	private int totalUsers;
	private Disposable notificationDisposable;

	public MainWindowController(ChatViewController chatViewController, LocationClient locationClient, TrayService trayService, WindowManager windowManager, Environment environment, IdentityClient identityClient, ConfigClient configClient, NotificationClient notificationClient, ResourceBundle bundle)
	{
		this.chatViewController = chatViewController;
		this.locationClient = locationClient;
		this.trayService = trayService;
		this.windowManager = windowManager;
		this.environment = environment;
		this.identityClient = identityClient;
		this.configClient = configClient;
		this.notificationClient = notificationClient;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		addPeer.setOnAction(event -> addPeer(null));
		addFriendButton.setOnAction(event -> addPeer(null));

		copyOwnId.setOnAction(event -> copyOwnId());
		copyShortIdButton.setOnAction(event -> copyOwnId());

		showQrCodeButton.setOnAction(event -> showQrCode(getWindow(event)));

		launchWebInterface.setOnAction(event -> openUrl(JavaFxApplication.getControlUrl()));

		showHelp.setOnAction(event -> openUrl(XERES_DOCS_URL));
		webHelpButton.setOnAction(event -> openUrl(XERES_DOCS_URL));

		reportBug.setOnAction(event -> openUrl(XERES_BUGS_URL));

		forums.setOnAction(event -> openUrl(XERES_FORUMS_URL));

		showAboutWindow.setOnAction(event -> windowManager.openAbout(getWindow(event)));

		showBroadcastWindow.setOnAction(event -> windowManager.openBroadcast(getWindow(event)));

		showProfilesWindow.setOnAction(event -> windowManager.openProfiles(getWindow(event)));

		showIdentitiesWindow.setOnAction(event -> windowManager.openIdentities(getWindow(event)));

		showSettingsWindow.setOnAction(event -> windowManager.openSettings(getWindow(event)));

		changeOwnIdentityPicture.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("main.select-avatar"));
			fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.jfif"));
			var selectedFile = fileChooser.showOpenDialog(getWindow(event));
			if (selectedFile != null && selectedFile.canRead())
			{
				identityClient.uploadIdentityImage(IdentityConstants.OWN_IDENTITY_ID, selectedFile)
						.subscribe();
			}
		});

		exportBackup.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle("Select the output profile");
			fileChooser.setInitialDirectory(new File(AppDirsFactory.getInstance().getUserDownloadsDir(null, null, null)));
			fileChooser.getExtensionFilters().add(new ExtensionFilter("XML files", "*.xml"));
			fileChooser.setInitialFileName("xeres_backup.xml");
			var selectedFile = fileChooser.showSaveDialog(UiUtils.getWindow(event));
			if (selectedFile != null)
			{
				DataBufferUtils.write(configClient.getBackup(), selectedFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).subscribe();
			}
		});

		showPeersWindow.setOnAction(event -> windowManager.openPeers());

		if (environment.acceptsProfiles(Profiles.of("dev")))
		{
			debug.setVisible(true);
			runGc.setOnAction(event -> System.gc());
			h2Console.setOnAction(event -> JavaFxApplication.openUrl(JavaFxApplication.getControlUrl() + "/h2-console"));
		}

		exitApplication.setOnAction(event ->
		{
			windowManager.closeAllWindows();
			Platform.exit();
		});

		setupStatusNotifications();

		trayService.addSystemTray();

		locationClient.getRSId(OWN_LOCATION_ID, Type.SHORT_INVITE)
				.doOnSuccess(rsIdResponse -> Platform.runLater(() -> shortId.setText(rsIdResponse.rsId())))
				.subscribe();
	}

	@Override
	public void onShown()
	{
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

	private void showQrCode(Window window)
	{
		var rsIdResponse = locationClient.getRSId(OWN_LOCATION_ID, Type.ANY);
		rsIdResponse.subscribe(reply -> Platform.runLater(() -> windowManager.openQrCode(window, reply)));
	}

	public void addPeer(String rsId)
	{
		windowManager.openAddPeer(getWindow(titleLabel), rsId);
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

		notificationDisposable = notificationClient.getStatusNotifications()
				.doOnComplete(() -> log.debug("Notification connection closed"))
				.doOnError(throwable -> log.debug("Notification error: {}", throwable.getMessage()))
				.doOnNext(sse -> Platform.runLater(() -> {
					if (sse.data() != null)
					{
						setUserCount(sse.data().currentUsers(), sse.data().totalUsers());
						setNatStatus(sse.data().natStatus());
						setDhtInfo(sse.data().dhtInfo());
					}
				}))
				.subscribe();
	}

	private void setUserCount(Integer newCurrentUsers, Integer newTotalUsers)
	{
		if (newCurrentUsers != null)
		{
			currentUsers = newCurrentUsers;
		}
		if (newTotalUsers != null)
		{
			totalUsers = newTotalUsers;
		}

		numberOfConnections.setText(currentUsers + "/" + totalUsers);
	}

	private void setNatStatus(NatStatus newNatStatus)
	{
		if (newNatStatus != null)
		{
			natStatus.setStatus(newNatStatus == NatStatus.UPNP ? LedStatus.OK : LedStatus.WARNING);
			switch (newNatStatus)
			{
				case UNKNOWN -> TooltipUtils.install(natStatus, bundle.getString("main.status.nat.unknown"));
				case FIREWALLED -> TooltipUtils.install(natStatus, bundle.getString("main.status.nat.firewalled"));
				case UPNP -> TooltipUtils.install(natStatus, bundle.getString("main.status.nat.upnp"));
			}
		}
	}

	private void setDhtInfo(DhtInfo newDhtInfo)
	{
		if (newDhtInfo != null)
		{
			switch (newDhtInfo.dhtStatus())
			{
				case OFF ->
				{
					dhtStatus.setState(false);
					TooltipUtils.install(dhtStatus, bundle.getString("main.status.dht.disabled"));
				}
				case INITIALIZING ->
				{
					dhtStatus.setState(true);
					dhtStatus.setStatus(LedStatus.WARNING);
					TooltipUtils.install(dhtStatus, bundle.getString("main.status.dht.initializing"));
				}
				case RUNNING ->
				{
					dhtStatus.setState(true);
					dhtStatus.setStatus(LedStatus.OK);
					if (newDhtInfo.numPeers() == 0)
					{
						TooltipUtils.install(dhtStatus, bundle.getString("main.status.dht.running"));
					}
					else
					{
						TooltipUtils.install(dhtStatus,
								MessageFormat.format(bundle.getString("main.status.dht.stats"),
										newDhtInfo.numPeers(),
										newDhtInfo.receivedPackets(),
										ByteUnitUtils.fromBytes(newDhtInfo.receivedBytes()),
										newDhtInfo.sentPackets(),
										ByteUnitUtils.fromBytes(newDhtInfo.sentBytes()),
										newDhtInfo.keyCount(),
										newDhtInfo.itemCount()));
					}
				}
			}
		}
	}

	@EventListener
	public void onApplicationEvent(ContextClosedEvent ignored)
	{
		if (notificationDisposable != null && !notificationDisposable.isDisposed())
		{
			notificationDisposable.dispose();
		}
	}
}
