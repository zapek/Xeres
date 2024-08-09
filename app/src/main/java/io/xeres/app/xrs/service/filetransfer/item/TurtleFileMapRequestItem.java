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

import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.deserializeInt;
import static io.xeres.app.xrs.serialization.Serializer.serialize;

public class TurtleFileMapRequestItem extends TurtleGenericTunnelItem implements RsSerializable
{
	@Override
	public boolean shouldStampTunnel()
	{
		return false;
	}

	@Override
	public int getSubType()
	{
		return 17;
	}

	@Override
	public TurtleFileMapRequestItem clone()
	{
		return (TurtleFileMapRequestItem) super.clone();
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, getTunnelId());
		size += serialize(buf, getDirection() == TunnelDirection.CLIENT ? 1 : 2);

		return size;
	}

	@Override
	public void readObject(ByteBuf buf)
	{
		setTunnelId(deserializeInt(buf));
		var tunnelDirection = deserializeInt(buf);
		setDirection(tunnelDirection == 2 ? TunnelDirection.SERVER : TunnelDirection.CLIENT);
	}
}
