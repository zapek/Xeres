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
import io.xeres.app.database.model.gxs.GxsGroupItem;

import java.util.Set;

final class GxsGroupSerializer
{
	private GxsGroupSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, GxsGroupItem gxsGroupItem, Set<SerializationFlags> flags)
	{
		var metaSize = 0;
		metaSize += gxsGroupItem.writeMetaObject(buf, flags);

		var groupSize = 0;
		groupSize += Serializer.serialize(buf, (byte) 2);
		groupSize += Serializer.serialize(buf, (short) gxsGroupItem.getServiceType().getType());
		groupSize += Serializer.serialize(buf, (byte) 2);
		var sizeOffset = buf.writerIndex();
		groupSize += Serializer.serialize(buf, 0); // write size at end

		groupSize += gxsGroupItem.writeGroupObject(buf, flags);

		buf.setInt(sizeOffset, groupSize); // write group size

		return metaSize + groupSize;
	}

	static void deserialize(ByteBuf buf, GxsGroupItem gxsGroupItem)
	{
		gxsGroupItem.readMetaObject(buf);
		readFakeHeader(buf, gxsGroupItem);
		gxsGroupItem.readGroupObject(buf);
	}

	private static void readFakeHeader(ByteBuf buf, GxsGroupItem gxsGroupItem)
	{
		if (buf.readByte() != 2)
		{
			throw new IllegalArgumentException("Packet version is not 0x2");
		}
		if (buf.readShort() != gxsGroupItem.getServiceType().getType())
		{
			throw new IllegalArgumentException("Packet type is not " + gxsGroupItem.getServiceType().getType());
		}
		if (buf.readByte() != 0x2)
		{
			throw new IllegalArgumentException("Packet subtype is not " + 0x2);
		}
		buf.readInt(); // size
	}
}
