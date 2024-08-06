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

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkMapUtilsTest
{
	@Test
	void ChunkMapUtils_toCompressedChunkMap()
	{
		var input = new BitSet(4);
		input.set(0);
		input.set(1);
		input.set(31);
		input.set(32);
		input.set(33);
		input.set(64);

		var output = ChunkMapUtils.toCompressedChunkMap(input);

		assertEquals(-2147483645, output.getFirst());
		assertEquals(3, output.get(1));
		assertEquals(1, output.get(2));
	}

	@Test
	void ChunkMapUtils_Transform()
	{
		List<Integer> input = List.of(0x1, 0xaabbccdd, 0x8844aa23);

		var bitSet = ChunkMapUtils.toBitSet(input);
		var output = ChunkMapUtils.toCompressedChunkMap(bitSet);

		assertArrayEquals(input.toArray(), output.toArray());
	}
}