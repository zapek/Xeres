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

package io.xeres.app.net.util;

public enum NetworkMode
{
	PUBLIC, // DHT & Discovery
	PRIVATE, // Discovery only
	INVERTED, // DHT only
	DARKNET; // None

	public static boolean isDiscoverable(NetworkMode networkMode)
	{
		return switch (networkMode)
				{
					case PUBLIC, PRIVATE -> true;
					case INVERTED, DARKNET -> false;
				};
	}

	public static boolean hasDht(NetworkMode networkMode)
	{
		return switch (networkMode)
				{
					case PUBLIC, INVERTED -> true;
					case PRIVATE, DARKNET -> false;
				};
	}

	public static NetworkMode getNetworkMode(int vsDisc, int vsDht)
	{
		if (vsDisc == 2 && vsDht == 2)
		{
			return PUBLIC;
		}
		else if (vsDisc == 2)
		{
			return PRIVATE;
		}
		else if (vsDht == 2)
		{
			return INVERTED;
		}
		return DARKNET;
	}
}
