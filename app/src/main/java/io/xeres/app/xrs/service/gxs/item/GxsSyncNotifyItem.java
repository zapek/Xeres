/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.gxs.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;

/**
 * Item used to tell the peer that there have been changes, and it should request them immediately without
 * waiting for the next sync delay.
 */
public class GxsSyncNotifyItem extends Item implements DynamicServiceType
{
	private int serviceType;

	@Override
	public int getSubType()
	{
		return 144;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.HIGH.getPriority(); // XXX: not sure...
	}

	@Override
	public int getServiceType()
	{
		return serviceType;
	}

	@Override
	public void setServiceType(int serviceType)
	{
		this.serviceType = serviceType;
	}

	@Override
	public GxsSyncNotifyItem clone()
	{
		return (GxsSyncNotifyItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "GxsSyncNotifyItem {}";
	}
}
