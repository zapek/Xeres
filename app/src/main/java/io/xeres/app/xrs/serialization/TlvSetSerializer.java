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
import io.xeres.common.id.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
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
		var len = getLongSize(set);
		log.trace("Writing set of longs: {}", log.isTraceEnabled() ? Arrays.toString(set.toArray()) : "");
		buf.ensureWritable(len);
		buf.writeShort(type.getValue());
		buf.writeInt(len);
		set.stream()
				.sorted()
				.forEach(buf::writeLong);
		return len;
	}

	static int serializeIdentifier(ByteBuf buf, TlvType type, Set<? extends Identifier> set)
	{
		var len = getIdentifierSize(set);
		log.trace("Writing set of identifiers: {}", log.isTraceEnabled() ? Arrays.toString(set.toArray()) : "");
		buf.ensureWritable(len);
		buf.writeShort(type.getValue());
		buf.writeInt(len);
		set.stream()
				.sorted(Comparator.comparing(identifier -> new BigInteger(1, identifier.getBytes())))
				.forEach(identifier -> buf.writeBytes(identifier.getBytes()));
		return len;
	}

	private static int getLongSize(Set<Long> set)
	{
		return TLV_HEADER_SIZE + Long.BYTES * set.size();
	}

	static int getIdentifierSize(Set<? extends Identifier> set)
	{
		if (set.isEmpty())
		{
			return TLV_HEADER_SIZE;
		}
		return TLV_HEADER_SIZE + set.stream().findFirst().orElseThrow().getLength() * set.size();
	}

	static Set<Long> deserializeLong(ByteBuf buf, TlvType type)
	{
		log.trace("Reading set of longs");
		var len = TlvUtils.checkTypeAndLength(buf, type);
		var count = len / Long.BYTES;
		HashSet<Long> set = HashSet.newHashSet(count);

		while (count-- > 0)
		{
			set.add(buf.readLong());
		}
		return set;
	}

	static Set<? extends Identifier> deserializeIdentifier(ByteBuf buf, TlvType type, Class<?> identifierClass)
	{
		log.trace("Reading set of identifiers");
		var len = TlvUtils.checkTypeAndLength(buf, type);
		var count = len / IdentifierSerializer.getIdentifierLength(identifierClass);
		HashSet<Identifier> set = HashSet.newHashSet(count);

		while (count-- > 0)
		{
			set.add(IdentifierSerializer.deserialize(buf, identifierClass));
		}
		return set;
	}
}
