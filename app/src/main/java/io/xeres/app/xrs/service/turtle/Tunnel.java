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
import io.xeres.common.id.Sha1Sum;

import java.time.Instant;

/**
 * Keeps track of tunnels.
 */
class Tunnel
{
	private final Location source;
	private final Location destination;
	private final Location virtualLocation;
	private Sha1Sum hash;
	private Instant lastUsed;
	private long transferredBytes;
	private double speedBps;

	/**
	 * Creates a tunnel.
	 *
	 * @param tunnelId    the tunnel id, it will define the virtual location
	 * @param source      where packets come from
	 * @param destination where packets go to (might not be the final recipient, the virtual location is)
	 * @param hash        hash of the file for this tunnel
	 */
	public Tunnel(int tunnelId, Location source, Location destination, Sha1Sum hash)
	{
		this.source = source;
		this.destination = destination;
		this.hash = hash;
		virtualLocation = VirtualLocation.fromTunnel(tunnelId);
		lastUsed = Instant.now();
	}

	public Location getSource()
	{
		return source;
	}

	public Location getDestination()
	{
		return destination;
	}

	public Location getVirtualLocation()
	{
		return virtualLocation;
	}

	public Sha1Sum getHash()
	{
		return hash;
	}

	public void setHash(Sha1Sum hash)
	{
		this.hash = hash;
	}

	public double getSpeedBps()
	{
		return speedBps;
	}

	public void setSpeedBps(double speedBps)
	{
		this.speedBps = speedBps;
	}

	public void addTransferredBytes(long transferredBytes)
	{
		this.transferredBytes += transferredBytes;
	}

	public Instant getLastUsed()
	{
		return lastUsed;
	}

	public long getTransferredBytes()
	{
		return transferredBytes;
	}

	public void clearTransferredBytes()
	{
		transferredBytes = 0;
	}

	public void stamp()
	{
		lastUsed = Instant.now();
	}
}
