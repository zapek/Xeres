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

import java.util.BitSet;

/// Tracks the receiving state of a single chunk from a peer. Chunks are
/// requested and received as a sequence of slices of progressively growing
/// size, so this tracks the current chunk, where the next slice starts, the
/// size of the next slice to request and the bytes already received.
class ChunkReceiver
{
	private BitSet chunkMap;

	private int currentChunk;
	private long nextSliceOffset;
	private int requestSize;
	private long receivedChunkBytes;
	private boolean sliceInFlight;

	// Request-size ramp state (TCP-like slow start / congestion avoidance)
	private boolean slowStart = true;
	private long previousRate;

	public BitSet getChunkMap()
	{
		return chunkMap;
	}

	public void setChunkMap(BitSet chunkMap)
	{
		this.chunkMap = chunkMap;
	}

	public boolean hasChunkMap()
	{
		return chunkMap != null;
	}

	public int getCurrentChunk()
	{
		return currentChunk;
	}

	public void setCurrentChunk(int currentChunk)
	{
		this.currentChunk = currentChunk;
	}

	public long getNextSliceOffset()
	{
		return nextSliceOffset;
	}

	public void setNextSliceOffset(long nextSliceOffset)
	{
		this.nextSliceOffset = nextSliceOffset;
	}

	public int getRequestSize()
	{
		return requestSize;
	}

	public void setRequestSize(int requestSize)
	{
		this.requestSize = requestSize;
	}

	public long getReceivedChunkBytes()
	{
		return receivedChunkBytes;
	}

	public void addReceivedChunkBytes(long bytes)
	{
		receivedChunkBytes += bytes;
	}

	public void resetReceivedChunkBytes()
	{
		receivedChunkBytes = 0;
	}

	public boolean isSliceInFlight()
	{
		return sliceInFlight;
	}

	public void setSliceInFlight(boolean sliceInFlight)
	{
		this.sliceInFlight = sliceInFlight;
	}

	public boolean isSlowStart()
	{
		return slowStart;
	}

	public void setSlowStart(boolean slowStart)
	{
		this.slowStart = slowStart;
	}

	public long getPreviousRate()
	{
		return previousRate;
	}

	public void setPreviousRate(long previousRate)
	{
		this.previousRate = previousRate;
	}
}
