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

package io.xeres.ui.controller.account;

import io.xeres.ui.client.ConfigClient;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@FxmlView(value = "/view/account/account_creation.fxml")
public class AccountCreationWindowController implements WindowController
{
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
	}

	/**
	 * Try to make the hostname better by removing the domain part, if present.
	 * For example, bar.foo.baz -> bar
	 *
	 * @param hostname a hostname
	 * @return a hostname without the domain part
	 */
	private String sanitizeHostname(String hostname)
	{
		return hostname.split("\\.")[0];
	}

	private void setInProgress(boolean inProgress)
	{
		if (inProgress)
		{
			okButton.setDisable(true);
			profileName.setDisable(true);
			locationName.setDisable(true);
			progress.setVisible(true);
		}
		else
		{
			okButton.setDisable(false);
			profileName.setDisable(false);
			locationName.setDisable(false);
			progress.setVisible(false);
		}
	}

	public void generateProfileAndLocation(String profileName, String locationName)
	{
		setInProgress(true);

		var result = configClient.createProfile(profileName);

		status.setText(bundle.getString("account.generation.profile-keys"));

		result.doOnSuccess(aVoid -> Platform.runLater(() -> generateLocation(profileName, locationName)))
				.doOnError(throwable -> Platform.runLater(() -> {
					UiUtils.showError(this.profileName, bundle.getString("account.generation.profile.error"));
					setInProgress(false);
				}))
				.subscribe();
	}

	private void generateLocation(String profileName, String locationName)
	{
		setInProgress(true);

		var result = configClient.createLocation(locationName);

		status.setText(bundle.getString("account.generation.location-keys-and-certificate"));

		result.doOnSuccess(aVoid -> Platform.runLater(() -> generateIdentity(profileName)))
				.doOnError(throwable -> Platform.runLater(() ->
				{
					UiUtils.showAlertError(bundle.getString("account.generation.location.error.title"), bundle.getString("account.generation.location.error.header"), "...");
					setInProgress(false);
				}))
				.subscribe();
	}

	private void generateIdentity(String identityName)
	{
		setInProgress(true);

		var result = configClient.createIdentity(identityName, false);

		status.setText(bundle.getString("account.generation.identity"));

		result.doOnSuccess(identityResponse -> Platform.runLater(this::openDashboard))
				.doOnError(throwable -> Platform.runLater(() ->
				{
					UiUtils.showAlertError(bundle.getString("account.generation.identity.error.title"), bundle.getString("account.generation.identity.error.header"), "...");
					setInProgress(false);
				}))
				.subscribe();
	}

	private void openDashboard()
	{
		profileClient.getOwn().doOnSuccess(profile -> Platform.runLater(() -> {
					windowManager.openMain(null, profile, false);
					profileName.getScene().getWindow().hide();
				}))
				.subscribe();

	}
}
