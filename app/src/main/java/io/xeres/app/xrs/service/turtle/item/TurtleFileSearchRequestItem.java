/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

/**
 * The superclass of all file search requests.
 */
public abstract class TurtleFileSearchRequestItem extends TurtleSearchRequestItem
{
	@Override
	public String getKeywords()
	{
		return "";
	}

	@Override
	public TurtleFileSearchRequestItem clone()
	{
		return (TurtleFileSearchRequestItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "TurtleFileSearchRequestItem{}";
	}
}
