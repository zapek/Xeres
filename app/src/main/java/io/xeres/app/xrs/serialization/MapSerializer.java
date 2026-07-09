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

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

final class MapSerializer
{
	private static final Logger log = LoggerFactory.getLogger(MapSerializer.class);

	private MapSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, Map<Object, Object> map)
	{
		var size = 0;

		if (map != null && !map.isEmpty())
		{
			log.trace("Entries in Map: {}", map.size());
			var mapSize = 0;
			var mapSizeOffset = prepareWriteSize(buf);
			for (var entry : map.entrySet())
			{
				log.trace("Writing key class: {}, value class: {}", entry.getKey().getClass().getSimpleName(), entry.getValue().getClass().getSimpleName());
				size += writeMapData(buf, entry.getKey());
				size += writeMapData(buf, entry.getValue());
			}
			log.trace("Writing total map size of {}", mapSize);
			size += actuallyWriteSize(buf, mapSizeOffset, mapSize);
		}
		else
		{
			size += actuallyWriteSize(buf, prepareWriteSize(buf), 0);
		}
		return size;
	}

	static Map<Object, Object> deserialize(ByteBuf buf, Map<Object, Object> map, ParameterizedType type)
	{
		if (map == null)
		{
			map = new HashMap<>();
		}

		var entries = readEntries(buf);
		log.trace("Map entries: {}", entries);

		while (entries-- > 0)
		{
			var keyClass = (Class<?>) type.getActualTypeArguments()[0];
			var dataClass = (Class<?>) type.getActualTypeArguments()[1];
			log.trace("Key class: {}, data class: {}", keyClass.getSimpleName(), dataClass.getSimpleName());
			var keyObject = Serializer.deserialize(buf, keyClass);
			var dataObject = Serializer.deserialize(buf, dataClass);
			map.put(keyObject, dataObject);
		}
		return map;
	}

	private static int writeMapData(ByteBuf buf, Object object)
	{
		return Serializer.serialize(buf, object.getClass(), object, null);
	}

	private static int prepareWriteSize(ByteBuf buf)
	{
		buf.ensureWritable(4);
		var offset = buf.writerIndex();
		buf.writerIndex(offset + 4);
		return offset;
	}

	private static int actuallyWriteSize(ByteBuf buf, int offset, int size)
	{
		size += 4;
		buf.setInt(offset, size);
		return size;
	}

	private static int readEntries(ByteBuf buf)
	{
		return Math.toIntExact(buf.readUnsignedInt());
	}
}
