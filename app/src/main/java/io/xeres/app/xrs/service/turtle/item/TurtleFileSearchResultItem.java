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

import io.xeres.app.xrs.serialization.RsSerialized;

import java.util.List;

public class TurtleFileSearchResultItem extends TurtleSearchResultItem implements Cloneable
{
	@RsSerialized
	private List<TurtleFileInfo> results;

	public TurtleFileSearchResultItem()
	{
		// Needed
	}

	@Override
	public int getSubType()
	{
		return 2;
	}

	@Override
	public int getCount()
	{
		return results.size();
	}

	public List<TurtleFileInfo> getResults()
	{
		return results;
	}

	@Override
	public TurtleFileSearchResultItem clone()
	{
		var clone = (TurtleFileSearchResultItem) super.clone();
		// XXX: copy results I think... since it's no deep copy...
		return clone;
	}

	@Override
	public String toString()
	{
		return "TurtleFileSearchResultItem{" +
				"results=" + results +
				'}';
	}
}
