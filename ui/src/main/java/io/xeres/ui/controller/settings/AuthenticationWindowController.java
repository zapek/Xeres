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

package io.xeres.ui.controller.settings;

import atlantafx.base.controls.PasswordTextField;
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

import java.util.ResourceBundle;
import java.util.function.Consumer;

@Component
@FxmlView(value = "/view/settings/authentication.fxml")
public class AuthenticationWindowController implements WindowController
{
	@FXML
	private PasswordTextField password;

	@FXML
	private Button okButton;

	@FXML
	private Button cancelButton;

	private Consumer<Boolean> result;

	private final ConfigClient configClient;
	private final ResourceBundle bundle;

	public AuthenticationWindowController(ConfigClient configClient, ResourceBundle bundle)
	{
		this.configClient = configClient;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		TextFieldUtils.setPasswordReveal(password);
		okButton.setOnAction(_ -> configClient.authenticate(new ScrambledString(password.getPassword()))
				.doOnSuccess(_ -> Platform.runLater(() -> {
					result.accept(true);
					UiUtils.closeWindow(okButton);
				}))
				.doOnError(_ -> Platform.runLater(() -> {
					result.accept(false);
					Requester.showError(bundle.getString("authentication.failure"));
					UiUtils.closeWindow(okButton);
				}))
				.subscribe());
		cancelButton.setOnAction(_ -> {
			result.accept(false);
			UiUtils.closeWindow(cancelButton);
		});
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(okButton);
		if (userData != null)
		{
			//noinspection unchecked
			result = (Consumer<Boolean>) userData;
		}
		UiUtils.getWindow(cancelButton).setOnCloseRequest(_ -> result.accept(false));
	}
}
