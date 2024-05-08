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

final class StringSerializer
{
	private static final Logger log = LoggerFactory.getLogger(StringSerializer.class);

	private StringSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, String value)
	{
		log.trace("Writing string: \"{}\"", value);
		if (value == null)
		{
			buf.ensureWritable(4);
			buf.writeInt(0);
			return 4;
		}
		var bytes = value.getBytes();
		buf.ensureWritable(4 + bytes.length);
		buf.writeInt(bytes.length);
		buf.writeBytes(bytes);
		return 4 + bytes.length;
	}

	static String deserialize(ByteBuf buf)
	{
		var len = buf.readInt();
		log.trace("Reading string of length: {}", len);
		var out = new byte[len];
		buf.readBytes(out);
		return new String(out);
	}
}
