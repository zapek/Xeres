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

import io.xeres.common.Features;
import io.xeres.ui.client.SettingsClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.settings.Settings;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

import static io.xeres.ui.controller.help.HelpWindowController.*;

@Component
@FxmlView(value = "/view/settings/settings.fxml")
public class SettingsWindowController implements WindowController
{
	private static final int PREFERENCE_ICON_SIZE = 24;

	private final SettingsClient settingsClient;
	private final WindowManager windowManager;
	private final FxWeaver fxWeaver;
	private final ResourceBundle bundle;

	@FXML
	private ListView<SettingsGroup> listView;

	@FXML
	private Button helpButton;

	private Settings originalSettings;
	private Settings newSettings;

	@FXML
	private AnchorPane content;

	public SettingsWindowController(SettingsClient settingsClient, WindowManager windowManager, FxWeaver fxWeaver, ResourceBundle bundle)
	{
		this.settingsClient = settingsClient;
		this.windowManager = windowManager;
		this.fxWeaver = fxWeaver;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		listView.setCellFactory(_ -> new SettingsCell());

		listView.getItems().addAll(
				new SettingsGroup(bundle.getString("settings.general"), createPreferenceGraphic("mdi2c-cog"), SettingsGeneralController.class, SECTION_SETTINGS_GENERAL),
				new SettingsGroup(bundle.getString("settings.notifications"), createPreferenceGraphic("mdi2m-message-alert"), SettingsNotificationController.class, SECTION_SETTINGS_NOTIFICATIONS),
				new SettingsGroup(bundle.getString("settings.network"), createPreferenceGraphic("mdi2s-server-network"), SettingsNetworksController.class, SECTION_SETTINGS_NETWORK),
				new SettingsGroup(bundle.getString("settings.transfer"), createPreferenceGraphic("mdi2b-briefcase-download"), SettingsTransferController.class, SECTION_SETTINGS_TRANSFER),
				new SettingsGroup(bundle.getString("settings.media"), createPreferenceGraphic("mdi2m-multimedia"), SettingsMediaController.class, SECTION_SETTINGS_MEDIA),
				new SettingsGroup(bundle.getString("settings.sound"), createPreferenceGraphic("mdi2m-music"), SettingsSoundController.class, SECTION_SETTINGS_SOUND),
				new SettingsGroup(bundle.getString("settings.remote"), createPreferenceGraphic("mdi2e-earth"), SettingsRemoteController.class, SECTION_SETTINGS_REMOTE)
		);

		listView.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
			saveContent();

			content.getChildren().clear();
			if (newValue != null && newValue.controllerClass() != null)
			{
				var controllerAndView = fxWeaver.load(newValue.controllerClass(), bundle);
				controllerAndView.getController().onLoad(newSettings);

				var view = controllerAndView.getView().orElseThrow();

				content.getChildren().add(view);
				AnchorPane.setTopAnchor(view, 0.0);
				AnchorPane.setBottomAnchor(view, 0.0);
				AnchorPane.setLeftAnchor(view, 0.0);
				AnchorPane.setRightAnchor(view, 0.0);

				view.setUserData(controllerAndView.getController());
			}
		});

		listView.setDisable(true);

		settingsClient.getSettings().doOnSuccess(settings -> Platform.runLater(() -> {
					assert settings != null;
					originalSettings = settings;
					newSettings = originalSettings.clone();
					listView.setDisable(false);
					listView.getSelectionModel().selectFirst();
				}))
				.subscribe();

		helpButton.setOnAction(this::showHelp);
	}

	private void showHelp(ActionEvent event)
	{
		var settingsGroup = listView.getSelectionModel().getSelectedItem();
		if (settingsGroup != null)
		{
			if (StringUtils.isNotEmpty(settingsGroup.helpSection()))
			{
				windowManager.openHelp(settingsGroup.helpSection());
				return;
			}
		}
		windowManager.openHelp(SECTION_SETTINGS);
	}

	@Override
	public void onHidden()
	{
		saveContent();

		if (newSettings != null)
		{
			if (Features.USE_PATCH_SETTINGS)
			{
				settingsClient.patchSettings(originalSettings, newSettings)
						.subscribe();
			}
			else
			{
				settingsClient.putSettings(newSettings)
						.subscribe();
			}
		}
	}

	private void saveContent()
	{
		if (!content.getChildren().isEmpty())
		{
			var controller = (SettingsController) content.getChildren().getFirst().getUserData();
			var controllerSettings = controller.onSave();
			if (controllerSettings != null)
			{
				newSettings = controllerSettings;
			}
		}
	}

	private static Node createPreferenceGraphic(String iconCode)
	{
		var pane = new StackPane(new FontIcon(iconCode));
		pane.setPrefWidth(PREFERENCE_ICON_SIZE);
		pane.setPrefHeight(PREFERENCE_ICON_SIZE);
		pane.setAlignment(Pos.CENTER);
		return pane;
	}
}
