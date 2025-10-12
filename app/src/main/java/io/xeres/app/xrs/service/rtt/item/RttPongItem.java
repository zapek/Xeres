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

package io.xeres.app.xrs.service.rtt.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;

public class RttPongItem extends Item
{
	@RsSerialized
	private int sequenceNumber;

	@RsSerialized
	private long pingTimestamp;

	@RsSerialized
	private long pongTimestamp;

	@SuppressWarnings("unused")
	public RttPongItem()
	{
	}

	public RttPongItem(RttPingItem pingItem, long timeStamp)
	{
		sequenceNumber = pingItem.getSequenceNumber();
		pingTimestamp = pingItem.getTimestamp();
		pongTimestamp = timeStamp;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.RTT.getType();
	}

	@Override
	public int getSubType()
	{
		return 2;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.REALTIME.getPriority();
	}

	public long getPingTimestamp()
	{
		return pingTimestamp;
	}

	public long getPongTimestamp()
	{
		return pongTimestamp;
	}

	@Override
	public RttPongItem clone()
	{
		return (RttPongItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "RttPongItem{" +
				"sequenceNumber=" + sequenceNumber +
				", pingTimeStamp=" + pingTimestamp +
				", pongTimeStamp=" + pongTimestamp +
				'}';
	}
}
