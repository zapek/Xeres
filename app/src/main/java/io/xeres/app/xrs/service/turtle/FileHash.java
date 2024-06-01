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

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static io.xeres.app.xrs.service.turtle.TurtleRsService.EMPTY_TUNNELS_DIGGING_TIME;

class FileHash
{
	private final Set<Integer> tunnels = ConcurrentHashMap.newKeySet(); // XXX: probably doesn't need to be concurrent?
	private int lastRequest;
	private Instant lastDiggTime;
	private final TurtleRsClient client;
	private final boolean aggressiveMode;

	public FileHash(boolean aggressiveMode, TurtleRsClient client)
	{
		lastDiggTime = Instant.now().plus(Duration.ofSeconds(ThreadLocalRandom.current().nextLong(EMPTY_TUNNELS_DIGGING_TIME.toSeconds())));
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
