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

package io.xeres.app.xrs.service.filetransfer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static io.xeres.app.xrs.service.filetransfer.FileTransferStrategy.LINEAR;

/**
 * Used to track which chunks are still remaining for a file to be complete.
 */
class ChunkDistributor
{
	private static final int MAX_RANDOM_TRY = 10;

	/**
	 * Time to consider a given chunk as "lost".
	 * XXX: add a way to update that value when a write for that chunk is received. if possible, make the timeout shorter then
	 */
	private static final Duration GIVEN_CHUNK_TIMEOUT = Duration.ofMinutes(10);

	private final BitSet chunkMap; // This is updated externally
	private final Map<Integer, Instant> givenChunks = new HashMap<>();
	private final int totalChunks;
	private final FileTransferStrategy fileTransferStrategy;
	private int minChunk;
	private int maxChunk;

	public ChunkDistributor(BitSet chunkMap, int totalChunks, FileTransferStrategy fileTransferStrategy)
	{
		Objects.requireNonNull(chunkMap);
		if (totalChunks < 1)
		{
			throw new IllegalArgumentException("totalChunks must be greater than 0");
		}
		this.chunkMap = chunkMap;
		this.totalChunks = totalChunks;
		this.fileTransferStrategy = fileTransferStrategy;
	}

	private void updateChunksInfo()
	{
		minChunk = chunkMap.nextClearBit(Math.max(minChunk, 0));
		maxChunk = chunkMap.previousClearBit(totalChunks - 1);

		// The given chunks that were downloaded should be
		// removed to consolidate the set.
		var beforeSize = givenChunks.size();
		givenChunks.entrySet().removeIf(entry -> chunkMap.get(entry.getKey()) || givenChunkIsTooOld(entry.getValue()));
		if (fileTransferStrategy == LINEAR && beforeSize != givenChunks.size())
		{
			minChunk = findMinChunk();
		}
	}

	private boolean givenChunkIsTooOld(Instant given)
	{
		return given.isBefore(Instant.now().minus(GIVEN_CHUNK_TIMEOUT));
	}

	private int findMinChunk()
	{
		minChunk = chunkMap.nextClearBit(0);
		while (givenChunks.containsKey(minChunk) || chunkMap.get(minChunk))
		{
			minChunk++;
		}
		if (minChunk > maxChunk)
		{
			minChunk = -1;
		}
		return minChunk;
	}

	/**
	 * Gets a next available chunk to fill in.
	 *
	 * @return an empty chunk which needs to be filled in
	 */
	public Optional<Integer> getNextChunk(BitSet availableChunks)
	{
		updateChunksInfo();

		// When maxChunk is -1, there's no free chunk left.
		// minChunk has a wrong value in that case because BitSet has no
		// concept of maximum bits, so it will always find a "free" bit.
		if (maxChunk == -1 || minChunk == -1 || chunkMap.cardinality() + givenChunks.size() == totalChunks)
		{
			return Optional.empty();
		}

		var chunk = fileTransferStrategy == LINEAR ? getLinearChunk() : getRandomChunk();
		if (!availableChunks.get(chunk))
		{
			return Optional.empty();
		}
		givenChunks.put(chunk, Instant.now());
		return Optional.of(chunk);
	}

	private int getLinearChunk()
	{
		if (givenChunks.containsKey(minChunk) || chunkMap.get(minChunk))
		{
			minChunk++;
		}
		return minChunk;
	}

	private int getRandomChunk()
	{
		int chunk;
		var attempt = 0;

		do
		{
			chunk = ThreadLocalRandom.current().nextInt(minChunk, maxChunk + 1);
		}
		while (givenChunks.containsKey(chunk) && attempt++ < MAX_RANDOM_TRY);

		if (givenChunks.containsKey(chunk))
		{
			for (int i = minChunk; i < maxChunk; i++)
			{
				if (!givenChunks.containsKey(i))
				{
					return i;
				}
			}
			throw new IllegalStateException("Couldn't return random chunk. Shouldn't happen");
		}
		return chunk;
	}
}
