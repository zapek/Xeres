package io.xeres.app.xrs.service.filetransfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.BLOCK_SIZE;
import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.CHUNK_SIZE;

/**
 * Represents a chunk. Is made up of several blocks of data.
 */
class Chunk
{
	private static final Logger log = LoggerFactory.getLogger(Chunk.class);

	private long hiBlocks;
	private long lowBlocks;
	private final int totalBlocks;

	/**
	 * Creates a chunk.
	 *
	 * @param size is at most {@link FileTransferRsService#CHUNK_SIZE} but can be less if the end of the file is within the last chunk
	 */
	public Chunk(long size)
	{
		totalBlocks = (int) (size / BLOCK_SIZE + (size % BLOCK_SIZE != 0 ? 1 : 0));
	}

	/**
	 * Marks the block as written.
	 *
	 * @param offset the offset within the file
	 */
	public void setBlockAsWritten(long offset)
	{
		if (offset % BLOCK_SIZE != 0)
		{
			throw new IllegalArgumentException("Wrong block offset: " + offset);
		}

		var blockOffset = offset % CHUNK_SIZE;
		var blockIndex = blockOffset / BLOCK_SIZE;
		if (blockIndex < 64)
		{
			lowBlocks |= 1L << blockIndex;
		}
		else
		{
			hiBlocks |= 1L << blockIndex - 64;
		}
	}

	/**
	 * Checks if the chunk has all data written to it.
	 *
	 * @return true if complete
	 */
	public boolean isComplete()
	{
		int total = 0;

		for (int blockIndex = 0; blockIndex < 64; blockIndex++)
		{
			if ((lowBlocks & 1L << blockIndex) != 0)
			{
				total++;
			}
		}

		for (int blockIndex = 0; blockIndex < 64; blockIndex++)
		{
			if ((hiBlocks & 1L << blockIndex) != 0)
			{
				total++;
			}
		}
		return total == totalBlocks;
	}

	@Override
	public String toString()
	{
		return "Chunk{" +
				"hiBlocks=" + hiBlocks +
				", lowBlocks=" + lowBlocks +
				", totalBlocks=" + totalBlocks +
				'}';
	}
}
