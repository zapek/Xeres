/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.messaging;

import io.xeres.ui.model.location.Location;
import io.xeres.ui.model.profile.Profile;

public class ProfileHolder // XXX: rename it as PeerHolder perhaps...
{
	private Profile profile;
	private Location location;

	public ProfileHolder()
	{

	}

	public ProfileHolder(Profile profile)
	{
		this.profile = profile;
	}

	public ProfileHolder(Profile profile, Location location)
	{
		this.profile = profile;
		this.location = location;
	}

	public Profile getProfile()
	{
		return profile;
	}

	public Location getLocation()
	{
		return location;
	}

	public void setLocation(Location location)
	{
		this.location = location;
	}

	public boolean hasLocation()
	{
		return location != null;
	}
}
