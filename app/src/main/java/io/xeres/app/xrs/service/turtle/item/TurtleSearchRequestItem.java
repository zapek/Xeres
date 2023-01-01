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

package io.xeres.app.xrs.service.turtle.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;

public abstract class TurtleSearchRequestItem extends Item implements Cloneable
{
	@RsSerialized
	private int requestId;

	@RsSerialized
	private short depth;

	public abstract String getKeywords();

	// XXX: Item has getService()... how do we implement that? (ie. how can another service use turtle itself?)

	protected TurtleSearchRequestItem()
	{
		// Needed
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.HIGH.getPriority();
	}

	public int getRequestId()
	{
		return requestId;
	}

	public short getDepth()
	{
		return depth;
	}

	public void setDepth(short depth)
	{
		this.depth = depth;
	}

	@Override
	public TurtleSearchRequestItem clone()
	{
		try
		{
			var clone = (TurtleSearchRequestItem) super.clone();
			clone.buf = null; // the cloning is done to write multiple buffers, we don't need to copy it
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			throw new AssertionError();
		}
	}
}
