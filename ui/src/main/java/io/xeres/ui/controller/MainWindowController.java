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

package io.xeres.ui.controller;

import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import io.xeres.common.mui.MUI;
import io.xeres.common.rest.notification.status.DhtInfo;
import io.xeres.common.rest.notification.status.NatStatus;
import io.xeres.common.rsid.Type;
import io.xeres.common.util.ByteUnitUtils;
import io.xeres.common.util.OsUtils;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.OpenUriEvent;
import io.xeres.ui.client.ConfigClient;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.chat.ChatViewController;
import io.xeres.ui.controller.file.FileMainController;
import io.xeres.ui.custom.DelayedAction;
import io.xeres.ui.custom.ReadOnlyTextField;
import io.xeres.ui.custom.led.LedControl;
import io.xeres.ui.custom.led.LedStatus;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.tray.TrayService;
import io.xeres.ui.support.updater.UpdateService;
import io.xeres.ui.support.uri.ChatRoomUri;
import io.xeres.ui.support.uri.ForumUri;
import io.xeres.ui.support.uri.IdentityUri;
import io.xeres.ui.support.uri.SearchUri;
import io.xeres.ui.support.util.TooltipUtils;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import jakarta.annotation.Nullable;
import javafx.animation.*;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ResourceBundle;

import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;
import static io.xeres.ui.support.util.UiUtils.getWindow;

@Component
@FxmlView(value = "/view/main.fxml")
public class MainWindowController implements WindowController
{
	private static final String XERES_DOCS_URL = "https://xeres.io/docs";
	private static final String XERES_BUGS_URL = "https://github.com/zapek/Xeres/issues/new/choose";

	private static final KeyCombination SHELL_SHORTCUT = new KeyCodeCombination(
			KeyCode.F12
	);

	private static final KeyCombination ONLINE_HELP_SHORTCUT = new KeyCodeCombination(
			KeyCode.F1, KeyCombination.SHORTCUT_DOWN
	);

	private EventHandler<KeyEvent> keyEventHandler;

	@FXML
	private StackPane stackPane;

	@FXML
	private TabPane tabPane;

	@FXML
	private Tab homeTab;

	@FXML
	private Tab chatTab;

	@FXML
	private Tab contactTab;

	@FXML
	private Tab forumTab;

	@FXML
	private Tab fileTab;

	@FXML
	private ImageView logo;

	@FXML
	private Label titleLabel;

	@FXML
	private Label shareId;

	@FXML
	private Label slogan;

	@FXML
	private MenuItem addPeer;

	@FXML
	private MenuItem launchWebInterface;

	@FXML
	private MenuItem launchSwagger;

	@FXML
	private MenuItem exitApplication;

	@FXML
	private MenuItem showDocumentation;

	@FXML
	private MenuItem reportBug;

	@FXML
	private MenuItem showAboutWindow;

	@FXML
	private MenuItem showBroadcastWindow;

	@FXML
	private MenuItem exportBackup;

	@FXML
	private MenuItem importFriends;

	@FXML
	private MenuItem statistics;

	@FXML
	private MenuItem showSettingsWindow;

	@FXML
	private MenuItem showSharesWindow;

	@FXML
	private Menu debug;

	@FXML
	private SeparatorMenuItem debugSeparator;

	@FXML
	private MenuItem runGc;

	@FXML
	private MenuItem h2Console;

	@FXML
	private MenuItem openShell;

	@FXML
	private MenuItem versionCheck;

	@FXML
	private ReadOnlyTextField shortId;

	@FXML
	private Button copyShortIdButton;

	@FXML
	private Button showQrCodeButton;

	@FXML
	private Button addFriendButton;

	@FXML
	private Button webHelpButton;

	@FXML
	private Label numberOfConnections;

	@FXML
	private LedControl natStatus;

	@FXML
	private LedControl dhtStatus;

	@FXML
	private HBox hashingStatus;

	@FXML
	private Label hashingName;

	@FXML
	private FileMainController fileMainController;

	private final ChatViewController chatViewController;

	private final LocationClient locationClient;
	private final TrayService trayService;
	private final WindowManager windowManager;
	private final Environment environment;
	private final ConfigClient configClient;
	private final NotificationClient notificationClient;
	private final HostServices hostServices;
	private final UpdateService updateService;
	private final ResourceBundle bundle;

	private int currentUsers;
	private int totalUsers;
	private Disposable statusNotificationDisposable;
	private Disposable fileNotificationDisposable;

	private DelayedAction hashingDelayedDisplayAction;

	public MainWindowController(ChatViewController chatViewController, LocationClient locationClient, TrayService trayService, WindowManager windowManager, Environment environment, ConfigClient configClient, NotificationClient notificationClient, @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") @Nullable HostServices hostServices, @Lazy UpdateService updateService, ResourceBundle bundle)
	{
		this.chatViewController = chatViewController;
		this.locationClient = locationClient;
		this.trayService = trayService;
		this.windowManager = windowManager;
		this.environment = environment;
		this.configClient = configClient;
		this.notificationClient = notificationClient;
		this.hostServices = hostServices;
		this.updateService = updateService;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		addPeer.setOnAction(event -> windowManager.openAddPeer());
		addFriendButton.setOnAction(event -> windowManager.openAddPeer());

		copyShortIdButton.setOnAction(event -> copyOwnId());

		showQrCodeButton.setOnAction(event -> showQrCode());

		launchWebInterface.setOnAction(event -> openUrl(RemoteUtils.getControlUrl()));
		launchSwagger.setOnAction(event -> openUrl(RemoteUtils.getControlUrl() + "/swagger-ui/index.html"));

		showDocumentation.setOnAction(event -> windowManager.openDocumentation());
		webHelpButton.setOnAction(event -> openUrl(XERES_DOCS_URL));

		reportBug.setOnAction(event -> openUrl(XERES_BUGS_URL));

		showAboutWindow.setOnAction(event -> windowManager.openAbout());

		showBroadcastWindow.setOnAction(event -> windowManager.openBroadcast());

		showSettingsWindow.setOnAction(event -> windowManager.openSettings());

		showSharesWindow.setOnAction(event -> windowManager.openShare());

		exportBackup.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("main.export-profile"));
			fileChooser.setInitialDirectory(OsUtils.getDownloadDir().toFile());
			fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("file-requester.xml"), "*.xml"));
			fileChooser.setInitialFileName("xeres_backup.xml");
			var selectedFile = fileChooser.showSaveDialog(getWindow(event));
			if (selectedFile != null)
			{
				DataBufferUtils.write(configClient.getBackup(), selectedFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).subscribe();
			}
		});

		importFriends.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("main.import-friends"));
			fileChooser.setInitialDirectory(OsUtils.getDownloadDir().toFile());
			fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("file-requester.xml"), "*.xml"));
			var selectedFile = fileChooser.showOpenDialog(getWindow(event));
			if (selectedFile != null && selectedFile.canRead())
			{
				configClient.sendRsFriends(selectedFile)
						.doOnSuccess(unused -> Platform.runLater(() -> UiUtils.alert(Alert.AlertType.INFORMATION, bundle.getString("main.friends-import-successful"))))
						.doOnError(UiUtils::showAlertError)
						.subscribe();
			}
		});

		statistics.setOnAction(event -> windowManager.openStatistics());

		if (environment.acceptsProfiles(Profiles.of("dev")))
		{
			debugSeparator.setVisible(true);
			debug.setVisible(true);
			runGc.setOnAction(event -> System.gc());
			h2Console.setOnAction(event -> openUrl(RemoteUtils.getControlUrl() + "/h2-console"));
			openShell.setOnAction(event -> MUI.openShell());
		}

		versionCheck.setOnAction(even -> updateService.checkForUpdate());

		exitApplication.setOnAction(event -> trayService.exitApplication());

		setupNotifications();

		trayService.addSystemTray(windowManager.getFullTitle());

		locationClient.getRSId(OWN_LOCATION_ID, Type.SHORT_INVITE)
				.doOnSuccess(rsIdResponse -> Platform.runLater(() -> shortId.setText(rsIdResponse.rsId())))
				.subscribe();

		setupAnimations();

		updateService.startBackgroundChecksIfEnabled();

		keyEventHandler = event -> {
			if (SHELL_SHORTCUT.match(event))
			{
				MUI.openShell();
				event.consume();
			}
			else if (ONLINE_HELP_SHORTCUT.match(event))
			{
				webHelpButton.fire();
				event.consume();
			}
		};
	}

	@Override
	public void onShowing()
	{
		fileMainController.resume();
	}

	@Override
	public void onShown()
	{
		windowManager.setRootWindow(getWindow(titleLabel));
		chatViewController.jumpToBottom();

		getWindow(titleLabel).addEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);

		if (!trayService.hasSystemTray())
		{
			UiUtils.getWindow(titleLabel).setOnCloseRequest(event -> {
				UiUtils.alertConfirm(bundle.getString("main.exit.confirm"), () -> UiUtils.getWindow(titleLabel).hide());
				event.consume();
			});
		}
	}

	@Override
	public void onHiding()
	{
		fileMainController.suspend();

		getWindow(titleLabel).removeEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);
	}

	@Override
	public void onHidden()
	{
		if (!trayService.hasSystemTray())
		{
			trayService.exitApplication();
		}
	}

	private void copyOwnId()
	{
		var rsIdResponse = locationClient.getRSId(OWN_LOCATION_ID, Type.ANY);
		rsIdResponse.subscribe(reply -> Platform.runLater(() -> ClipboardUtils.copyTextToClipboard(reply.rsId())));
	}

	private void showQrCode()
	{
		var rsIdResponse = locationClient.getRSId(OWN_LOCATION_ID, Type.ANY);
		rsIdResponse.subscribe(reply -> Platform.runLater(() -> windowManager.openQrCode(reply)));
	}

	public void showUpdate(String message, String tagName, Runnable downloadAction)
	{
		var msg = new Notification(message, new FontIcon(MaterialDesignI.INFORMATION));
		msg.getStyleClass().addAll(Styles.ACCENT, Styles.ELEVATED_2);
		msg.setPrefHeight(Region.USE_PREF_SIZE);
		msg.setMaxHeight(Region.USE_PREF_SIZE);

		var downloadButton = new Button(bundle.getString("download"));
		downloadButton.setDefaultButton(true);
		downloadButton.setOnAction(actionEvent -> downloadAction.run());
		var skipButton = new Button(bundle.getString("skip"));
		skipButton.setOnAction(actionEvent -> updateService.skipUpdate(tagName));
		msg.setPrimaryActions(downloadButton, skipButton);

		StackPane.setAlignment(msg, Pos.TOP_RIGHT);
		StackPane.setMargin(msg, new Insets(0, 10, 10, 0));
		msg.setOnClose(event -> {
			var out = Animations.slideOutUp(msg, javafx.util.Duration.millis(250));
			out.setOnFinished(f -> stackPane.getChildren().remove(msg));
			out.playFromStart();
		});

		var in = Animations.slideInDown(msg, javafx.util.Duration.millis(250));
		stackPane.getChildren().add(msg);
		in.playFromStart();

		// If the window is iconified, un-iconify it
		((Stage) UiUtils.getWindow(stackPane)).show();
	}

	private void setupNotifications()
	{
		// Apparently the LED is not happy if we don't turn it on first here.
		natStatus.setState(true);
		dhtStatus.setState(true);

		statusNotificationDisposable = notificationClient.getStatusNotifications()
				.doOnError(UiUtils::showAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					if (sse.data() != null)
					{
						setUserCount(sse.data().currentUsers(), sse.data().totalUsers());
						setNatStatus(sse.data().natStatus());
						setDhtInfo(sse.data().dhtInfo());
					}
				}))
				.subscribe();

		fileNotificationDisposable = notificationClient.getFileNotifications()
				.doOnError(UiUtils::showAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					if (sse.data() != null)
					{
						switch (sse.data().action())
						{
							case START_SCANNING ->
							{
								var defaultText = MessageFormat.format(bundle.getString("main.scanning"), sse.data().shareName());
								hashingStatus.setVisible(true);
								hashingDelayedDisplayAction = new DelayedAction(() -> hashingName.setText(defaultText), () -> hashingName.setText(null), Duration.ofMillis(2000));
								hashingDelayedDisplayAction.run();
							}
							case START_HASHING ->
							{
								if (hashingDelayedDisplayAction == null) // Can happen when scanning temporary files
								{
									hashingDelayedDisplayAction = new DelayedAction(null, () -> hashingName.setText(null), Duration.ofMillis(2000));
								}
								hashingDelayedDisplayAction.abort();
								hashingName.setText(MessageFormat.format(bundle.getString("main.hashing"), Path.of(sse.data().scannedFile()).getFileName()));
								TooltipUtils.install(hashingStatus, MessageFormat.format(bundle.getString("main.scanning.tip"), sse.data().shareName(), sse.data().scannedFile()));
							}
							case STOP_HASHING ->
							{
								TooltipUtils.uninstall(hashingStatus);
								hashingDelayedDisplayAction.run();
							}
							case STOP_SCANNING ->
							{
								if (hashingDelayedDisplayAction == null) // Can happen when connecting remotely
								{
									return;
								}
								hashingDelayedDisplayAction.abort();
								hashingStatus.setVisible(false);
								hashingDelayedDisplayAction = null;
							}
							case NONE ->
							{
								// Nothing to do
							}
						}
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
			switch (newNatStatus)
			{
				case UNKNOWN ->
				{
					TooltipUtils.install(natStatus, bundle.getString("main.status.nat.unknown"));
					natStatus.setStatus(LedStatus.WARNING);
				}
				case FIREWALLED ->
				{
					TooltipUtils.install(natStatus, bundle.getString("main.status.nat.firewalled"));
					natStatus.setStatus(LedStatus.ERROR);
				}
				case UPNP ->
				{
					TooltipUtils.install(natStatus, bundle.getString("main.status.nat.upnp"));
					natStatus.setStatus(LedStatus.OK);
				}
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
	public void handleOpenUriEvents(OpenUriEvent event)
	{
		switch (event.uri())
		{
			case ChatRoomUri ignored -> tabPane.getSelectionModel().select(chatTab);
			case ForumUri ignored -> tabPane.getSelectionModel().select(forumTab);
			case SearchUri ignored -> tabPane.getSelectionModel().select(fileTab);
			case IdentityUri ignored -> tabPane.getSelectionModel().select(contactTab);
			default ->
			{
				// Nothing to do
			}
		}
	}

	@EventListener
	public void onApplicationEvent(ContextClosedEvent ignored)
	{
		if (statusNotificationDisposable != null && !statusNotificationDisposable.isDisposed())
		{
			statusNotificationDisposable.dispose();
		}

		if (fileNotificationDisposable != null && !fileNotificationDisposable.isDisposed())
		{
			fileNotificationDisposable.dispose();
		}
	}

	private void setupAnimations()
	{
		var rotateTransition = new RotateTransition(javafx.util.Duration.millis(2000), logo);
		rotateTransition.setByAngle(360);
		rotateTransition.setCycleCount(Animation.INDEFINITE);
		rotateTransition.setInterpolator(Interpolator.LINEAR);

		var scaleTransition = new ScaleTransition(javafx.util.Duration.millis(200), titleLabel);
		scaleTransition.setByX(0.2);
		scaleTransition.setByY(0.2);
		scaleTransition.setAutoReverse(true);
		scaleTransition.setCycleCount(Animation.INDEFINITE);
		scaleTransition.setInterpolator(Interpolator.EASE_BOTH);

		var fadeTransition = new FadeTransition(javafx.util.Duration.millis(100), slogan);
		fadeTransition.setByValue(-1.0);
		fadeTransition.setAutoReverse(true);
		fadeTransition.setCycleCount(Animation.INDEFINITE);
		fadeTransition.setInterpolator(Interpolator.EASE_BOTH);

		var translateTransitionLeft = new TranslateTransition(javafx.util.Duration.millis(300));
		translateTransitionLeft.setFromX(0.0);
		translateTransitionLeft.setToX(-80.0);
		translateTransitionLeft.setAutoReverse(true);
		translateTransitionLeft.setCycleCount(2);
		translateTransitionLeft.setInterpolator(Interpolator.LINEAR);

		var translateTransitionRight = new TranslateTransition(javafx.util.Duration.millis(300));
		translateTransitionRight.setFromX(0.0);
		translateTransitionRight.setToX(+80.0);
		translateTransitionRight.setAutoReverse(true);
		translateTransitionRight.setCycleCount(2);
		translateTransitionRight.setInterpolator(Interpolator.LINEAR);

		var sequentialTransition = new SequentialTransition(translateTransitionLeft, translateTransitionRight);
		sequentialTransition.setNode(shareId);
		sequentialTransition.setCycleCount(Animation.INDEFINITE);

		UiUtils.setOnPrimaryMouseDoubleClicked(logo, event -> {
			if (rotateTransition.getStatus() == Animation.Status.RUNNING)
			{
				rotateTransition.jumpTo(javafx.util.Duration.millis(0));
				rotateTransition.stop();

				scaleTransition.jumpTo(javafx.util.Duration.millis(0));
				scaleTransition.stop();

				fadeTransition.jumpTo(javafx.util.Duration.millis(0));
				fadeTransition.stop();

				sequentialTransition.jumpTo(javafx.util.Duration.millis(0));
				sequentialTransition.stop();
			}
			else
			{
				rotateTransition.play();
				scaleTransition.play();
				fadeTransition.play();
				sequentialTransition.play();
			}
		});
	}

	private void openUrl(String url)
	{
		if (hostServices != null)
		{
			hostServices.showDocument(url);
		}
	}
}
