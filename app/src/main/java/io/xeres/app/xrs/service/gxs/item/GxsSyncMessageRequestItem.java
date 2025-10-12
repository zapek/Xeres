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

package io.xeres.app.xrs.service.gxs.item;

import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.GxsId;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static io.xeres.app.xrs.serialization.TlvType.STR_HASH_SHA1;

/**
 * Item used to request messages from a peer's group.
 */
public class GxsSyncMessageRequestItem extends GxsExchange
{
	public static final byte USE_HASHED_GROUP_ID = 0x2;

	@RsSerialized
	private byte flags;

	@RsSerialized
	private int createSince; // how far back to sync data

	@RsSerialized(tlvType = STR_HASH_SHA1)
	private String syncHash;

	@RsSerialized
	private GxsId groupId;

	@RsSerialized
	private int lastUpdated;

	@SuppressWarnings("unused")
	public GxsSyncMessageRequestItem()
	{
	}

	public GxsSyncMessageRequestItem(GxsId groupId, Instant lastUpdated, Duration limit)
	{
		this.groupId = groupId;
		this.lastUpdated = (int) lastUpdated.getEpochSecond();
		createSince = (int) getMostRecent(lastUpdated, limit).getEpochSecond();
	}

	private static Instant getMostRecent(Instant last, Duration limit)
	{
		return Stream.of(Instant.now().minus(limit), last)
				.max(Instant::compareTo)
				.get();
	}

	@Override
	public int getSubType()
	{
		return 16;
	}

	public int getCreateSince()
	{
		return createSince;
	}

	public void setCreateSince(int createSince)
	{
		this.createSince = createSince;
	}

	public String getSyncHash()
	{
		return syncHash;
	}

	public void setSyncHash(String syncHash)
	{
		this.syncHash = syncHash;
	}

	public GxsId getGroupId()
	{
		return groupId;
	}

	public void setGroupId(GxsId groupId)
	{
		this.groupId = groupId;
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
				", createSince=" + Instant.ofEpochSecond(createSince) +
				", syncHash='" + syncHash + '\'' +
				", groupId=" + groupId +
				", lastUpdated=" + Instant.ofEpochSecond(lastUpdated) +
				", super=" + super.toString() +
				'}';
	}
}
