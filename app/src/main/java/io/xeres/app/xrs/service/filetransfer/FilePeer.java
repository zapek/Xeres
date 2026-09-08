/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

/// Superclass of [FileLeecher] and [FileSeeder].
/// Note: this class has a natural ordering that is inconsistent with equals.
abstract class FilePeer implements Comparable<FilePeer>
{
	private final Location location;

	private final RateTracker rateTracker = new RateTracker(5000); // XXX: 5 seconds... make that settable?

	private Instant nextScheduling = Instant.MAX;

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

	/// Adds a next scheduled. Is only taken into account if the supplied duration would make
	/// a schedule fire before the currently scheduled one (or if the current one is long past).
	///
	/// @param duration the duration
	public void addNextScheduling(Duration duration)
	{
		var now = Instant.now();
		var newScheduling = now.plus(duration);
		if (newScheduling.isBefore(nextScheduling) || nextScheduling.isBefore(now) || nextScheduling.equals(Instant.MAX))
		{
			nextScheduling = newScheduling;
		}
	}

	public void trackBytes(long bytes)
	{
		rateTracker.addBytes(bytes);
	}

	public long getSpeed()
	{
		return rateTracker.getBytesPerSecond();
	}

	@Override
	public int compareTo(FilePeer o)
	{
		return nextScheduling.compareTo(o.getNextScheduling());
	}
}
