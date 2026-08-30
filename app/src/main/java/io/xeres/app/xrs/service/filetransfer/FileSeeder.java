/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

	public boolean hasChunkMap()
	{
		return chunkReceiver.hasChunkMap();
	}

	public BitSet getChunkMap()
	{
		return chunkReceiver.getChunkMap();
	}

	public int getCurrentChunk()
	{
		return chunkReceiver.getCurrentChunk();
	}

	public void setCurrentChunk(int currentChunk)
	{
		chunkReceiver.setCurrentChunk(currentChunk);
	}

	public long getNextSliceOffset()
	{
		return chunkReceiver.getNextSliceOffset();
	}

	public void setNextSliceOffset(long nextSliceOffset)
	{
		chunkReceiver.setNextSliceOffset(nextSliceOffset);
	}

	public int getRequestSize()
	{
		return chunkReceiver.getRequestSize();
	}

	public void setRequestSize(int requestSize)
	{
		chunkReceiver.setRequestSize(requestSize);
	}

	public long getReceivedChunkBytes()
	{
		return chunkReceiver.getReceivedChunkBytes();
	}

	public void addReceivedChunkBytes(long bytes)
	{
		chunkReceiver.addReceivedChunkBytes(bytes);
	}

	public void resetReceivedChunkBytes()
	{
		chunkReceiver.resetReceivedChunkBytes();
	}

	public boolean isSliceInFlight()
	{
		return chunkReceiver.isSliceInFlight();
	}

	public void setSliceInFlight(boolean sliceInFlight)
	{
		chunkReceiver.setSliceInFlight(sliceInFlight);
	}

	public boolean isSlowStart()
	{
		return chunkReceiver.isSlowStart();
	}

	public void setSlowStart(boolean slowStart)
	{
		chunkReceiver.setSlowStart(slowStart);
	}

	public long getPreviousRate()
	{
		return chunkReceiver.getPreviousRate();
	}

	public void setPreviousRate(long previousRate)
	{
		chunkReceiver.setPreviousRate(previousRate);
	}
}
