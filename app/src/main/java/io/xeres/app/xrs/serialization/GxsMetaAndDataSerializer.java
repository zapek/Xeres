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
import io.xeres.app.database.model.gxs.GxsMetaData;
import io.xeres.app.xrs.item.Item;

import java.util.Set;

final class GxsMetaAndDataSerializer
{
	private GxsMetaAndDataSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, GxsMetaData gxsMetaData, Set<SerializationFlags> flags)
	{
		var metaSize = 0;
		metaSize += gxsMetaData.writeMetaObject(buf, flags);

		var dataSize = 0;
		dataSize += Serializer.serialize(buf, (byte) 2);
		dataSize += Serializer.serialize(buf, (short) ((Item)gxsMetaData).getService().getServiceType().getType());
		dataSize += Serializer.serialize(buf, (byte) 2);
		var sizeOffset = buf.writerIndex();
		dataSize += Serializer.serialize(buf, 0); // write size at end

		dataSize += gxsMetaData.writeDataObject(buf, flags);

		buf.setInt(sizeOffset, dataSize); // write group size

		return metaSize + dataSize;
	}

	static void deserialize(ByteBuf buf, GxsMetaData gxsMetaData)
	{
		gxsMetaData.readMetaObject(buf);
		readFakeHeader(buf, gxsMetaData);
		gxsMetaData.readDataObject(buf);
	}

	private static void readFakeHeader(ByteBuf buf, GxsMetaData gxsMetaData)
	{
		if (buf.readByte() != 2)
		{
			throw new IllegalArgumentException("Packet version is not 0x2");
		}
		if (buf.readShort() != ((Item)gxsMetaData).getService().getServiceType().getType())
		{
			throw new IllegalArgumentException("Packet type is not " + ((Item)gxsMetaData).getService().getServiceType().getType());
		}
		if (buf.readByte() != 0x2)
		{
			throw new IllegalArgumentException("Packet subtype is not " + 0x2);
		}
		buf.readInt(); // size
	}
}
