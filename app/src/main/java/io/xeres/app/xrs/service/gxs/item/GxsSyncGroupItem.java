/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.serialization.FieldSize;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.GxsId;

import java.util.Set;

/**
 * Item used to send the list of groups to a peer.
 */
public class GxsSyncGroupItem extends GxsExchange
{
	@RsSerialized(fieldSize = FieldSize.BYTE)
	private Set<SyncFlags> flags;

	@RsSerialized
	private GxsId groupId;

	@RsSerialized
	private int publishTimestamp;

	@RsSerialized
	private GxsId authorId;

	public GxsSyncGroupItem()
	{
		// Needed
	}

	public GxsSyncGroupItem(Set<SyncFlags> flags, GxsGroupItem groupItem, int transactionId)
	{
		this.flags = flags;
		this.publishTimestamp = (int) groupItem.getPublished().getEpochSecond();
		this.groupId = groupItem.getGxsId();
		this.authorId = groupItem.getAuthor();
		setTransactionId(transactionId);
	}

	public GxsSyncGroupItem(Set<SyncFlags> flags, GxsId groupId, int transactionId)
	{
		this.flags = flags;
		this.groupId = groupId;
		setTransactionId(transactionId);
	}

	public GxsId getGroupId()
	{
		return groupId;
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
