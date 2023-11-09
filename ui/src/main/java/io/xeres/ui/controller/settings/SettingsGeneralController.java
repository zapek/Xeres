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

package io.xeres.ui.controller.settings;

import io.xeres.common.rest.config.Capabilities;
import io.xeres.ui.client.ConfigClient;
import io.xeres.ui.model.settings.Settings;
import io.xeres.ui.support.theme.AppTheme;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.prefs.Preferences;

@Component
@FxmlView(value = "/view/settings/settings_general.fxml")
public class SettingsGeneralController implements SettingsController
{
	@FXML
	private ChoiceBox<AppTheme> themeSelector;

	@FXML
	private CheckBox autoStartEnabled;

	@FXML
	private Label autoStartNotAvailable;

	private Settings settings;

	private final ConfigClient configClient;

	public SettingsGeneralController(ConfigClient configClient)
	{
		this.configClient = configClient;
	}

	private static void changeTheme(ObservableValue<? extends AppTheme> observable, AppTheme oldValue, AppTheme newValue)
	{
		try
		{
			Application.setUserAgentStylesheet(newValue.getThemeClass().getDeclaredConstructor().newInstance().getUserAgentStylesheet());
			var preferences = Preferences.userRoot().node("Application");
			preferences.put("Theme", newValue.getName());
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public void initialize()
	{
		themeSelector.getItems().addAll(Arrays.stream(AppTheme.values()).toList());
		var preferences = Preferences.userRoot().node("Application");
		var theme = preferences.get("Theme", AppTheme.PRIMER_LIGHT.getName());
		themeSelector.getSelectionModel().select(AppTheme.findByName(theme));
		themeSelector.getSelectionModel().selectedItemProperty().addListener(SettingsGeneralController::changeTheme);

		configClient.getCapabilities()
				.doOnSuccess(capabilities -> Platform.runLater(() -> {
					if (capabilities.contains(Capabilities.AUTOSTART))
					{
						autoStartEnabled.setDisable(false);
					}
					else
					{
						autoStartNotAvailable.setVisible(true);
					}
				}))
				.subscribe();
	}

	@Override
	public void onLoad(Settings settings)
	{
		this.settings = settings;

		autoStartEnabled.setSelected(settings.isAutoStartEnabled());
	}

	@Override
	public Settings onSave()
	{
		settings.setAutoStartEnabled(autoStartEnabled.isSelected());

		return settings;
	}
}
