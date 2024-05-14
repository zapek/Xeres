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

import java.util.Arrays;

public class TurtleGenericSearchResultItem extends TurtleSearchResultItem implements Cloneable
{
	@RsSerialized
	private byte[] searchData; // XXX: not sure it's the right data type

	@SuppressWarnings("unused")
	public TurtleGenericSearchResultItem()
	{
	}

	@Override
	public int getSubType()
	{
		return 12;
	}

	public byte[] getSearchData()
	{
		return searchData;
	}

	@Override
	public int getCount()
	{
		return searchData.length / 50; // XXX: this is an estimate... probably wrong
	}

	@Override
	public void trim(int size)
	{
		// XXX: implement?
	}

	@Override
	public TurtleGenericSearchResultItem clone()
	{
		var clone = (TurtleGenericSearchResultItem) super.clone();
		// XXXX: not sure if I have to copy...
		return clone;
	}

	@Override
	public String toString()
	{
		return "TurtleGenericSearchResultItem{" +
				"searchData=" + Arrays.toString(searchData) +
				'}';
	}
}
