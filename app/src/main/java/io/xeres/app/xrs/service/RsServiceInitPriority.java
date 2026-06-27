/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service;

/**
 * Priority for running service initializations against a peer connection. Except when OFF (default),
 * contains a time range with random triggering in between, to decrease simultaneous handshake
 * chances between peers.
 */
public enum RsServiceInitPriority
{
	/**
	 * The initialization method is never called (default).
	 */
	OFF(0, 0),

	/**
	 * The initialization method is called late (between 11 and 20 seconds).
	 */
	LOW(11, 20),

	/**
	 * The initialization method is called normally (between 6 and 10 seconds).
	 */
	NORMAL(6, 10),

	/**
	 * The initialization method is called quickly (between 3 and 5 seconds).
	 */
	HIGH(3, 5),

	/**
	 * The initialization method is called immediately (1 to 2 seconds).
	 */
	IMMEDIATE(1, 2);

	private final int minTime;
	private final int maxTime;

	RsServiceInitPriority(int minTime, int maxTime)
	{
		this.minTime = minTime;
		this.maxTime = maxTime;
	}

	public int getMinTime()
	{
		return minTime;
	}

	public int getMaxTime()
	{
		return maxTime;
	}
}
