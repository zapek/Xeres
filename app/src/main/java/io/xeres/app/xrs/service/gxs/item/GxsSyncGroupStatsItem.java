/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.GxsId;

/**
 * This item is used to request statistics about a group.
 * Note that it doesn't extend GxsExchange because it doesn't use transactions.
 */
public class GxsSyncGroupStatsItem extends Item implements DynamicServiceType
{
	@RsSerialized
	private RequestType requestType;

	@RsSerialized
	private GxsId groupId;

	@RsSerialized
	private int numberOfPosts;

	@RsSerialized
	private int lastPostTimestamp;

	private int serviceType;

	@SuppressWarnings("unused")
	public GxsSyncGroupStatsItem()
	{
	}

	public GxsSyncGroupStatsItem(RequestType requestType, GxsId groupId)
	{
		this(requestType, groupId, 0, 0);
	}

	public GxsSyncGroupStatsItem(RequestType requestType, GxsId groupId, int lastPostTimestamp, int numberOfPosts)
	{
		this.requestType = requestType;
		this.groupId = groupId;
		this.lastPostTimestamp = lastPostTimestamp;
		this.numberOfPosts = numberOfPosts;
	}

	@Override
	public int getSubType()
	{
		return 3;
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

	public RequestType getRequestType()
	{
		return requestType;
	}

	public GxsId getGroupId()
	{
		return groupId;
	}

	public int getNumberOfPosts()
	{
		return numberOfPosts;
	}

	public int getLastPostTimestamp()
	{
		return lastPostTimestamp;
	}

	@Override
	public GxsSyncGroupStatsItem clone()
	{
		return (GxsSyncGroupStatsItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "GxsSyncGroupStatsItem{" +
				"requestType=" + requestType +
				", groupId=" + groupId +
				", numberOfPosts=" + numberOfPosts +
				", lastPostTimestamp=" + lastPostTimestamp +
				'}';
	}
}
