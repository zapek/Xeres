/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

final class ByteSerializer
{
	private static final Logger log = LoggerFactory.getLogger(ByteSerializer.class);

	private ByteSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	@SuppressWarnings("SameReturnValue")
	static int serialize(ByteBuf buf, byte value)
	{
		log.trace("Writing byte: {}", value);
		buf.ensureWritable(Byte.BYTES);
		buf.writeByte(value);
		return Byte.BYTES;
	}

	static byte deserialize(ByteBuf buf)
	{
		var val = buf.readByte();
		log.trace("Reading byte: {}", val);
		return val;
	}
}
