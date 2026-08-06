/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.contact;

import atlantafx.base.controls.PasswordTextField;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.util.ScrambledString;
import io.xeres.ui.client.ConfigClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.util.Requester;
import io.xeres.ui.support.util.TextFieldUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static io.xeres.ui.controller.account.AccountCreationWindowController.MINIMUM_PASSWORD_LENGTH;

@Component
@FxmlView(value = "/view/contact/change_password.fxml")
public class ChangePasswordWindowController implements WindowController
{
	@FXML
	private PasswordTextField oldPassword;

	@FXML
	private PasswordTextField password;

	@FXML
	private PasswordTextField passwordConfirm;

	@FXML
	private Button changeButton;

	@FXML
	private Button cancelButton;

	private final ConfigClient configClient;

	public ChangePasswordWindowController(ConfigClient configClient)
	{
		this.configClient = configClient;
	}


	@Override
	public void initialize() throws IOException
	{
		password.textProperty().addListener(_ -> checkChangeButton());
		passwordConfirm.textProperty().addListener(_ -> checkChangeButton());
		TextFieldUtils.setPasswordReveal(password);
		changeButton.setOnAction(_ -> configClient.changePassphrase(new ScrambledString(oldPassword.getPassword()), new ScrambledString(password.getPassword()))
				.doOnSuccess(_ -> Platform.runLater(() -> {
					Requester.showInfo(I18nUtils.getBundle().getString("contact.password.success"));
					UiUtils.closeWindow(changeButton);
				}))
				.doOnError(UiUtils::webAlertError)
				.subscribe());
		cancelButton.setOnAction(UiUtils::closeWindow);
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(changeButton);
		if (userData != null)
		{
			var emptyPassword = (Boolean) userData;

			if (emptyPassword)
			{
				oldPassword.setDisable(true);
			}
		}
	}

	private void checkChangeButton()
	{
		changeButton.setDisable(
				password.getPassword().isBlank() ||
						password.getPassword().length() < MINIMUM_PASSWORD_LENGTH ||
						!passwordConfirm.getPassword().equals(password.getPassword())
		);
	}
}
