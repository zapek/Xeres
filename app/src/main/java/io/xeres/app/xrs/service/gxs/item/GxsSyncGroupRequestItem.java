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
 * Item used to request new group list from a peer. Sent each minute with
 * the last syncing time.
 */
public class GxsSyncGroupRequestItem extends GxsExchange
{
	@RsSerialized
	private byte flags; // unused

	@RsSerialized
	private int createdSince; // unused

	@RsSerialized(tlvType = STR_HASH_SHA1)
	private String syncHash; // unused. This is old stuff where it used to transfer files instead of building tunnels

	@RsSerialized
	private int lastUpdated; // last group update

	public GxsSyncGroupRequestItem()
	{
		// Needed
	}

	public GxsSyncGroupRequestItem(Instant lastUpdated)
	{
		this.lastUpdated = (int) lastUpdated.getEpochSecond();
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
	public String toString()
	{
		return "GxsSyncGroupRequestItem{" +
				", flags=" + flags +
				", createdSince=" + createdSince +
				", syncHash='" + syncHash + '\'' +
				", lastUpdated=" + lastUpdated +
				", super=" + super.toString() +
				'}';
	}
}
