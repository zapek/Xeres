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

import java.util.ArrayList;
import java.util.List;

/**
 * The results of a file search.
 */
public class TurtleFileSearchResultItem extends TurtleSearchResultItem
{
	@RsSerialized
	private List<TurtleFileInfo> results = new ArrayList<>();

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

	@Override
	public void trim(int size)
	{
		if (size < results.size())
		{
			results = results.subList(0, size);
		}
	}

	public List<TurtleFileInfo> getResults()
	{
		return results;
	}

	public void addFileInfo(TurtleFileInfo fileInfo)
	{
		results.add(new TurtleFileInfo());
	}

	@Override
	public TurtleFileSearchResultItem clone()
	{
		return (TurtleFileSearchResultItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "TurtleFileSearchResultItem{" +
				"results=" + results +
				'}';
	}
}
