/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.filetransfer;

import io.xeres.app.database.model.location.Location;

import java.util.BitSet;

public class FileSeeder extends FilePeer
{
	private final ChunkReceiver chunkReceiver = new ChunkReceiver();

	FileSeeder(Location location)
	{
		super(location);
	}

	public void updateChunkMap(BitSet chunkMap)
	{
		chunkReceiver.setChunkMap(chunkMap);
	}

	public void setReceiving(boolean receiving)
	{
		chunkReceiver.setReceiving(receiving);
	}

	public boolean isReceiving()
	{
		return chunkReceiver.isReceiving();
	}

	public int getChunkNumber()
	{
		return chunkReceiver.getChunkNumber();
	}

	public boolean hasChunkMap()
	{
		return chunkReceiver.hasChunkMap();
	}

	public BitSet getChunkMap()
	{
		return chunkReceiver.getChunkMap();
	}

	public void setChunkNumber(int chunkNumber)
	{
		chunkReceiver.setChunkNumber(chunkNumber);
	}
}
