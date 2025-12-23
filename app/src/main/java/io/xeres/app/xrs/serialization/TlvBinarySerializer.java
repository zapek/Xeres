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

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;

final class TlvBinarySerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvBinarySerializer.class);

	private TlvBinarySerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, byte[] data)
	{
		return serialize(buf, TlvType.STR_NONE, data);
	}

	static int serialize(ByteBuf buf, TlvType type, byte[] data)
	{
		return serialize(buf, type.getValue(), data);
	}

	static int serialize(ByteBuf buf, int type, byte[] data)
	{
		if (data == null)
		{
			data = new byte[0];
		}

		var len = getSize(data);
		log.trace("Writing TLV binary data (size: {})", data.length);
		buf.ensureWritable(len);
		buf.writeShort(type);
		buf.writeInt(len);
		if (data.length > 0)
		{
			buf.writeBytes(data);
		}
		return len;
	}

	static int getSize(byte[] data)
	{
		return TLV_HEADER_SIZE + (data != null ? data.length : 0);
	}

	static byte[] deserialize(ByteBuf buf)
	{
		return deserialize(buf, TlvType.STR_NONE);
	}

	static byte[] deserialize(ByteBuf buf, TlvType type)
	{
		return deserialize(buf, type.getValue());
	}

	static byte[] deserialize(ByteBuf buf, int type)
	{
		log.trace("Reading TLV binary");
		var len = TlvUtils.checkTypeAndLength(buf, type);
		log.trace("  of {} bytes", len);
		var out = new byte[len];
		buf.readBytes(out);
		return out;
	}
}
