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

import java.util.Set;

final class GxsMetaAndDataSerializer
{
	private GxsMetaAndDataSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, GxsMetaAndData gxsMetaAndData, Set<SerializationFlags> flags, GxsMetaAndDataResult result)
	{
		var dataSize = gxsMetaAndData.writeDataObject(buf, flags);
		var metaSize = gxsMetaAndData.writeMetaObject(buf, flags);

		if (result != null)
		{
			result.setDataSize(dataSize);
			result.setMetaSize(metaSize);
		}

		return dataSize + metaSize;
	}
}
