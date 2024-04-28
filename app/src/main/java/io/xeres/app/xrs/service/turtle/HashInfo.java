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

class HashInfo
{
	private final Set<Integer> tunnels = ConcurrentHashMap.newKeySet(); // XXX: probably doesn't need to be concurrent?
	private final int lastRequest;
	private Instant lastTime;
	// XXX: add the rest from TurtleHashInfo

	public HashInfo(int tunnelId)
	{
		lastRequest = tunnelId;
		lastTime = Instant.now();
	}
}
