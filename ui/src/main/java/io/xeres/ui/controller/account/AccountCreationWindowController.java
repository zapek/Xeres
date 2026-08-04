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
import io.xeres.ui.support.util.*;
import io.xeres.ui.support.window.WindowManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.css.PseudoClass;
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
import me.gosimple.nbvcxz.resources.ConfigurationBuilder;
import me.gosimple.nbvcxz.resources.Dictionary;
import me.gosimple.nbvcxz.resources.DictionaryBuilder;
import me.gosimple.nbvcxz.scoring.Result;
import me.gosimple.nbvcxz.scoring.TimeEstimate;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import static io.xeres.ui.controller.help.HelpWindowController.SECTION_GETTING_STARTED;
import static io.xeres.ui.support.util.UiUtils.getWindow;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@FxmlView(value = "/view/account/account_creation.fxml")
public class AccountCreationWindowController implements WindowController
{
	public static final int MINIMUM_PASSWORD_LENGTH = 3; // 4 would be better but RS uses that, and we can import profiles from it so...
	public static final int MAXIMUM_PASSWORD_LENGTH = 128;
	private static final int PASSWORD_DEBOUNCE_MILLIS = 500;

	private static final PseudoClass riskyPseudoClass = PseudoClass.getPseudoClass("risky");
	private static final PseudoClass modestPseudoClass = PseudoClass.getPseudoClass("modest");
	private static final PseudoClass strongPseudoClass = PseudoClass.getPseudoClass("strong");

	private static final KeyCombination HELP_SHORTCUT = new KeyCodeCombination(
			KeyCode.F1
	);
	private EventHandler<KeyEvent> keyEventHandler;

	@FXML
	private Button okButton;

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
		profileName.textProperty().addListener(_ -> checkOkButton());
		locationName.textProperty().addListener(_ -> checkOkButton());
		password.textProperty().addListener(_ -> checkPassword());
		passwordConfirm.textProperty().addListener(_ -> checkOkButton());

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
		passwordDebounce.setOnFinished(_ -> checkPasswordStrength());
		okButton.setOnAction(_ ->
		{
			var profileNameText = profileName.getText();
			var locationNameText = locationName.getText();
			if (isNotBlank(profileNameText) && isNotBlank(locationNameText) && password.getLength() > 0 && passwordConfirm.getLength() > 0 && password.getText().equals(passwordConfirm.getText()))
			{
				var pass = password.getPassword();
				if (pass.length() > MAXIMUM_PASSWORD_LENGTH)
				{
					Requester.showError(MessageFormat.format(bundle.getString("account.password-too-long"), MAXIMUM_PASSWORD_LENGTH));
					return;
				}
				generateProfileAndLocation(profileNameText, locationNameText, new ScrambledString(pass));
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
							if (!Requester.ask(MessageFormat.format(bundle.getString("account.import-new-location-name.confirm"), locationName)))
							{
								return;
							}
						}
					}
					status.setText(bundle.getString("account.generation.import.progress"));
					setInProgress(true);
					var dialog = new TextInputDialog();
					dialog.setTitle(bundle.getString("account.generation.import.confirm.title"));
					dialog.setHeaderText(null);
					dialog.setContentText(bundle.getString("account.generation.import.confirm.prompt"));
					dialog.initOwner(UiUtils.getWindow(event));
					dialog.showAndWait().ifPresent(response -> configClient.sendBackup(selectedFile, locationName.getText(), new ScrambledString(response))
							.doOnSuccess(_ -> Platform.runLater(() -> Platform.runLater(this::openDashboard)))
							.doOnError(throwable -> {
								UiUtils.webAlertError(throwable);
								setInProgress(false);
								status.setText(null);
							})
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
		checkOkButton();
		passwordDebounce.playFromStart();
	}

	private void checkPasswordStrength()
	{
		if (nbvcxz == null)
		{
			List<Dictionary> dictionaryList = ConfigurationBuilder.getDefaultDictionaries();
			dictionaryList.add(new DictionaryBuilder()
					.setDictionaryName("exclude")
					.setExclusion(true)
					.addWord(profileName.getText(), 0)
					.addWord(locationName.getText(), 0)
					.createDictionary()
			);

			BigDecimal mooreMultiplier = ConfigurationBuilder.getMooresMultiplier();
			long crackingHardwareCost = ConfigurationBuilder.getDefaultCrackingHardwareCost();
			BigDecimal costMultiplier = BigDecimal.valueOf(crackingHardwareCost).divide(BigDecimal.valueOf(crackingHardwareCost), 5, RoundingMode.HALF_UP);

			var configuration = new ConfigurationBuilder()
					.setLocale(Locale.getDefault())
					.setMaxLength(MAXIMUM_PASSWORD_LENGTH)
					.setGuessTypes(Map.of("PGP", costMultiplier.multiply(mooreMultiplier.multiply(BigDecimal.valueOf(3_900_000_000L / 65_536))).longValue()))
					.createConfiguration();

			nbvcxz = new Nbvcxz(configuration);
		}

		String pass = password.getPassword();

		if (pass.isEmpty())
		{
			TooltipUtils.uninstall(passwordStrength);
			TooltipUtils.uninstall(passwordStrengthLabel);
			TooltipUtils.uninstall(password);
			passwordStrength.setProgress(0.0);
			passwordStrengthLabel.setText(null);
		}
		else
		{
			CompletableFuture.supplyAsync(() -> nbvcxz.estimate(pass))
					.thenAccept(result -> Platform.runLater(() -> {
						var secondsToCrack = TimeEstimate.getTimeToCrack(result, "PGP");

						var strength = PasswordStrength.getStrength(secondsToCrack);
						passwordStrength.setProgress((1.0 + strength.ordinal()) / PasswordStrength.values().length);
						setPasswordStrength(strength);
						passwordStrengthLabel.setText(strength.toString());
						var recommendations = getPasswordRecommendations(result);
						TooltipUtils.install(passwordStrength, recommendations);
						TooltipUtils.install(passwordStrengthLabel, recommendations);
						TooltipUtils.install(password, recommendations);
					}));
		}
	}

	private String getPasswordRecommendations(Result result)
	{
		var sb = new StringBuilder();

		var feedback = result.getFeedback();

		if (feedback.getWarning() != null)
		{
			sb.append(bundle.getString("account.password.warning")).append(" ").append(feedback.getWarning()).append("\n");
		}
		for (String suggestion : feedback.getSuggestion())
		{
			sb.append(bundle.getString("account.password.suggestion")).append(" ").append(suggestion).append("\n");
		}
		sb.append(bundle.getString("account.password.time-to-crack")).append(" ").append(TimeEstimate.getTimeToCrackFormatted(result, "PGP"));
		return sb.toString();
	}

	private void setPasswordStrength(PasswordStrength strength)
	{
		passwordStrength.pseudoClassStateChanged(riskyPseudoClass, strength == PasswordStrength.VERY_WEAK || strength == PasswordStrength.WEAK);
		passwordStrength.pseudoClassStateChanged(modestPseudoClass, strength == PasswordStrength.GOOD);
		passwordStrength.pseudoClassStateChanged(strongPseudoClass, strength == PasswordStrength.STRONG);
	}

	private void checkOkButton()
	{
		okButton.setDisable(
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
		getWindow(okButton).addEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);
		getWindow(okButton).setOnCloseRequest(_ -> Platform.exit());
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

	public void generateProfileAndLocation(String profileName, String locationName, ScrambledString passphrase)
	{
		setInProgress(true);

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
		profileClient.getOwn().doOnSuccess(profile -> Platform.runLater(() -> {
					windowManager.openMain(null, profile, false);
					getWindow(profileName).hide();
				}))
				.subscribe();

	}
}
