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

import io.xeres.app.xrs.serialization.RsClassSerializedReversed;
import io.xeres.app.xrs.serialization.RsSerialized;

import static io.xeres.app.xrs.serialization.TlvType.STR_VALUE;

@RsClassSerializedReversed
public class TurtleStringSearchRequestItem extends TurtleFileSearchRequestItem implements Cloneable
{
	@RsSerialized(tlvType = STR_VALUE)
	private String search;

	public TurtleStringSearchRequestItem()
	{
		// Required
	}

	public String getSearch()
	{
		return search;
	}

	@Override
	public String getKeywords()
	{
		return search;
	}

	@Override
	public String toString()
	{
		return "TurtleStringSearchRequestItem{" +
				"search='" + search + '\'' +
				", requestId=" + getRequestId() +
				", depth=" + getDepth() +
				'}';
	}

	@Override
	public TurtleStringSearchRequestItem clone()
	{
		TurtleStringSearchRequestItem clone = (TurtleStringSearchRequestItem) super.clone();
		// TODO: copy mutable state here, so the clone can't change the internals of the original
		return clone;
	}
}
