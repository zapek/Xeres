/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.turtle.item;

import io.xeres.app.xrs.serialization.RsSerialized;

/**
 * Used by any service to pass on arbitrary data into a tunnel.
 */
public class TurtleGenericDataItem extends TurtleGenericTunnelItem
{
	/**
	 * The data.
	 */
	@RsSerialized
	private byte[] tunnelData;

	public TurtleGenericDataItem()
	{
		// Required
	}

	public TurtleGenericDataItem(byte[] data)
	{
		tunnelData = data;
	}

	@Override
	public int getSubType()
	{
		return 10;
	}

	@Override
	public boolean shouldStampTunnel()
	{
		return true;
	}

	public byte[] getTunnelData()
	{
		return tunnelData;
	}

	@Override
	public String toString()
	{
		return "TurtleGenericDataItem{" +
				"tunnelData.length=" + (tunnelData == null ? "[null]" : tunnelData.length) +
				'}';
	}

	@Override
	public TurtleGenericDataItem clone()
	{
		return (TurtleGenericDataItem) super.clone();
	}
}
