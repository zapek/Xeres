/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

import atlantafx.base.controls.PasswordTextField;
import io.xeres.common.util.OsUtils;
import io.xeres.common.util.ScrambledString;
import io.xeres.ui.client.ConfigClient;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.util.ChooserUtils;
import io.xeres.ui.support.util.Requester;
import io.xeres.ui.support.util.TextFieldUtils;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.animation.PauseTransition;
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
import javafx.util.Duration;
import me.gosimple.nbvcxz.Nbvcxz;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

import static io.xeres.ui.controller.help.HelpWindowController.SECTION_GETTING_STARTED;
import static io.xeres.ui.support.util.TextFieldUtils.*;
import static io.xeres.ui.support.util.UiUtils.getWindow;
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
	private Button createButton;

	@FXML
	private Button helpButton;

	@FXML
	private TextField profileName;

	@FXML
	private TextField locationName;

	@FXML
	private PasswordTextField password;

	@FXML
	private Label passwordStrengthLabel;

	@FXML
	private ProgressBar passwordStrength;

	@FXML
	private PasswordTextField passwordConfirm;

	@FXML
	private ProgressIndicator progress;

	@FXML
	private Label status;

	@FXML
	private TitledPane titledPane;

	@FXML
	private Button importBackup;

	private Nbvcxz nbvcxz;
	private PauseTransition passwordDebounce;
	private boolean inProgress;

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
		profileName.textProperty().addListener(_ -> checkCreateButton());
		locationName.textProperty().addListener(_ -> checkCreateButton());
		password.textProperty().addListener(_ -> checkPassword());
		passwordConfirm.textProperty().addListener(_ -> checkCreateButton());

		configClient.getUsername()
				.doOnSuccess(usernameResult -> Platform.runLater(() -> {
					assert usernameResult != null;
					profileName.setText(usernameResult.username());
				}))
				.subscribe();

		configClient.getHostname()
				.doOnSuccess(hostnameResult -> Platform.runLater(() -> {
					assert hostnameResult != null;
					locationName.setText(sanitizeHostname(hostnameResult.hostname()));
				}))
				.subscribe();

		TextFieldUtils.setPasswordReveal(password);
		passwordStrength.setProgress(0.0);

		passwordDebounce = new PauseTransition(Duration.millis(PASSWORD_DEBOUNCE_MILLIS));
		passwordDebounce.setOnFinished(_ -> nbvcxz = TextFieldUtils.checkPasswordStrength(nbvcxz, List.of(profileName.getText(), locationName.getText()), password, passwordStrengthLabel, passwordStrength));
		createButton.setOnAction(_ ->
		{
			var profileNameText = profileName.getText();
			var locationNameText = locationName.getText();
			if (isNotBlank(profileNameText) && isNotBlank(locationNameText) && password.getLength() > 0 && passwordConfirm.getLength() > 0 && password.getPassword().equals(passwordConfirm.getPassword()))
			{
				if (isPasswordCompliant(password, passwordStrength))
				{
					generateProfileAndLocation(profileNameText, locationNameText, new ScrambledString(password.getPassword()));
				}
			}
		});

		importBackup.setOnAction(event -> {
			if (!Requester.confirm(bundle.getString("account.import.intro")))
			{
				return;
			}
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("account.generation.profile-load"));
			ChooserUtils.setInitialDirectory(fileChooser, OsUtils.getDownloadDir());
			fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("file-requester.profiles"), "*.xml", "*.gpg", "*.asc"));
			var selectedFile = fileChooser.showOpenDialog(UiUtils.getWindow(event));
			if (selectedFile != null && selectedFile.canRead())
			{
				if (selectedFile.getPath().endsWith(".xml"))
				{
					var wantNewLocation = Requester.ask(bundle.getString("account.import.ask-new-location"),
							bundle.getString("account.import.ask-new-location.create"),
							bundle.getString("account.import.ask-new-location.restore"));
					if (wantNewLocation)
					{
						if (StringUtils.isBlank(locationName.getText()))
						{
							Requester.showInfo(bundle.getString("account.import.new-location-missing"));
							return;
						}
						else
						{
							if (!Requester.ask(MessageFormat.format(bundle.getString("account.import-new-location-name.confirm"), locationName.getText())))
							{
								return;
							}
						}
					}
					status.setText(bundle.getString("account.generation.import.progress"));
					setInProgress(true);
					var dialog = new TextInputDialog(); // XXX: this one displays the password in the clear. there should be a custom dialog
					dialog.setTitle(bundle.getString("account.generation.import.confirm.title"));
					dialog.setHeaderText(null);
					dialog.setContentText(bundle.getString("account.generation.import.confirm.prompt"));
					dialog.initOwner(UiUtils.getWindow(event));
					dialog.showAndWait().ifPresent(response -> configClient.sendBackup(selectedFile, wantNewLocation ? locationName.getText() : null, new ScrambledString(response))
							.doOnSuccess(_ -> Platform.runLater(() -> Platform.runLater(this::openDashboard)))
							.doOnError(throwable -> Platform.runLater(() -> {
								UiUtils.webAlertError(throwable);
								setInProgress(false);
								status.setText(null);
							}))
							.subscribe());
				}
				else if (selectedFile.getPath().endsWith(".gpg") || selectedFile.getPath().endsWith(".asc"))
				{
					status.setText(bundle.getString("account.generation.import.progress"));
					setInProgress(true);
					var dialog = new TextInputDialog();
					dialog.setTitle(bundle.getString("account.generation.import-rs.confirm.title"));
					dialog.setHeaderText(null);
					dialog.setContentText(bundle.getString("account.generation.import-rs.confirm.prompt"));
					dialog.initOwner(UiUtils.getWindow(event));
					dialog.showAndWait().ifPresent(response -> configClient.sendRsKeyring(selectedFile, locationName.getText(), new ScrambledString(response))
							.doOnSuccess(_ -> Platform.runLater(() -> Platform.runLater(this::openDashboard)))
							.doOnError(throwable -> {
								UiUtils.webAlertError(throwable);
								setInProgress(false);
								status.setText(null);
							})
							.subscribe());
				}
				else
				{
					Requester.showError(bundle.getString("account.generation.import.unknown"));
				}
			}
		});

		keyEventHandler = event -> {
			if (HELP_SHORTCUT.match(event))
			{
				windowManager.openHelp(false);
				event.consume();
			}
		};
		helpButton.setOnAction(_ -> windowManager.openHelp(false, SECTION_GETTING_STARTED));
	}

	private void checkPassword()
	{
		checkCreateButton();
		passwordDebounce.playFromStart();
	}

	private void checkCreateButton()
	{
		createButton.setDisable(
				inProgress ||
						profileName.getText().isBlank() ||
						locationName.getText().isBlank() ||
						password.getPassword().isBlank() ||
						password.getPassword().length() < MINIMUM_PASSWORD_LENGTH ||
						!passwordConfirm.getPassword().equals(password.getPassword())
		);
	}

	@Override
	public void onShown()
	{
		getWindow(createButton).addEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);
		getWindow(createButton).setOnCloseRequest(_ -> Platform.exit());
	}

	@Override
	public void onHiding()
	{
		getWindow(createButton).removeEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);
	}

	/// Try to make the hostname better by removing the domain part, if present.
	/// For example, bar.foo.baz -> bar
	///
	/// @param hostname a hostname
	/// @return a hostname without the domain part
	private static String sanitizeHostname(String hostname)
	{
		return hostname.split("\\.")[0];
	}

	private void setInProgress(boolean inProgress)
	{
		this.inProgress = inProgress;
		createButton.setDisable(inProgress);
		profileName.setDisable(inProgress);
		locationName.setDisable(inProgress);
		importBackup.setDisable(inProgress);
		progress.setVisible(inProgress);
		titledPane.setExpanded(!inProgress);
	}

	public void generateProfileAndLocation(String profileName, String locationName, ScrambledString passphrase)
	{
		setInProgress(true);
		progress.setProgress(0.25);

		status.setText(bundle.getString("account.generation.profile-keys"));

		configClient.createProfile(profileName, passphrase).doOnSuccess(_ -> Platform.runLater(() -> generateLocation(profileName, locationName, passphrase)))
				.doOnError(e -> Platform.runLater(() -> {
					UiUtils.webAlertError(e);
					setInProgress(false);
					status.setText(null);
				}))
				.subscribe();
	}

	private void generateLocation(String profileName, String locationName, ScrambledString passphrase)
	{
		setInProgress(true);
		progress.setProgress(0.50);

		status.setText(bundle.getString("account.generation.location-keys-and-certificate"));

		configClient.createLocation(locationName, passphrase).doOnSuccess(_ -> Platform.runLater(() -> generateIdentity(profileName, passphrase)))
				.doOnError(e -> Platform.runLater(() -> {
					UiUtils.webAlertError(e);
					setInProgress(false);
					status.setText(null);
				}))
				.subscribe();
	}

	private void generateIdentity(String identityName, ScrambledString passphrase)
	{
		setInProgress(true);
		progress.setProgress(0.75);

		var result = configClient.createIdentity(identityName, false, passphrase);
		passphrase.dispose();

		status.setText(bundle.getString("account.generation.identity"));

		result.doOnSuccess(_ -> Platform.runLater(this::openDashboard))
				.doOnError(e -> Platform.runLater(() -> {
					UiUtils.webAlertError(e);
					setInProgress(false);
					status.setText(null);
				}))
				.subscribe();
	}

	private void openDashboard()
	{
		progress.setProgress(1.0);
		profileClient.getOwn().doOnSuccess(profile -> Platform.runLater(() -> {
					windowManager.openMain(null, profile, false);
					getWindow(profileName).hide();
				}))
				.subscribe();

	}
}
