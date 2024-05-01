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

class FileHash
{
	private final Set<Integer> tunnels = ConcurrentHashMap.newKeySet(); // XXX: probably doesn't need to be concurrent?
	private int lastRequest;
	private Instant lastTime;
	private final TurtleRsClient client;
	private boolean aggressiveMode;

	public FileHash(boolean aggressiveMode, TurtleRsClient client)
	{
		lastTime = Instant.now();
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
}
