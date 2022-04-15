/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import static io.xeres.app.xrs.serialization.TlvType.STR_VALUE;

public class TurtleStringSearchRequestItem extends Item
{
	@RsSerialized(tlvType = STR_VALUE)
	private String search;

	@RsSerialized
	private int requestId;

	@RsSerialized
	private short depth;

	public TurtleStringSearchRequestItem()
	{
		// Required
	}

	public String getSearch()
	{
		return search;
	}

	public int getRequestId()
	{
		return requestId;
	}

	public short getDepth()
	{
		return depth;
	}

	@Override
	public String toString()
	{
		return "TurtleStringSearchRequestItem{" +
				"search='" + search + '\'' +
				", requestId=" + requestId +
				", depth=" + depth +
				'}';
	}
}
