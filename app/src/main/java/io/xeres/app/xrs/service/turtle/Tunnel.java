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

package io.xeres.app.xrs.service.turtle;

import io.xeres.app.database.model.location.Location;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.Sha1Sum;

import java.time.Instant;

class Tunnel
{
	private Location source;
	private Location destination;
	private Instant lastUsed;
	private long transferred;
	private double speed;
	private Sha1Sum hash;
	private LocationId virtualId;

	public Tunnel(Location source, Location destination, Sha1Sum hash)
	{
		this.source = source;
		this.destination = destination;
		this.hash = hash;
		lastUsed = Instant.now();
	}

	public Tunnel(Location source, Location destination)
	{
		this(source, destination, null);
	}

	public void setVirtualId(LocationId virtualId)
	{
		this.virtualId = virtualId;
	}

	public LocationId getVirtualId()
	{
		return virtualId;
	}

	public Sha1Sum getHash()
	{
		return hash;
	}

	public void setHash(Sha1Sum hash)
	{
		this.hash = hash;
	}

	public double getSpeed()
	{
		return speed;
	}
}
