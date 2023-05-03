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
import io.xeres.app.database.model.gxs.GxsMetaAndData;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemHeader;

import java.util.Set;

final class GxsMetaAndDataSerializer
{
	private GxsMetaAndDataSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, GxsMetaAndData gxsMetaAndData, Set<SerializationFlags> flags)
	{
		var metaSize = 0;
		metaSize += gxsMetaAndData.writeMetaObject(buf, flags);

		var itemHeader = new ItemHeader(buf, ((Item) gxsMetaAndData).getService().getServiceType().getType(), 3); // XXX: is 3 correct?
		itemHeader.writeHeader();
		var dataSize = gxsMetaAndData.writeDataObject(buf, flags);
		itemHeader.writeSize(dataSize);

		return metaSize + dataSize;
	}

	static void deserialize(ByteBuf buf, GxsMetaAndData gxsMetaAndData)
	{
		gxsMetaAndData.readMetaObject(buf);
		ItemHeader.readHeader(buf, ((Item) gxsMetaAndData).getService().getServiceType().getType(), 3);
		gxsMetaAndData.readDataObject(buf);
	}
}
