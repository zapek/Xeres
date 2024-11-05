/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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
import io.xeres.ui.custom.ReadOnlyTextField;
import io.xeres.ui.model.settings.Settings;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static io.xeres.common.properties.StartupProperties.Property.CONTROL_PORT;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@FxmlView(value = "/view/settings/settings_remote.fxml")
public class SettingsRemoteController implements SettingsController
{
	@FXML
	private PasswordTextField password;

	@FXML
	private ReadOnlyTextField port;

	private Settings settings;

	@Override
	public void initialize() throws IOException
	{
		var icon = new FontIcon("mdi2e-eye-off");
		icon.setCursor(Cursor.HAND);
		icon.setOnMouseClicked(mouseEvent -> {
			icon.setIconLiteral(password.getRevealPassword() ? "mdi2e-eye-off" : "mdi2e-eye");
			password.setRevealPassword(!password.getRevealPassword());
		});
		password.setRight(icon);
	}

	@Override
	public void onLoad(Settings settings)
	{
		this.settings = settings;

		password.setText(settings.getRemotePassword());
		port.setText(String.valueOf(StartupProperties.getInteger(CONTROL_PORT)));
	}

	@Override
	public Settings onSave()
	{
		settings.setRemotePassword(isBlank(password.getPassword()) ? null : password.getPassword());

		return settings;
	}
}
