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

import java.time.Instant;

import static io.xeres.app.xrs.serialization.TlvType.STR_HASH_SHA1;

/**
 * Item used to request group list from a peer.
 */
public class GxsSyncGroupRequestItem extends GxsExchange
{
	@RsSerialized
	private byte flag; // unused

	@RsSerialized
	private int createdSince; // how far back to sync data

	@RsSerialized(tlvType = STR_HASH_SHA1)
	private String syncHash; // unused. This is old stuff where it used to transfer files instead of building tunnels

	@RsSerialized
	private int updateTimestamp; // last group update

	public GxsSyncGroupRequestItem()
	{
		// Needed
	}

	public GxsSyncGroupRequestItem(Instant lastUpdate)
	{
		this.updateTimestamp = (int) lastUpdate.getEpochSecond();
	}

	public int getCreatedSince()
	{
		return createdSince;
	}

	public void setCreatedSince(int createdSince)
	{
		this.createdSince = createdSince;
	}

	public int getUpdateTimestamp()
	{
		return updateTimestamp;
	}

	public void setUpdateTimestamp(int updateTimestamp)
	{
		this.updateTimestamp = updateTimestamp;
	}

	@Override
	public String toString()
	{
		return "GxsSyncGroupRequestItem{" +
				", flag=" + flag +
				", createdSince=" + createdSince +
				", syncHash='" + syncHash + '\'' +
				", updateTimestamp=" + updateTimestamp +
				", super=" + super.toString() +
				'}';
	}
}
