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

package io.xeres.app.xrs.service.filetransfer.item;

import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.turtle.item.TurtleGenericTunnelItem;

import java.util.Arrays;

import static io.xeres.app.xrs.service.turtle.item.TunnelDirection.CLIENT;

public class TurtleFileDataItem extends TurtleGenericTunnelItem
{
	@RsSerialized
	private long chunkOffset;

	@RsSerialized
	private byte[] chunkData;

	public TurtleFileDataItem()
	{
		setDirection(CLIENT);
	}

	public TurtleFileDataItem(long chunkOffset, byte[] chunkData)
	{
		this();
		this.chunkOffset = chunkOffset;
		this.chunkData = chunkData;
	}

	@Override
	public boolean shouldStampTunnel()
	{
		return true;
	}

	@Override
	public int getSubType()
	{
		return 8;
	}

	public long getChunkOffset()
	{
		return chunkOffset;
	}

	public byte[] getChunkData()
	{
		return chunkData;
	}

	@Override
	public TurtleFileDataItem clone()
	{
		return (TurtleFileDataItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "TurtleFileDataItem{" +
				"chunkOffset=" + chunkOffset +
				", chunkData=" + Arrays.toString(chunkData) +
				'}';
	}
}
