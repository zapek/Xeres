/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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
import io.xeres.common.properties.StartupProperties;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.custom.DisclosedHyperlink;
import io.xeres.ui.custom.ReadOnlyTextField;
import io.xeres.ui.model.settings.Settings;
import io.xeres.ui.support.tray.TrayService;
import io.xeres.ui.support.util.TextFieldUtils;
import io.xeres.ui.support.util.UiUtils;
import jakarta.annotation.Nullable;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import static io.xeres.common.properties.StartupProperties.Property.CONTROL_PORT;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@FxmlView(value = "/view/settings/settings_remote.fxml")
public class SettingsRemoteController implements SettingsController
{
	@FXML
	private CheckBox remoteEnabled;

	@FXML
	private PasswordTextField password;

	@FXML
	private DisclosedHyperlink viewApi;

	@FXML
	private TextField port;

	@FXML
	private ReadOnlyTextField username;

	@FXML
	private CheckBox remoteUpnpEnabled;

	private boolean noUpnp;

	private Settings settings;

	private boolean originalRemoteEnabled;

	private String originalPassword;

	private final TrayService trayService;
	private final HostServices hostServices;
	private final ResourceBundle bundle;

	public SettingsRemoteController(TrayService trayService, @Nullable HostServices hostServices, ResourceBundle bundle)
	{
		this.trayService = trayService;
		this.hostServices = hostServices;
		this.bundle = bundle;
	}

	@Override
	public void initialize() throws IOException
	{
		TextFieldUtils.setNumeric(port, 0, 6);

		var icon = new FontIcon("mdi2e-eye-off");
		icon.setCursor(Cursor.HAND);
		icon.setOnMouseClicked(mouseEvent -> {
			icon.setIconLiteral(password.getRevealPassword() ? "mdi2e-eye-off" : "mdi2e-eye");
			password.setRevealPassword(!password.getRevealPassword());
		});
		password.setRight(icon);

		remoteEnabled.setOnAction(actionEvent -> checkDisabled());

		UiUtils.linkify(viewApi, hostServices);
		viewApi.setUri(RemoteUtils.getControlUrl() + "/swagger-ui/index.html");
	}

	@Override
	public void onLoad(Settings settings)
	{
		this.settings = settings;

		noUpnp = !settings.isUpnpEnabled();

		remoteEnabled.setSelected(settings.isRemoteEnabled());
		remoteUpnpEnabled.setSelected(settings.isUpnpRemoteEnabled());
		checkDisabled();
		password.setText(settings.getRemotePassword());
		port.setText(String.valueOf(StartupProperties.getInteger(CONTROL_PORT)));

		originalRemoteEnabled = settings.isRemoteEnabled();
		originalPassword = settings.getRemotePassword();
	}

	@Override
	public Settings onSave()
	{
		var portChanged = false;

		settings.setRemotePassword(isBlank(password.getPassword()) ? null : password.getPassword());
		settings.setRemoteEnabled(remoteEnabled.isSelected());
		settings.setUpnpRemoteEnabled(remoteUpnpEnabled.isSelected());
		if (!port.getText().isEmpty())
		{
			var portValue = Integer.parseInt(port.getText());
			if (portValue >= 1025 && portValue <= 65535 && portValue != Objects.requireNonNull(StartupProperties.getInteger(CONTROL_PORT)))
			{
				settings.setRemotePort(portValue);
				portChanged = true;
			}
		}

		if (originalRemoteEnabled != settings.isRemoteEnabled() || !originalPassword.equals(settings.getRemotePassword()) || portChanged)
		{
			UiUtils.alertConfirm(bundle.getString("settings.remote.restart"), trayService::exitApplication);
		}

		return settings;
	}

	private void checkDisabled()
	{
		var selected = remoteEnabled.isSelected();
		port.setDisable(!selected);
		username.setDisable(!selected);
		password.setDisable(!selected);
		remoteUpnpEnabled.setDisable(noUpnp || !selected);
	}
}
