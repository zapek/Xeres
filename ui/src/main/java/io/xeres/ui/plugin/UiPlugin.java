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

package io.xeres.ui.plugin;

import io.xeres.ui.client.message.MessageClient;
import java.util.List;

/// Contributions are queried only in GUI mode after the Spring context is ready.
public interface UiPlugin
{
	default List<PluginTab> tabs()
	{
		return List.of();
	}

	default List<PluginSettingsPage> settingsPages()
	{
		return List.of();
	}

	default List<PluginIdentityAction> identityActions()
	{
		return List.of();
	}

	default List<PluginNotificationSetting> notificationSettings()
	{
		return List.of();
	}

	default void subscribe(MessageClient messages)
	{
	}

	default void onExit()
	{
	}
}
