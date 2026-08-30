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

/// Note: this class has a natural ordering that is inconsistent with equals.
abstract class FilePeer implements Comparable<FilePeer>
{
	private final Location location;

	private Instant nextScheduling = Instant.EPOCH;

	/// Tracks the download rate (data received from this peer).
	private final RateTracker receiveRate = new RateTracker(FileTransferRsService.getRateWindow());

	/// Tracks the upload rate (data sent to this peer).
	private final RateTracker sendRate = new RateTracker(FileTransferRsService.getRateWindow());

	/// The bandwidth advertised by the peer, in bytes per second, or 0 if unknown.
	private long bandwidthBytesPerSecond;

	FilePeer(Location location)
	{
		this.location = location;
	}

	public RateTracker getReceiveRate()
	{
		return receiveRate;
	}

	public RateTracker getSendRate()
	{
		return sendRate;
	}

	public long getBandwidthBytesPerSecond()
	{
		return bandwidthBytesPerSecond;
	}

	public void setBandwidthBytesPerSecond(long bandwidthBytesPerSecond)
	{
		this.bandwidthBytesPerSecond = bandwidthBytesPerSecond;
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
