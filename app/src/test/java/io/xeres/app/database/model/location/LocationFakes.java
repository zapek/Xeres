/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.database.model.location;

import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.common.id.LocationId;
import io.xeres.common.protocol.NetMode;
import io.xeres.testutils.StringFakes;

import java.util.concurrent.ThreadLocalRandom;

import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;

public final class LocationFakes
{
	private LocationFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static long id = OWN_LOCATION_ID + 1;

	private static long getUniqueId()
	{
		return id++;
	}

	public static Location createOwnLocation()
	{
		return new Location(OWN_LOCATION_ID, StringFakes.createNickname(), ProfileFakes.createProfile(), new LocationId(getRandomArray()));
	}

	public static Location createLocation()
	{
		return createLocation(StringFakes.createNickname(), ProfileFakes.createProfile(), new LocationId(getRandomArray()));
	}

	public static Location createLocation(String name, Profile profile)
	{
		return createLocation(name, profile, new LocationId(getRandomArray()));
	}

	public static Location createLocation(String name, Profile profile, LocationId locationId)
	{
		var location = new Location(getUniqueId(), name, profile, locationId);
		location.setNetMode(NetMode.UPNP);
		location.setVersion("Xeres 0.1.1");
		return location;
	}

	private static byte[] getRandomArray()
	{
		var a = new byte[16];
		ThreadLocalRandom.current().nextBytes(a);
		return a;
	}
}
