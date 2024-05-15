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

package io.xeres.app.xrs.service.filetransfer.item;

import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.turtle.item.TurtleGenericTunnelItem;

import static io.xeres.app.xrs.service.turtle.item.TunnelDirection.CLIENT;

public class TurtleFileDataItem extends TurtleGenericTunnelItem implements Cloneable
{
	@RsSerialized
	private long chunkOffset;

	@RsSerialized
	private int chunkSize;

	@RsSerialized
	private byte[] chunkData;

	public TurtleFileDataItem()
	{
		setDirection(CLIENT);
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

	@Override
	public TurtleFileDataItem clone()
	{
		var clone = (TurtleFileDataItem) super.clone();
		clone.chunkData = chunkData.clone();
		return clone;
	}
}