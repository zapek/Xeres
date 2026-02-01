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

package io.xeres.ui.controller.settings;

import io.xeres.common.rest.config.Capabilities;
import io.xeres.ui.client.ConfigClient;
import io.xeres.ui.model.settings.Settings;
import io.xeres.ui.support.preference.PreferenceUtils;
import io.xeres.ui.support.theme.AppTheme;
import io.xeres.ui.support.theme.AppThemeManager;
import io.xeres.ui.support.updater.UpdateService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static io.xeres.ui.support.preference.PreferenceUtils.UPDATE_CHECK;

@Component
@FxmlView(value = "/view/settings/settings_general.fxml")
public class SettingsGeneralController implements SettingsController
{
	@FXML
	private ChoiceBox<AppTheme> themeSelector;

	@FXML
	private CheckBox autoStartEnabled;

	@FXML
	private CheckBox checkForUpdates;

	@FXML
	private Label autoStartNotAvailable;

	private Settings settings;

	private final ConfigClient configClient;
	private final AppThemeManager appThemeManager;
	private final UpdateService updateService;

	public SettingsGeneralController(ConfigClient configClient, AppThemeManager appThemeManager, UpdateService updateService)
	{
		this.configClient = configClient;
		this.appThemeManager = appThemeManager;
		this.updateService = updateService;
	}

	@Override
	public void initialize()
	{
		themeSelector.getItems().addAll(Arrays.stream(AppTheme.values()).toList());
		var currentTheme = appThemeManager.getCurrentTheme();
		themeSelector.getSelectionModel().select(currentTheme);
		themeSelector.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> appThemeManager.changeTheme(newValue));

		configClient.getCapabilities()
				.doOnSuccess(capabilities -> Platform.runLater(() -> {
					assert capabilities != null;
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

		checkForUpdates.setSelected(updateService.isAutomaticallyCheckingForUpdates(PreferenceUtils.getPreferences().node(UPDATE_CHECK)));
	}

	@Override
	public Settings onSave()
	{
		settings.setAutoStartEnabled(autoStartEnabled.isSelected());

		updateService.setAutomaticCheckForUpdates(PreferenceUtils.getPreferences().node(UPDATE_CHECK), checkForUpdates.isSelected());

		return settings;
	}
}
