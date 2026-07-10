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

/**
 * A TLV map used in ServiceInfo. Much more annoying
 * to use than the normal MapSerializer.
 */
final class TlvMapSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvMapSerializer.class);

	private TlvMapSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, TlvType mapType, TlvType pairType, TlvType keyType, TlvType valueType, Map<?, ?> map)
	{
		var size = 0;

		if (map != null && !map.isEmpty())
		{
			log.trace("Entries in Map: {}", map.size());
			var mapSize = 0;
			var mapSizeOffset = TlvUtils.prepareWriteTlvSize(buf, mapType);
			for (var entry : map.entrySet())
			{
				var entrySizeOffset = TlvUtils.prepareWriteTlvSize(buf, pairType);
				var entrySize = 0;
				log.trace("Writing key class: {}, value class: {}", entry.getKey().getClass().getSimpleName(), entry.getValue().getClass().getSimpleName());
				entrySize += writeMapData(buf, keyType, entry.getKey());
				entrySize += writeMapData(buf, valueType, entry.getValue());
				mapSize += TlvUtils.actuallyWriteTlvSize(buf, entrySizeOffset, entrySize);
			}
			log.trace("Writing total map size of {}", mapSize);
			size += TlvUtils.actuallyWriteTlvSize(buf, mapSizeOffset, mapSize);
		}
		else
		{
			var mapSizeOffset = TlvUtils.prepareWriteTlvSize(buf, mapType);
			size += TlvUtils.actuallyWriteTlvSize(buf, mapSizeOffset, 0);
		}
		return size;
	}

	static <K, V> Map<K, V> deserialize(ByteBuf buf, TlvType mapType, TlvType pairType, TlvType keyType, TlvType valueType, Map<K, V> map, ParameterizedType type)
	{
		if (map == null)
		{
			map = new HashMap<>();
		}

		var mapSize = TlvUtils.readTlvSize(buf, mapType);
		log.trace("Map size: {}, readerIndex: {}", mapSize, buf.readerIndex());
		var mapIndex = buf.readerIndex();

		while (buf.readerIndex() < mapIndex + mapSize)
		{
			log.trace("buf.readerIndex: {}, mapIndex + mapSize: {}", buf.readerIndex(), mapIndex + mapSize);
			TlvUtils.readTlvSize(buf, pairType);

			@SuppressWarnings("unchecked") var keyClass = (Class<K>) type.getActualTypeArguments()[0];
			@SuppressWarnings("unchecked") var dataClass = (Class<V>) type.getActualTypeArguments()[1];
			log.trace("Key class: {}, Value class: {}", keyClass.getSimpleName(), dataClass.getSimpleName());
			var keyObject = readMapData(buf, keyType, keyClass);
			var dataObject = readMapData(buf, valueType, dataClass);
			map.put(keyObject, dataObject);
		}
		log.trace("done: buf.readerIndex: {}", buf.readerIndex());
		return map;
	}

	private static int writeMapData(ByteBuf buf, TlvType tlvType, Object object)
	{
		var offset = TlvUtils.prepareWriteTlvSize(buf, tlvType);
		int size = Serializer.serialize(buf, object.getClass(), object, null);
		return TlvUtils.actuallyWriteTlvSize(buf, offset, size);
	}

	private static <T> T readMapData(ByteBuf buf, TlvType tlvType, Class<T> javaClass)
	{
		TlvUtils.readTlvSize(buf, tlvType);
		return Serializer.deserialize(buf, javaClass);
	}
}
