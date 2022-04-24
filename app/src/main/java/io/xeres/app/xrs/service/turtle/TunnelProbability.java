/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import java.util.concurrent.ThreadLocalRandom;

import static io.xeres.app.xrs.service.turtle.TurtleRsService.MAX_TUNNEL_DEPTH;

class TunnelProbability
{
	private final int bias;

	public TunnelProbability()
	{
		bias = ThreadLocalRandom.current().nextInt();
	}

	/**
	 * Finds out if a packet is forwardable. Its depth has to be lower than MAX_TUNNEL_DEPTH. There's a random
	 * bias to let some packets pass to avoid a successful search by depth attack.
	 *
	 * @param id    the tunnel id (search requests) or partial tunnel id (tunnel requests)
	 * @param depth the depth
	 * @return true if forwardable
	 */
	public boolean isForwardable(int id, int depth)
	{
		var randomBypass = depth >= MAX_TUNNEL_DEPTH && (((bias ^ id) & 0x7) == 2);

		return depth < MAX_TUNNEL_DEPTH || randomBypass;
	}

	public int incrementDepth(int id, int depth)
	{
		var randomDepthSkipShift = depth == 1 && (((bias ^ id) & 0x7) == 6);

		if (!randomDepthSkipShift)
		{
			depth++;
		}
		return depth;
	}
}
