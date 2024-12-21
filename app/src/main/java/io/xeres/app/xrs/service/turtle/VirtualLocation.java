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

package io.xeres.app.xrs.service.turtle;

import io.xeres.app.database.model.location.Location;
import io.xeres.common.id.LocationIdentifier;

/**
 * Handles Virtual Locations, which are "distant" locations in the Turtle network (it could be your direct peer to, it's impossible to know).
 */
final class VirtualLocation
{
	private VirtualLocation()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Creates a virtual location out of a tunnel id.
	 * <p>
	 * A virtual location performs more or less like a normal location.
	 *
	 * @param tunnelId the tunnel id
	 * @return a virtual location
	 */
	public static Location fromTunnel(int tunnelId)
	{
		var buf = new byte[LocationIdentifier.LENGTH];

		for (var i = 0; i < 4; i++)
		{
			buf[i] = (byte) ((tunnelId >> ((3 - i) * 8)) & 0xff);
		}
		return Location.createLocation("TurtleVirtualLocation", new LocationIdentifier(buf));
	}
}
