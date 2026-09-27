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

import io.xeres.ui.model.settings.Settings;
import io.xeres.ui.support.notification.NotificationSettings;
import io.xeres.ui.plugin.PluginNotificationSetting;
import io.xeres.ui.plugin.PluginUiService;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.scene.control.CheckBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

@Component
@FxmlView(value = "/view/settings/settings_notifications.fxml")
public class SettingsNotificationController implements SettingsController
{
	@FXML
	private CheckBox showConnections;

	@FXML
	private CheckBox showBroadcasts;

	@FXML
	private CheckBox showDiscovery;

	@FXML
	private GridPane notificationOptions;

	private final PluginUiService plugins;
	private final Map<PluginNotificationSetting, CheckBox> pluginOptions = new LinkedHashMap<>();
	private final NotificationSettings notificationSettings;

	public SettingsNotificationController(NotificationSettings notificationSettings, PluginUiService plugins)
	{
		this.notificationSettings = notificationSettings;
		this.plugins = plugins;
	}

	@Override
	public void initialize()
	{
		for (var option : plugins.notificationSettings())
		{
			var checkbox = new CheckBox(option.title());
			checkbox.setId("plugin." + option.id());
			var tooltip = new Tooltip(option.tooltip());
			tooltip.setShowDuration(Duration.minutes(1));
			tooltip.setMaxWidth(300);
			tooltip.setWrapText(true);
			checkbox.setTooltip(tooltip);
			notificationOptions.add(checkbox, 0, notificationOptions.getRowCount(), GridPane.REMAINING, 1);
			pluginOptions.put(option, checkbox);
		}
	}

	@Override
	public void onLoad(Settings settings)
	{
		showConnections.setSelected(notificationSettings.isConnectionEnabled());
		showBroadcasts.setSelected(notificationSettings.isBroadcastsEnabled());
		showDiscovery.setSelected(notificationSettings.isDiscoveryEnabled());
		pluginOptions.forEach((option, checkbox) -> checkbox.setSelected(option.read().getAsBoolean()));
	}

	@Override
	public Settings onSave()
	{
		notificationSettings.setConnectionEnabled(showConnections.isSelected());
		notificationSettings.setBroadcastsEnabled(showBroadcasts.isSelected());
		notificationSettings.setDiscoveryEnabled(showDiscovery.isSelected());

		notificationSettings.save();
		pluginOptions.forEach((option, checkbox) -> option.write().accept(checkbox.isSelected()));
		return null;
	}
}
