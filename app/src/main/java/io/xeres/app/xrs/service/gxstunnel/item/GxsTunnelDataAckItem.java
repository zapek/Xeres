/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.gxstunnel.item;

import io.xeres.app.xrs.serialization.RsSerialized;

public class GxsTunnelDataAckItem extends GxsTunnelItem
{
	@RsSerialized
	private long counter;

	@Override
	public int getSubType()
	{
		return 4;
	}

	public GxsTunnelDataAckItem()
	{
		// Needed
	}

	public GxsTunnelDataAckItem(long counter)
	{
		this.counter = counter;
	}

	public long getCounter()
	{
		return counter;
	}

	@Override
	public GxsTunnelDataAckItem clone()
	{
		return (GxsTunnelDataAckItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "GxsTunnelDataAckItem{" +
				"counter=" + counter +
				'}';
	}
}
