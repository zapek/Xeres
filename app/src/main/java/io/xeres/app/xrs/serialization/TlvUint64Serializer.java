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

package io.xeres.app.xrs.serialization;

import io.netty.buffer.ByteBuf;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;

final class TlvUint64Serializer
{
	private TlvUint64Serializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, TlvType type, long value)
	{
		var len = getSize();
		buf.ensureWritable(len);
		buf.writeShort(type.getValue());
		buf.writeInt(len);
		buf.writeLong(value);
		return len;
	}

	static int getSize()
	{
		return TLV_HEADER_SIZE + 8;
	}

	static long deserialize(ByteBuf buf, TlvType type)
	{
		var readType = buf.readUnsignedShort();
		if (readType != type.getValue())
		{
			throw new IllegalArgumentException("Type " + readType + " does not match " + type);
		}
		var len = buf.readInt();
		if (len != getSize())
		{
			throw new IllegalArgumentException("Length is wrong: " + len + ", expected: " + getSize());
		}
		return buf.readLong();
	}
}
