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

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;

/**
 * A TLV map used in certain places. Much more annoying
 * to use than a normal MapSerializer.
 */
final class TlvMapSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvMapSerializer.class);

	private TlvMapSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, TlvType tlvType, Map<Object, Object> map)
	{
		var size = 0;

		if (map != null && !map.isEmpty())
		{
			log.trace("Entries in Map: {}", map.size());
			var mapSize = 0;
			var mapSizeOffset = prepareWriteTlv(buf, tlvType);
			for (var entry : map.entrySet())
			{
				var entrySizeOffset = prepareWriteTlv(buf, tlvType);
				var entrySize = 0;
				log.trace("Writing Key class: {}", entry.getKey().getClass().getSimpleName());
				entrySize += writeMapData(buf, tlvType, entry.getKey());
				log.trace("Writing Value class: {}", entry.getValue().getClass().getSimpleName());
				entrySize += writeMapData(buf, tlvType, entry.getValue());
				mapSize += actuallyWriteTlv(buf, entrySizeOffset, entrySize);
				log.trace("Writing total entry size of {}", entrySize);
			}
			log.trace("Writing total map size of {}", mapSize);
			size += actuallyWriteTlv(buf, mapSizeOffset, mapSize);
		}
		else
		{
			size += actuallyWriteTlv(buf, prepareWriteTlv(buf, tlvType), 0);
		}
		return size;
	}

	static Map<Object, Object> deserialize(ByteBuf buf, TlvType tlvType, Map<Object, Object> map, ParameterizedType type)
	{
		if (map == null)
		{
			map = new HashMap<>();
		}

		var mapSize = readTlv(buf, tlvType);
		log.trace("Map size: {}, readerIndex: {}", mapSize, buf.readerIndex());
		var mapIndex = buf.readerIndex();

		while (buf.readerIndex() < mapIndex + mapSize - TLV_HEADER_SIZE)
		{
			log.trace("buf.readerIndex: {}, mapIndex + mapSize: {}", buf.readerIndex(), mapIndex + mapSize);
			readTlv(buf, tlvType);

			var keyClass = (Class<?>) type.getActualTypeArguments()[0];
			log.trace("Key class: {}", keyClass.getSimpleName());
			var keyObject = readMapData(buf, tlvType, keyClass);
			var dataClass = (Class<?>) type.getActualTypeArguments()[1];
			log.trace("Data class: {}", dataClass.getSimpleName());
			var dataObject = readMapData(buf, tlvType, dataClass);
			log.trace("result: {}", dataObject);

			map.put(keyObject, dataObject);
		}
		log.trace("done: buf.readerIndex: {}", buf.readerIndex());
		return map;
	}

	private static int writeMapData(ByteBuf buf, TlvType tlvType, Object object)
	{
		int size;

		var sizeOffset = prepareWriteTlv(buf, tlvType);
		size = Serializer.serialize(buf, object.getClass(), object, null);
		return actuallyWriteTlv(buf, sizeOffset, size);
	}

	// XXX: we don't really need to check for the sizes everywhere. first deserialize can check the total size, then the rest just locally. just throw something if deserializing is wrong

	private static int prepareWriteTlv(ByteBuf buf, TlvType tlvType)
	{
		buf.ensureWritable(TLV_HEADER_SIZE);
		buf.writeShort(tlvType.getValue());
		var offset = buf.writerIndex();
		buf.writerIndex(offset + 4);
		return offset;
	}

	private static int actuallyWriteTlv(ByteBuf buf, int offset, int size)
	{
		size += TLV_HEADER_SIZE;
		buf.setInt(offset, size);
		return size;
	}

	private static Object readMapData(ByteBuf buf, TlvType tlvType, Class<?> javaClass)
	{
		var size = readTlv(buf, tlvType); // XXX: check size
		log.trace("Reading map data of size: {}", size);

		return Serializer.deserialize(buf, javaClass);
	}

	private static int readTlv(ByteBuf buf, TlvType tlvType)
	{
		if (buf.readShort() != tlvType.getValue())
		{
			throw new IllegalArgumentException("Wrong TLV");
		}
		return buf.readInt();
	}
}
