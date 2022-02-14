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

import java.util.ArrayList;
import java.util.List;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;

final class TlvStringSetRefSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvStringSetRefSerializer.class);

	private TlvStringSetRefSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	// XXX: warning! serialization has not been tested
	static int serialize(ByteBuf buf, TlvType type, List<String> refIds)
	{
		var len = getSize(refIds);
		log.trace("Writing refids: {}", log.isTraceEnabled() ? refIds : "");
		buf.ensureWritable(len);
		buf.writeShort(type.getValue());
		buf.writeInt(len);
		refIds.forEach(s -> TlvSerializer.serialize(buf, TlvType.STR_GENID, s));
		return len;
	}

	static int getSize(List<String> refIds)
	{
		return TLV_HEADER_SIZE + (int) refIds.stream().map(s -> TLV_HEADER_SIZE + s.length()).count();
	}

	static List<String> deserialize(ByteBuf buf, TlvType type)
	{
		log.trace("Reading refids");
		var len = TlvUtils.checkTypeAndLength(buf, type);
		var listIndex = buf.readerIndex();
		List<String> refIds = new ArrayList<>();
		while (buf.readerIndex() < listIndex + len)
		{
			refIds.add((String) Serializer.deserialize(buf, TlvType.STR_GENID));
		}
		return refIds;
	}
}
