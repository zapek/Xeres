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

package io.xeres.app.xrs.service.gxs.item;

import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.GxsId;

/**
 * This item is used to request statistics about a group.
 */
public class GxsSyncGroupStatsItem extends GxsExchange
{
	@RsSerialized
	private RequestType requestType;

	@RsSerialized
	private GxsId groupId;

	@RsSerialized
	private int numberOfPosts;

	@RsSerialized
	private int lastPostTimestamp;

	public GxsSyncGroupStatsItem()
	{
		// Needed
	}

	@Override
	public int getSubType()
	{
		return 3;
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
