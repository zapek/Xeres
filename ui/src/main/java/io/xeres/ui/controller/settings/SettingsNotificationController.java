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
import javafx.fxml.FXML;
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

	private final NotificationSettings notificationSettings;

	public SettingsNotificationController(NotificationSettings notificationSettings)
	{
		this.notificationSettings = notificationSettings;
	}

	@Override
	public void initialize()
	{

	}

	@Override
	public void onLoad(Settings settings)
	{
		showConnections.setSelected(notificationSettings.isConnectionEnabled());
		showBroadcasts.setSelected(notificationSettings.isBroadcastsEnabled());
		showDiscovery.setSelected(notificationSettings.isDiscoveryEnabled());
	}

	@Override
	public Settings onSave()
	{
		notificationSettings.setConnectionEnabled(showConnections.isSelected());
		notificationSettings.setBroadcastsEnabled(showBroadcasts.isSelected());
		notificationSettings.setDiscoveryEnabled(showDiscovery.isSelected());

		notificationSettings.save();
		return null;
	}
}
