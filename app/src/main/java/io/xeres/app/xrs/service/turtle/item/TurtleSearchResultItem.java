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

package io.xeres.app.xrs.service.turtle.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;

/**
 * The superclass of all search result items.
 */
public abstract class TurtleSearchResultItem extends Item
{
	@RsSerialized
	private int requestId;

	@SuppressWarnings("unused")
	@RsSerialized
	private short depth; // Always set to 0, not used

	public abstract int getCount();

	public abstract void trim(int size);

	@Override
	public int getServiceType()
	{
		return RsServiceType.TURTLE_ROUTER.getType();
	}

	public int getRequestId()
	{
		return requestId;
	}

	public void setRequestId(int requestId)
	{
		this.requestId = requestId;
	}

	@Override
	public TurtleSearchResultItem clone()
	{
		return (TurtleSearchResultItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "TurtleSearchResultItem{" +
				"requestId=" + requestId +
				", depth=" + depth +
				'}';
	}
}
