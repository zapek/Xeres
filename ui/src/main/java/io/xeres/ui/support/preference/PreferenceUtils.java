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

package io.xeres.ui.support.preference;

import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.model.location.Location;

import java.util.prefs.Preferences;

/**
 * Utility class to help get a proper preference so that multiple clients can run concurrently.
 * The path to check is:
 * <ul>
 *     <li>Windows: Registry, HKCU\Software\JavaSoft\Prefs (the '/' in front of capital letters in keys and values is an attempt by Sun to make the registry case sensitive)</li>
 *     <li>Linux: $HOME/.java</li>
 * </ul>
 */
public final class PreferenceUtils
{
	public static final String CONTACTS = "Contacts";
	public static final String CHAT_ROOMS = "Chatrooms";
	public static final String FORUMS = "Forums";
	public static final String NOTIFICATIONS = "Notifications";
	public static final String UPDATE_CHECK = "UpdateCheck";
	public static final String SOUND = "Sound";
	public static final String IMAGE_VIEW = "ImageView";

	private static LocationIdentifier locationIdentifier;

	private PreferenceUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void setLocation(Location location)
	{
		locationIdentifier = location.getLocationIdentifier();
	}

	public static Preferences getPreferences()
	{
		var rootNode = Preferences.userNodeForPackage(JavaFxApplication.class);

		if (RemoteUtils.isRemoteUiClient())
		{
			return rootNode.node("0");
		}
		else
		{
			if (locationIdentifier == null)
			{
				throw new IllegalStateException("Preferences: LocationIdentifier is not set");
			}
			return rootNode.node(locationIdentifier.toString());
		}
	}
}
