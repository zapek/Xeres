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

package io.xeres.ui.support.preference;

import io.xeres.common.id.LocationId;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.model.location.Location;
import org.springframework.stereotype.Service;

import java.util.prefs.Preferences;

/**
 * Service to help get a proper preference so that multiple clients can run concurrently.
 * The path to check is:
 * <ul>
 *     <li>Windows: Registry, HKCU\Software\JavaSoft\Prefs (the '/' in front of capital letters in keys and values is an attempt by Sun to make the registry case sensitive)</li>
 *     <li>Linux: $HOME/.java</li>
 * </ul>
 */
@Service
public class PreferenceService
{
	public static final String CONTACTS = "Contacts";

	private LocationId locationId;

	public void setLocation(Location location)
	{
		locationId = location.getLocationId();
	}

	public Preferences getPreferences()
	{
		var rootNode = Preferences.userNodeForPackage(JavaFxApplication.class);

		if (RemoteUtils.isRemoteUiClient())
		{
			return rootNode.node("0");
		}
		else
		{
			if (locationId == null)
			{
				return null;
			}
			return rootNode.node(locationId.toString());
		}
	}
}
