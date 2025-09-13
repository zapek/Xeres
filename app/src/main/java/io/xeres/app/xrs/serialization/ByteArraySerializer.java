/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ByteArraySerializer
{
	private static final Logger log = LoggerFactory.getLogger(ByteArraySerializer.class);

	private ByteArraySerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, byte[] a)
	{
		if (a == null)
		{
			buf.ensureWritable(Integer.BYTES);
			buf.writeInt(0);
			return Integer.BYTES;
		}
		log.trace("Writing byte array of size {}", a.length);
		buf.ensureWritable(Integer.BYTES + a.length);
		buf.writeInt(a.length);
		buf.writeBytes(a);
		return Integer.BYTES + a.length;
	}

	static byte[] deserialize(ByteBuf buf)
	{
		var len = buf.readInt();
		log.trace("Reading byte array of size {}", len);
		var out = new byte[len];
		buf.readBytes(out);
		return out;
	}

	static int serialize(ByteBuf buf, byte[] array, int size)
	{
		log.trace("Writing byte array of specific size {}", size);
		buf.ensureWritable(size);
		buf.writeBytes(array, 0, size);
		return size;
	}

	static byte[] deserialize(ByteBuf buf, int size)
	{
		log.trace("Reading byte array of specific size {}", size);
		var out = new byte[size];
		buf.readBytes(out);
		return out;
	}
}
