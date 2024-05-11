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

package io.xeres.app.xrs.service.turtle.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;

public abstract class TurtleGenericTunnelItem extends Item implements Cloneable
{
	@RsSerialized
	private int tunnelId;

	// This is not serialized
	private TunnelDirection direction;

	public abstract boolean shouldStampTunnel();

	protected TurtleGenericTunnelItem()
	{
		// Needed
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.TURTLE.getType();
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.NORMAL.getPriority();
	}

	public int getTunnelId()
	{
		return tunnelId;
	}

	public void setTunnelId(int tunnelId)
	{
		this.tunnelId = tunnelId;
	}

	public TunnelDirection getDirection()
	{
		return direction;
	}

	public void setDirection(TunnelDirection direction)
	{
		this.direction = direction;
	}

	@Override
	public TurtleGenericTunnelItem clone()
	{
		try
		{
			var clone = (TurtleGenericTunnelItem) super.clone();
			clone.buf = null; // the cloning is done to write multiple buffers, we don't need to copy it
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			throw new AssertionError();
		}
	}
}
