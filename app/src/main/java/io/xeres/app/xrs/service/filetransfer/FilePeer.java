/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.filetransfer;

import io.xeres.app.database.model.location.Location;

import java.time.Duration;
import java.time.Instant;

/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
abstract class FilePeer implements Comparable<FilePeer>
{
	private final Location location;

	private Instant nextScheduling = Instant.EPOCH;

	FilePeer(Location location)
	{
		this.location = location;
	}

	public Location getLocation()
	{
		return location;
	}

	public Instant getNextScheduling()
	{
		return nextScheduling;
	}

	public void addNextScheduling(Duration duration)
	{
		nextScheduling = Instant.now().plus(duration);
	}

	@Override
	public int compareTo(FilePeer o)
	{
		return nextScheduling.compareTo(o.getNextScheduling());
	}
}
