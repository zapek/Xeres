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

import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.GxsId;

/**
 * Item used to send the list of new groups that we have for a peer.
 */
public class GxsSyncGroupItem extends GxsExchange
{
	public static final byte REQUEST = 0x1;
	public static final byte RESPONSE = 0x2;

	@RsSerialized
	private byte flags;

	@RsSerialized
	private GxsId groupId;

	@RsSerialized
	private int publishTimestamp;

	@RsSerialized
	private GxsId authorId;

	@SuppressWarnings("unused")
	public GxsSyncGroupItem()
	{
		// Needed
	}

	public GxsSyncGroupItem(byte flags, GxsGroupItem groupItem, int transactionId)
	{
		this.flags = flags;
		publishTimestamp = (int) groupItem.getPublished().getEpochSecond();
		groupId = groupItem.getGxsId();
		authorId = groupItem.getAuthor();
		setTransactionId(transactionId);
	}

	public GxsSyncGroupItem(byte flags, GxsId groupId, int transactionId)
	{
		this.flags = flags;
		this.groupId = groupId;
		setTransactionId(transactionId);
	}

	public GxsId getGroupId()
	{
		return groupId;
	}

	public int getPublishTimestamp()
	{
		return publishTimestamp;
	}

	@Override
	public String toString()
	{
		return "GxsSyncGroupItem{" +
				"flags=" + flags +
				", publishTimestamp=" + publishTimestamp +
				", groupId=" + groupId +
				", authorId=" + authorId +
				'}';
	}
}
