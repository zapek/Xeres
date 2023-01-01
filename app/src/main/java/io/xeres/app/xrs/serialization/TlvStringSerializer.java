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

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;

final class TlvStringSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvStringSerializer.class);

	private TlvStringSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, TlvType type, String s)
	{
		var len = getSize(s);

		var bytes = s != null ? s.getBytes() : new byte[0];

		log.trace("Writing string ({}): \"{}\"", type, s);
		buf.ensureWritable(len);
		buf.writeShort(type.getValue());
		buf.writeInt(len);
		if (bytes.length > 0)
		{
			buf.writeBytes(bytes);
		}
		return len;
	}

	static int getSize(String s)
	{
		return TLV_HEADER_SIZE + (s != null ? s.getBytes().length : 0);
	}

	static String deserialize(ByteBuf buf, TlvType type)
	{
		log.trace("Reading TLV string");
		var len = TlvUtils.checkTypeAndLength(buf, type);
		var out = new byte[len];
		buf.readBytes(out);
		return new String(out);
	}
}
