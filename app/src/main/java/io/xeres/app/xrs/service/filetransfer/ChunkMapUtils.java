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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

final class ChunkMapUtils
{
	private ChunkMapUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Converts the chunkMap to the format used by RS. Note that there might
	 * be spurious unset chunks at the end. This is normal and RS also does that
	 * because the file size is taken into account when searching chunks.
	 *
	 * @param chunkMap the chunk map
	 * @return a compressed chunk map
	 */
	static List<Integer> toCompressedChunkMap(BitSet chunkMap)
	{
		var intBuf = ByteBuffer.wrap(alignArray(chunkMap.toByteArray()))
				.order(ByteOrder.LITTLE_ENDIAN)
				.asIntBuffer();
		var ints = new int[intBuf.remaining()];
		intBuf.get(ints);
		return Arrays.stream(ints).boxed().toList();
	}

	static BitSet toBitSet(List<Integer> chunkMap)
	{
		var bitSet = new BitSet(chunkMap.size() * 32);
		for (var i = 0; i < chunkMap.size(); i++)
		{
			var value = chunkMap.get(i);

			for (var j = 0; j < 32; j++)
			{
				bitSet.set(i * 32 + j, (value & (1 << j)) != 0);
			}
		}
		return bitSet;
	}

	/**
	 * Aligns the array to an integer (32-bits) boundary.
	 *
	 * @param src the source array
	 * @return the array aligned to an integer boundary
	 */
	private static byte[] alignArray(byte[] src)
	{
		if (src.length % 4 != 0)
		{
			var dst = new byte[src.length + (4 - src.length % 4)];
			System.arraycopy(src, 0, dst, 0, src.length);
			return dst;
		}
		return src;
	}
}
