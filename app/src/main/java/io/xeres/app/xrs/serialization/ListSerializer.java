/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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
import java.util.ArrayList;
import java.util.List;

final class ListSerializer
{
	private static final Logger log = LoggerFactory.getLogger(ListSerializer.class);

	private ListSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, List<Object> list)
	{
		var size = Integer.BYTES;

		buf.ensureWritable(size);
		if (list != null)
		{
			log.trace("Entries in List: {}", list.size());
			buf.writeInt(list.size());
			for (var data : list)
			{
				size += Serializer.serialize(buf, data.getClass(), data, null);
			}
		}
		else
		{
			buf.writeInt(0);
		}
		return size;
	}

	static int serialize(ByteBuf buf, List<Object> list, TlvType tlvType)
	{
		var size = Integer.BYTES;

		buf.ensureWritable(size);
		if (list != null)
		{
			log.trace("Entries in List: {} with TlvType {}", list.size(), tlvType);
			buf.writeInt(list.size());
			for (var data : list)
			{
				size += TlvSerializer.serialize(buf, tlvType, data);
			}
		}
		else
		{
			buf.writeInt(0);
		}
		return size;
	}

	static List<Object> deserialize(ByteBuf buf, List<Object> list, ParameterizedType type)
	{
		if (list == null)
		{
			list = new ArrayList<>();
		}

		var entries = buf.readInt();
		var dataClass = (Class<?>) type.getActualTypeArguments()[0];
		log.trace("Data class: {}", dataClass.getSimpleName());

		while (entries-- > 0)
		{
			var dataObject = Serializer.deserialize(buf, dataClass);
			log.trace("result: {}", dataObject);
			list.add(dataObject);
		}
		return list;
	}

	static List<Object> deserialize(ByteBuf buf, List<Object> list, TlvType tlvType)
	{
		if (list == null)
		{
			list = new ArrayList<>();
		}

		var entries = buf.readInt();

		while (entries-- > 0)
		{
			var dataObject = TlvSerializer.deserialize(buf, tlvType);
			log.trace("result: {} (tlvType: {})", dataObject, tlvType);
			list.add(dataObject);
		}
		return list;
	}
}
