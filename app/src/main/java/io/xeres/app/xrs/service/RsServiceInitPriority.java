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

package io.xeres.app.xrs.service;

/**
 * Priority for running service initializations. Except when OFF (default),
 * contains a time range with random triggering in between, to increase handshake
 * chances between peers.
 */
public enum RsServiceInitPriority
{
	OFF(0, 0),
	LOW(11, 20),
	NORMAL(6, 10),
	HIGH(3, 5),
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
