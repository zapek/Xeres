/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

@Component
@FxmlView(value = "/view/settings/settings.fxml")
public class SettingsWindowController implements WindowController
{
	private static final int PREFERENCE_ICON_SIZE = 24;

	private final SettingsClient settingsClient;

	@FXML
	private ListView<SettingsGroup> listView;

	private Settings originalSettings;
	private Settings newSettings;

	@FXML
	private AnchorPane content;

	private final FxWeaver fxWeaver;
	private final ResourceBundle bundle;

	public SettingsWindowController(SettingsClient settingsClient, FxWeaver fxWeaver, ResourceBundle bundle)
	{
		this.settingsClient = settingsClient;
		this.fxWeaver = fxWeaver;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		listView.setCellFactory(param -> new SettingsCell());

		listView.getItems().addAll(
				new SettingsGroup(bundle.getString("settings.general"), createPreferenceGraphic("mdi2c-cog"), SettingsGeneralController.class),
				new SettingsGroup(bundle.getString("settings.notifications"), createPreferenceGraphic("mdi2m-message-alert"), SettingsNotificationController.class),
				new SettingsGroup(bundle.getString("settings.network"), createPreferenceGraphic("mdi2s-server-network"), SettingsNetworksController.class),
				new SettingsGroup(bundle.getString("settings.transfer"), createPreferenceGraphic("mdi2b-briefcase-download"), SettingsTransferController.class),
				new SettingsGroup(bundle.getString("settings.sound"), createPreferenceGraphic("mdi2m-music"), SettingsSoundController.class),
				new SettingsGroup(bundle.getString("settings.remote"), createPreferenceGraphic("mdi2e-earth"), SettingsRemoteController.class)
		);

		listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			saveContent();

			content.getChildren().clear();
			if (newValue.controllerClass() != null)
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
					originalSettings = settings;
					newSettings = originalSettings.clone();
					listView.setDisable(false);
					listView.getSelectionModel().selectFirst();
				}))
				.subscribe();
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
