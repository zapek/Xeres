/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

import java.time.Duration;
import java.time.Instant;

import static io.xeres.app.xrs.serialization.TlvType.STR_HASH_SHA1;

/**
 * Item used to request messages from a peer's group.
 */
public class GxsSyncMessageRequestItem extends GxsExchange
{
	public static final byte USE_HASHED_GROUP_ID = 0x2; // Use this when implementing circles (avoids someone outside the circle to know to which group we're subscribed)

	@RsSerialized
	private byte flags;

	@RsSerialized
	private int limit; // how far back to sync data

	@RsSerialized(tlvType = STR_HASH_SHA1)
	private String syncHash;

	@RsSerialized
	private GxsId gxsId;

	@RsSerialized
	private int lastUpdated;

	@SuppressWarnings("unused")
	public GxsSyncMessageRequestItem()
	{
	}

	public GxsSyncMessageRequestItem(GxsId gxsId, Instant lastUpdated, Duration limit)
	{
		this.gxsId = gxsId;
		this.lastUpdated = (int) lastUpdated.getEpochSecond();
		this.limit = (int) Instant.now().minus(limit).getEpochSecond();
	}

	@Override
	public int getSubType()
	{
		return 16;
	}

	public int getLimit()
	{
		return limit;
	}

	public void setLimit(int limit)
	{
		this.limit = limit;
	}

	public String getSyncHash()
	{
		return syncHash;
	}

	public void setSyncHash(String syncHash)
	{
		this.syncHash = syncHash;
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public void setGxsId(GxsId gxsId)
	{
		this.gxsId = gxsId;
	}

	public int getLastUpdated()
	{
		return lastUpdated;
	}

	public void setLastUpdated(int lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	@Override
	public GxsSyncMessageRequestItem clone()
	{
		return (GxsSyncMessageRequestItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "GxsSyncMessageRequestItem{" +
				"flags=" + flags +
				", gxsId=" + gxsId +
				", syncHash='" + syncHash + '\'' +
				", lastUpdated=" + Instant.ofEpochSecond(lastUpdated) +
				", limit=" + Instant.ofEpochSecond(limit) +
				", super=" + super.toString() +
				'}';
	}
}
