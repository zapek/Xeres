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

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of the activity for the file hashes that the turtle router is asked to monitor.
 */
class HashInfo
{
	private final Set<Integer> tunnels = ConcurrentHashMap.newKeySet();
	private int lastRequest;
	private Instant lastDiggTime;
	private final TurtleRsClient client;
	private final boolean aggressiveMode; // if set, allows creation of concurrent tunnels (for example 4 tunnels to download 1 file)

	/**
	 * Creates a HashInfo to keep track of the activity regarding a file hash, thus is usually paired with one.
	 *
	 * @param aggressiveMode if true, allow the use of multiple tunnels for one hash
	 * @param client         the {@link TurtleRsClient}
	 */
	public HashInfo(boolean aggressiveMode, TurtleRsClient client)
	{
		lastDiggTime = Instant.EPOCH;
		this.client = client;
		this.aggressiveMode = aggressiveMode;
	}

	public int getLastRequest()
	{
		return lastRequest;
	}

	public void setLastRequest(int lastRequest)
	{
		this.lastRequest = lastRequest;
	}

	public void addTunnel(int tunnelId)
	{
		tunnels.add(tunnelId);
	}

	public TurtleRsClient getClient()
	{
		return client;
	}

	public Set<Integer> getTunnels()
	{
		return tunnels;
	}

	public void removeTunnel(int tunnelId)
	{
		tunnels.remove(tunnelId);
	}

	public boolean hasTunnels()
	{
		return !tunnels.isEmpty();
	}

	public Instant getLastDiggTime()
	{
		return lastDiggTime;
	}

	public void setLastDiggTime(Instant lastDiggTime)
	{
		this.lastDiggTime = lastDiggTime;
	}

	public boolean isAggressiveMode()
	{
		return aggressiveMode;
	}
}
