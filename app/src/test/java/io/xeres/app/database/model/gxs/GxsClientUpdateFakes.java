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

package io.xeres.app.database.model.gxs;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.common.id.GxsId;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public final class GxsClientUpdateFakes
{
	private GxsClientUpdateFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static GxsClientUpdate createGxsClientUpdate()
	{
		return createGxsClientUpdate(LocationFakes.createLocation(), ThreadLocalRandom.current().nextInt(1, 200));
	}

	public static GxsClientUpdate createGxsClientUpdate(Location location, int serviceType)
	{
		return new GxsClientUpdate(location, serviceType, Instant.now());
	}

	public static GxsClientUpdate createGxsClientUpdateWithMessages(Location location, GxsId groupId, Instant update, int serviceType)
	{
		return new GxsClientUpdate(location, serviceType, groupId, update);
	}
}
