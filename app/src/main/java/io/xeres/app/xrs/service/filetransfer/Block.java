package io.xeres.app.xrs.service.filetransfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.BLOCK_SIZE;
import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.CHUNK_SIZE;

class Block
{
	private static final Logger log = LoggerFactory.getLogger(Block.class);

	private long hiBlocks;
	private long lowBlocks;
	private final int totalBlocks;

	public Block(long size)
	{
		// size is at most CHUNK_SIZE but could be less if the end of the file is within the last chunk
		totalBlocks = (int) (size / BLOCK_SIZE + (size % BLOCK_SIZE != 0 ? 1 : 0));
		log.debug("totalBlocks: {}", totalBlocks);
	}

	public void setBlock(long offset)
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
		return "Block{" +
				"hiBlocks=" + hiBlocks +
				", lowBlocks=" + lowBlocks +
				", totalBlocks=" + totalBlocks +
				'}';
	}
}
