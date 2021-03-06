/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;

final class TlvSetSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvSetSerializer.class);

	private TlvSetSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serializeLong(ByteBuf buf, TlvType type, Set<Long> set)
	{
		var len = getSize(set);
		log.trace("Writing set of longs: {}", log.isTraceEnabled() ? Arrays.toString(set.toArray()) : "");
		buf.ensureWritable(len);
		buf.writeShort(type.getValue());
		buf.writeInt(len);
		set.forEach(buf::writeLong);
		return len;
	}

	static int getSize(Set<Long> set)
	{
		return TLV_HEADER_SIZE + 8 * set.size();
	}

	static Set<Long> deserializeLong(ByteBuf buf, TlvType type)
	{
		log.trace("Reading set of longs");
		var len = TlvUtils.checkTypeAndLength(buf, type);
		var count = len / 8;
		var set = new HashSet<Long>(count);

		while (count-- > 0)
		{
			set.add(buf.readLong());
		}
		return set;
	}
}
