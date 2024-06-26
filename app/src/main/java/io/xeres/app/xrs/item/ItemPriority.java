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

package io.xeres.app.xrs.item;

public enum ItemPriority
{
	/**
	 * Anything that happens in the background and is not really urgent (for example discovery exchanges, file transfers, ...)
	 */
	BACKGROUND(2),

	/**
	 * The default priority.
	 */
	DEFAULT(3),

	NORMAL(5),

	/**
	 * High priority. Has consequences for other services and should be serviced quickly (for example GxS exchanges).
	 */
	HIGH(6),

	/**
	 * Generated by a  user and requires immediate feedback (for example chat, typing feedback, ...)
	 */
	INTERACTIVE(7),

	/**
	 * Must be acknowledged by the other peer quickly, or it will have disruptive effects (for example, heartbeats).
	 */
	IMPORTANT(8),

	/**
	 * Must be carried away immediately or it won't be usable (for example RTT measurements).
	 */
	REALTIME(9);

	private final int priority;

	ItemPriority(int priority)
	{
		this.priority = priority;
	}

	public int getPriority()
	{
		return priority;
	}
}

