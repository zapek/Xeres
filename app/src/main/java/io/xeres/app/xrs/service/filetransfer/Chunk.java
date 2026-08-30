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

import io.xeres.common.util.ByteUnitUtils;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

/// Represents a chunk. Is made up of several blocks of data.
class Chunk
{
	static final int CHUNK_SIZE = ByteUnitUtils.fromMegabytes(1);

	private record Slice(long offset, int size)
	{
	}

	private final NavigableSet<Slice> slices = new TreeSet<>(Comparator.comparingLong(Slice::offset));
	private final long baseOffset;
	private final int size;

	/**
	 * Gets the chunk number handling the file at a particular offset.
	 *
	 * @param offset the offset within the file
	 * @return the chunk number responsible for it
	 */
	public static int getChunkKey(long offset)
	{
		return (int) (offset / CHUNK_SIZE);
	}

	/// Creates a chunk.
	///
	/// @param offset the offset within the file from where the chunk will start
	/// @param fileSize the total file size
	public Chunk(long offset, long fileSize)
	{
		baseOffset = offset;
		size = (int) Math.min(CHUNK_SIZE, fileSize - offset);
	}

	/**
	 * Adds a slice.
	 *
	 * @param offset the offset within the file being transferred
	 * @param size   the size of the slice
	 */
	public void addCompletedSlice(long offset, int size)
	{
		// XXX: the range checks should be removed later or handled, right now they will kill the transfer process (handling them should be done before the write, though)
		if (offset < baseOffset)
		{
			throw new IllegalArgumentException("Slice offset is smaller than base offset");
		}
		if (offset >= baseOffset + this.size)
		{
			throw new IllegalArgumentException("Slice offset is bigger than base offset + size");
		}
		var slice = new Slice(offset, size);
		var lower = slices.floor(slice);
		if (lower != null)
		{
			if (lower.offset() + lower.size() > offset)
			{
				throw new IllegalStateException("Slice is overstepping the previous one");
			}
		}
		var higher = slices.ceiling(slice);
		if (higher != null)
		{
			if (offset + size > higher.offset())
			{
				throw new IllegalStateException("Slice is overstepping the next one");
			}
		}
		slices.add(slice);
	}

	/// Checks if the chunk has all data written to it.
	///
	/// @return true if complete
	public boolean isComplete()
	{
		return slices.stream()
				.mapToInt(Slice::size)
				.sum() == size;
	}

	@Override
	public String toString()
	{
		return "Chunk{" +
				"slices=" + slices.size() +
				", size=" + size +
				'}';
	}
}
