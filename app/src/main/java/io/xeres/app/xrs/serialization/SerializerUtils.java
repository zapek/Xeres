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

package io.xeres.app.xrs.serialization;

import io.netty.buffer.ByteBuf;

final class SerializerUtils
{
	private static final int ENTRIES_MAX = 16_384;

	private SerializerUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int readLength(ByteBuf buf)
	{
		var len = buf.readInt();
		if (len < 0 || len > buf.readableBytes())
		{
			throw new IllegalArgumentException("Invalid length: " + len);
		}
		return len;
	}

	static int readEntriesCount(ByteBuf buf)
	{
		var count = buf.readInt();
		if (count < 0 || count > buf.readableBytes() || count > ENTRIES_MAX)
		{
			throw new IllegalArgumentException("Invalid entries: " + count);
		}
		return count;
	}

	static int getElementsCount(int len, int elementSize)
	{
		if (len % elementSize != 0)
		{
			throw new IllegalArgumentException("Invalid element length: " + len + " for element size: " + elementSize);
		}
		return len / elementSize;
	}
}
