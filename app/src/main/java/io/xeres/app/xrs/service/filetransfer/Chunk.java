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

import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.BLOCK_SIZE;
import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.CHUNK_SIZE;

/**
 * Represents a chunk. Is made up of several blocks of data.
 */
class Chunk
{
	// hiBlocks and lowBlocks aren't necessary, but they could be used to re-ask only for the missing block instead of the whole chunk
	private long hiBlocks;
	private long lowBlocks;
	private final int totalBlocks;
	private int remainingBlocks;

	/**
	 * Creates a chunk.
	 *
	 * @param size is at most {@link FileTransferRsService#CHUNK_SIZE} but can be less if the end of the file is within the last chunk
	 */
	public Chunk(long size)
	{
		if (size > CHUNK_SIZE)
		{
			throw new IllegalArgumentException("Chunk size is greater than " + CHUNK_SIZE);
		}
		totalBlocks = (int) (size / BLOCK_SIZE + (size % BLOCK_SIZE != 0 ? 1 : 0));
		remainingBlocks = totalBlocks;
	}

	/**
	 * Marks the block as written.
	 *
	 * @param offset the offset within the file
	 * @param size the total written size
	 */
	public void setBlocksAsWritten(long offset, int size)
	{
		if (offset % BLOCK_SIZE != 0)
		{
			throw new IllegalArgumentException("Wrong block offset: " + offset);
		}

		while (size > 0)
		{
			var blockOffset = offset % CHUNK_SIZE;
			var blockIndex = blockOffset / BLOCK_SIZE;
			if (blockIndex < 64)
			{
				if ((lowBlocks & 1L << blockIndex) > 0)
				{
					return; // Already set
				}
				lowBlocks |= 1L << blockIndex;
			}
			else
			{
				if ((hiBlocks & 1L << blockIndex - 64) > 0)
				{
					return; // Already set
				}
				hiBlocks |= 1L << blockIndex - 64;
			}
			remainingBlocks--;
			size -= BLOCK_SIZE;
			offset += BLOCK_SIZE;
		}
	}

	/**
	 * Checks if the chunk has all data written to it.
	 *
	 * @return true if complete
	 */
	public boolean isComplete()
	{
		return remainingBlocks == 0;
	}

	@Override
	public String toString()
	{
		return "Chunk{" +
				"hiBlocks=" + hiBlocks +
				", lowBlocks=" + lowBlocks +
				", totalBlocks=" + totalBlocks +
				", remainingBlocks=" + remainingBlocks +
				'}';
	}
}
