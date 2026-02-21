/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.client.preview;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A DataBuffer collector that can avoid fetching the whole HTML file.
 */
public class SizeLimitingCollector
{
	private final List<byte[]> chunks = new ArrayList<>();
	private long totalBytes;
	private final long maxBytes;
	private boolean limitReached;

	public SizeLimitingCollector(long maxBytes)
	{
		this.maxBytes = maxBytes;
	}

	public void add(DataBuffer buffer)
	{
		if (limitReached)
		{
			DataBufferUtils.release(buffer);
			return;
		}

		int readableBytes = buffer.readableByteCount();

		if (totalBytes + readableBytes <= maxBytes)
		{
			// We can take the whole buffer
			var bytes = new byte[readableBytes];
			buffer.read(bytes);
			chunks.add(bytes);
			totalBytes += readableBytes;
			DataBufferUtils.release(buffer);
		}
		else
		{
			// Partial buffer
			int bytesToTake = (int) (maxBytes - totalBytes);
			if (bytesToTake > 0)
			{
				var bytes = new byte[bytesToTake];
				buffer.read(bytes);
				chunks.add(bytes);
				totalBytes += bytesToTake;
			}
			DataBufferUtils.release(buffer);
			limitReached = true;
		}
	}

	public byte[] getResult()
	{
		// Combine all chunks into one byte array
		var result = new byte[(int) totalBytes];
		var offset = 0;
		for (byte[] chunk : chunks)
		{
			System.arraycopy(chunk, 0, result, offset, chunk.length);
			offset += chunk.length;
		}
		return result;
	}
}
