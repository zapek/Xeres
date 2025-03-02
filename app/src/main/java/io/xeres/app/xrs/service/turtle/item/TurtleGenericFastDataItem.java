/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

import io.xeres.app.xrs.item.ItemPriority;

/**
 * Used by any service to pass on arbitrary data into a tunnel.
 * <p>
 * Same as {@link TurtleGenericDataItem} but with a fast priority. Can be
 * used for example by distant chat.
 */
public class TurtleGenericFastDataItem extends TurtleGenericDataItem
{
	public TurtleGenericFastDataItem()
	{
		// Required
	}

	public TurtleGenericFastDataItem(byte[] data)
	{
		super(data);
	}

	@Override
	public int getSubType()
	{
		return 22;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.INTERACTIVE.getPriority();
	}

	@Override
	public String toString()
	{
		return "TurtleGenericFastDataItem{" +
				"tunnelData.length=" + (getTunnelData() == null ? "[null]" : getTunnelData().length) +
				'}';
	}

	@Override
	public TurtleGenericFastDataItem clone()
	{
		return (TurtleGenericFastDataItem) super.clone();
	}
}
