/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.common.util;

/**
 * Various utility functions to use when debugging.
 */
public final class DebugUtils
{
	private DebugUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Waits for a certain time. Useful to simulate things like a network delay or a heavy computation.
	 *
	 * @param seconds the number of seconds to wait
	 */
	public static void wait(int seconds)
	{
		try
		{
			Thread.sleep(seconds * 1000L);
		}
		catch (InterruptedException _)
		{
			Thread.currentThread().interrupt();
		}
	}
}
