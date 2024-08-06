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

import static io.xeres.app.xrs.service.turtle.item.TunnelDirection.SERVER;

public class TurtleChunkCrcRequestItem extends TurtleGenericTunnelItem
{
	@RsSerialized
	private int chunkNumber;

	@SuppressWarnings("unused")
	public TurtleChunkCrcRequestItem()
	{
		setDirection(SERVER);
	}

	public TurtleChunkCrcRequestItem(int chunkNumber)
	{
		super();
		this.chunkNumber = chunkNumber;
	}

	@Override
	public boolean shouldStampTunnel()
	{
		return false;
	}

	@Override
	public int getSubType()
	{
		return 21;
	}

	public int getChunkNumber()
	{
		return chunkNumber;
	}

	@Override
	public TurtleChunkCrcRequestItem clone()
	{
		return (TurtleChunkCrcRequestItem) super.clone();
	}
}
