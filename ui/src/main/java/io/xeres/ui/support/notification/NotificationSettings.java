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

package io.xeres.ui.support.notification;

import io.xeres.ui.support.preference.PreferenceUtils;
import org.springframework.stereotype.Service;

import static io.xeres.ui.support.preference.PreferenceUtils.NOTIFICATIONS;

@Service
public class NotificationSettings
{
	private static final String ENABLE_BROADCAST = "EnableBroadcast";
	private static final String ENABLE_CONNECTION = "EnableConnection";
	private static final String ENABLE_DISCOVERY = "EnableDiscovery";

	private boolean broadcastsEnabled;
	private boolean connectionEnabled;
	private boolean discoveryEnabled;

	private boolean loaded;

	public NotificationSettings()
	{
	}

	public boolean isBroadcastsEnabled()
	{
		loadIfNeeded();
		return broadcastsEnabled;
	}

	public void setBroadcastsEnabled(boolean broadcastsEnabled)
	{
		this.broadcastsEnabled = broadcastsEnabled;
	}

	public boolean isConnectionEnabled()
	{
		loadIfNeeded();
		return connectionEnabled;
	}

	public void setConnectionEnabled(boolean connectionEnabled)
	{
		this.connectionEnabled = connectionEnabled;
	}

	public boolean isDiscoveryEnabled()
	{
		loadIfNeeded();
		return discoveryEnabled;
	}

	public void setDiscoveryEnabled(boolean discoveryEnabled)
	{
		this.discoveryEnabled = discoveryEnabled;
	}

	private void loadIfNeeded()
	{
		if (loaded)
		{
			return;
		}
		var node = PreferenceUtils.getPreferences().node(NOTIFICATIONS);
		broadcastsEnabled = node.getBoolean(ENABLE_BROADCAST, true);
		connectionEnabled = node.getBoolean(ENABLE_CONNECTION, false);
		discoveryEnabled = node.getBoolean(ENABLE_DISCOVERY, true);

		loaded = true;
	}

	public void save()
	{
		var node = PreferenceUtils.getPreferences().node(NOTIFICATIONS);
		node.putBoolean(ENABLE_BROADCAST, broadcastsEnabled);
		node.putBoolean(ENABLE_CONNECTION, connectionEnabled);
		node.putBoolean(ENABLE_DISCOVERY, discoveryEnabled);
	}
}
