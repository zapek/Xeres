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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

final class InstantSerializer
{
	private static final Logger log = LoggerFactory.getLogger(InstantSerializer.class);

	private InstantSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, Instant value)
	{
		log.trace("Writing instant: {}", value);
		buf.ensureWritable(Integer.BYTES);
		buf.writeInt((int) (value != null ? value.getEpochSecond() : 0));
		return Integer.BYTES;
	}

	static Instant deserialize(ByteBuf buf)
	{
		var val = buf.readUnsignedInt();
		var instant = Instant.ofEpochSecond(val);
		log.trace("Reading instant: {}", instant);
		return instant;
	}
}
