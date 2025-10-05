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

import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.BLOCK_SIZE;
import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.CHUNK_SIZE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkTest
{
	@Test
	void fillFullChunk()
	{
		var chunk = new Chunk(CHUNK_SIZE);
		for (int i = 0; i < CHUNK_SIZE - BLOCK_SIZE; i += BLOCK_SIZE)
		{
			chunk.setBlockAsWritten(i);
			assertFalse(chunk.isComplete());
		}
		chunk.setBlockAsWritten(CHUNK_SIZE - BLOCK_SIZE);
		assertTrue(chunk.isComplete());
	}

	@Test
	void fillPartialChunk()
	{
		var chunk = new Chunk(CHUNK_SIZE - 5000);
		for (int i = 0; i < CHUNK_SIZE - BLOCK_SIZE; i += BLOCK_SIZE)
		{
			chunk.setBlockAsWritten(i);
			assertFalse(chunk.isComplete());
		}
		chunk.setBlockAsWritten(CHUNK_SIZE - BLOCK_SIZE);
		assertTrue(chunk.isComplete());
	}
}