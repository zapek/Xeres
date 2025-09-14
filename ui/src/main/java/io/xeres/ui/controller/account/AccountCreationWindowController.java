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

package io.xeres.ui.controller.account;

import io.xeres.common.util.OsUtils;
import io.xeres.ui.client.ConfigClient;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

import static io.xeres.ui.support.util.UiUtils.getWindow;
import static javafx.scene.control.Alert.AlertType.ERROR;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@FxmlView(value = "/view/account/account_creation.fxml")
public class AccountCreationWindowController implements WindowController
{
	private static final KeyCombination HELP_SHORTCUT = new KeyCodeCombination(
			KeyCode.F1
	);

	private EventHandler<KeyEvent> keyEventHandler;

	@FXML
	private Button okButton;

	@FXML
	private TextField profileName;

	@FXML
	private TextField locationName;

	@FXML
	private ProgressIndicator progress;

	@FXML
	private Label status;

	@FXML
	private TitledPane titledPane;

	@FXML
	private Button importBackup;

	private final ConfigClient configClient;
	private final ProfileClient profileClient;
	private final WindowManager windowManager;
	private final ResourceBundle bundle;

	public AccountCreationWindowController(ConfigClient configClient, ProfileClient profileClient, WindowManager windowManager, ResourceBundle bundle)
	{
		this.configClient = configClient;
		this.profileClient = profileClient;
		this.windowManager = windowManager;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		profileName.textProperty().addListener(observable -> okButton.setDisable(profileName.getText().isBlank()));
		locationName.textProperty().addListener(observable -> okButton.setDisable(locationName.getText().isBlank()));

		configClient.getUsername()
				.doOnSuccess(usernameResult -> Platform.runLater(() -> profileName.setText(usernameResult.username())))
				.subscribe();

		configClient.getHostname()
				.doOnSuccess(hostnameResult -> Platform.runLater(() -> locationName.setText(sanitizeHostname(hostnameResult.hostname()))))
				.subscribe();

		okButton.setOnAction(actionEvent ->
		{
			var profileNameText = profileName.getText();
			var locationNameText = locationName.getText();
			if (isNotBlank(profileNameText) && isNotBlank(locationNameText))
			{
				generateProfileAndLocation(profileNameText, locationNameText);
			}
		});

		importBackup.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("account.generation.profile-load"));
			fileChooser.setInitialDirectory(OsUtils.getDownloadDir().toFile());
			fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("file-requester.profiles"), "*.xml", "*.gpg", "*.asc"));
			var selectedFile = fileChooser.showOpenDialog(UiUtils.getWindow(event));
			if (selectedFile != null && selectedFile.canRead())
			{
				if (selectedFile.getPath().endsWith(".xml"))
				{
					status.setText(bundle.getString("account.generation.import.progress"));
					setInProgress(true);
					configClient.sendBackup(selectedFile)
							.doOnSuccess(unused -> Platform.runLater(() -> Platform.runLater(this::openDashboard)))
							.doOnError(throwable -> {
								UiUtils.showAlertError(throwable);
								setInProgress(false);
								status.setText(null);
							})
							.subscribe();
				}
				else if (selectedFile.getPath().endsWith(".gpg") || selectedFile.getPath().endsWith(".asc"))
				{
					status.setText(bundle.getString("account.generation.import.progress"));
					setInProgress(true);
					var dialog = new TextInputDialog();
					dialog.setTitle(bundle.getString("account.generation.import.confirm.title"));
					dialog.setHeaderText(null);
					dialog.setContentText(bundle.getString("account.generation.import.confirm.prompt"));
					dialog.initOwner(UiUtils.getWindow(event));
					dialog.showAndWait().ifPresent(response -> configClient.sendRsKeyring(selectedFile, locationName.getText(), response)
							.doOnSuccess(unused -> Platform.runLater(() -> Platform.runLater(this::openDashboard)))
							.doOnError(throwable -> {
								UiUtils.showAlertError(throwable);
								setInProgress(false);
								status.setText(null);
							})
							.subscribe());
				}
				else
				{
					UiUtils.alert(ERROR, bundle.getString("account.generation.import.unknown"));
				}
			}
		});

		keyEventHandler = event -> {
			if (HELP_SHORTCUT.match(event))
			{
				windowManager.openDocumentation(false);
				event.consume();
			}
		};
	}

	@Override
	public void onShown()
	{
		getWindow(okButton).addEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);
		getWindow(okButton).setOnCloseRequest(event -> Platform.exit());
	}

	@Override
	public void onHiding()
	{
		getWindow(okButton).removeEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);
	}

	/**
	 * Try to make the hostname better by removing the domain part, if present.
	 * For example, bar.foo.baz -> bar
	 *
	 * @param hostname a hostname
	 * @return a hostname without the domain part
	 */
	private static String sanitizeHostname(String hostname)
	{
		return hostname.split("\\.")[0];
	}

	private void setInProgress(boolean inProgress)
	{
		okButton.setDisable(inProgress);
		profileName.setDisable(inProgress);
		locationName.setDisable(inProgress);
		importBackup.setDisable(inProgress);
		progress.setVisible(inProgress);
		titledPane.setExpanded(!inProgress);
	}

	public void generateProfileAndLocation(String profileName, String locationName)
	{
		setInProgress(true);

		status.setText(bundle.getString("account.generation.profile-keys"));

		configClient.createProfile(profileName).doOnSuccess(unused -> Platform.runLater(() -> generateLocation(profileName, locationName)))
				.doOnError(e -> Platform.runLater(() -> {
					UiUtils.showAlertError(e);
					setInProgress(false);
					status.setText(null);
				}))
				.subscribe();
	}

	private void generateLocation(String profileName, String locationName)
	{
		setInProgress(true);

		status.setText(bundle.getString("account.generation.location-keys-and-certificate"));

		configClient.createLocation(locationName).doOnSuccess(unused -> Platform.runLater(() -> generateIdentity(profileName)))
				.doOnError(e -> Platform.runLater(() -> {
					UiUtils.showAlertError(e);
					setInProgress(false);
					status.setText(null);
				}))
				.subscribe();
	}

	private void generateIdentity(String identityName)
	{
		setInProgress(true);

		var result = configClient.createIdentity(identityName, false);

		status.setText(bundle.getString("account.generation.identity"));

		result.doOnSuccess(identityResponse -> Platform.runLater(this::openDashboard))
				.doOnError(e -> Platform.runLater(() -> {
					UiUtils.showAlertError(e);
					setInProgress(false);
					status.setText(null);
				}))
				.subscribe();
	}

	private void openDashboard()
	{
		profileClient.getOwn().doOnSuccess(profile -> Platform.runLater(() -> {
					windowManager.openMain(null, profile, false);
					getWindow(profileName).hide();
				}))
				.subscribe();

	}
}
