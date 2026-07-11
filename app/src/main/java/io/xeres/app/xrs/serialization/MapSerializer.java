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

	static int serialize(ByteBuf buf, Map<?, ?> map)
	{
		var size = Integer.BYTES;

		if (map != null && !map.isEmpty())
		{
			var mapSize = map.size();
			log.trace("Entries in Map: {}", mapSize);
			buf.writeInt(mapSize);
			for (var entry : map.entrySet())
			{
				log.trace("Writing key class: {}, value class: {}", entry.getKey().getClass().getSimpleName(), entry.getValue().getClass().getSimpleName());
				size += writeMapData(buf, entry.getKey());
				size += writeMapData(buf, entry.getValue());
			}
		}
		else
		{
			buf.writeInt(0);
		}
		return size;
	}

	static <K, V> Map<K, V> deserialize(ByteBuf buf, Map<K, V> map, ParameterizedType type)
	{
		if (map == null)
		{
			map = new HashMap<>();
		}

		var entries = buf.readInt();
		log.trace("Map entries: {}", entries);

		while (entries-- > 0)
		{
			@SuppressWarnings("unchecked") var keyClass = (Class<K>) type.getActualTypeArguments()[0];
			@SuppressWarnings("unchecked") var dataClass = (Class<V>) type.getActualTypeArguments()[1];
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
}
