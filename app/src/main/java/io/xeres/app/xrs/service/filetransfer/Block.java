package io.xeres.app.xrs.service.filetransfer;

import java.util.Optional;

import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.BLOCK_SIZE;
import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.CHUNK_SIZE;

class Block
{
	private long hiBlocks;
	private long lowBlocks;

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

	public Optional<Long> getNextBlock()
	{
		for (long blockIndex = 0; blockIndex < lowBlocks; blockIndex++)
		{
			if ((lowBlocks & blockIndex) != 0)
			{
				return Optional.of(blockIndex);
			}
		}

		for (long blockIndex = 0; blockIndex < hiBlocks; blockIndex++)
		{
			if ((hiBlocks & blockIndex) != 0)
			{
				return Optional.of(blockIndex + 64);
			}
		}
		return Optional.empty();
	}

	public boolean isComplete()
	{
		return getNextBlock().isEmpty();
	}
}
