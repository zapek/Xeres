/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.filetransfer.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.turtle.item.TunnelDirection;
import io.xeres.app.xrs.service.turtle.item.TurtleGenericTunnelItem;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;

public class TurtleFileMapItem extends TurtleGenericTunnelItem implements Cloneable, RsSerializable
{
	private List<Integer> compressedChunks;

	public TurtleFileMapItem()
	{
		// Required
	}

	public TurtleFileMapItem(List<Integer> compressedChunks)
	{
		this.compressedChunks = compressedChunks;
	}

	@Override
	public boolean shouldStampTunnel()
	{
		return false;
	}

	@Override
	public int getSubType()
	{
		return 16;
	}

	@Override
	public TurtleFileMapItem clone()
	{
		var clone = (TurtleFileMapItem) super.clone();
		clone.compressedChunks = new ArrayList<>(compressedChunks);
		return clone;
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, getTunnelId());
		size += serialize(buf, getDirection() == TunnelDirection.CLIENT ? 1 : 2);
		//noinspection unchecked
		size += serialize(buf, (List<Object>) (List<?>) compressedChunks);

		return size;
	}

	@Override
	public void readObject(ByteBuf buf)
	{
		setTunnelId(deserializeInt(buf));
		setDirection(TunnelDirection.values()[deserializeInt(buf)]);
		//noinspection unchecked
		compressedChunks = (List<Integer>) (List<?>) deserializeList(buf, new ParameterizedType()
		{
			@Override
			public Type[] getActualTypeArguments()
			{
				return new Type[]{Integer.class};
			}

			@Override
			public Type getRawType()
			{
				return List.class;
			}

			@Override
			public Type getOwnerType()
			{
				return null;
			}
		});
	}
}
