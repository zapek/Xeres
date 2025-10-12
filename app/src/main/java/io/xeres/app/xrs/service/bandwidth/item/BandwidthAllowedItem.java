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

package io.xeres.app.xrs.service.bandwidth.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.app.xrs.service.RsServiceType;

public class BandwidthAllowedItem extends Item
{
	@RsSerialized(tlvType = TlvType.INT_BANDWIDTH)
	private int allowedBandwidth;

	@SuppressWarnings("unused")
	public BandwidthAllowedItem()
	{
	}

	public BandwidthAllowedItem(long allowedBandwidth)
	{
		this.allowedBandwidth = toUnsignedIntSaturated(allowedBandwidth);
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.BANDWIDTH_CONTROL.getType();
	}

	@Override
	public int getSubType()
	{
		return 1;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.REALTIME.getPriority();
	}

	public long getAllowedBandwidth()
	{
		return Integer.toUnsignedLong(allowedBandwidth);
	}

	private static int toUnsignedIntSaturated(long value)
	{
		if (value >= 4_294_967_296L)
		{
			return Integer.MIN_VALUE; // Maximum value of an unsigned int
		}
		else
		{
			return (int) (value & 0xFFFFFFFFL);
		}
	}

	@Override
	public BandwidthAllowedItem clone()
	{
		return (BandwidthAllowedItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "BandwidthAllowedItem{" +
				"allowedBandwidth=" + allowedBandwidth +
				'}';
	}
}
