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

import io.xeres.app.xrs.serialization.FieldType;
import io.xeres.app.xrs.serialization.RsSerialized;

import java.util.Set;

/// This item is used to make a transaction, which guarantees
/// that a collection of items have been received.
public class GxsTransactionItem extends GxsExchange
{
	@RsSerialized(fieldType = FieldType.SHORT_SIGNED)
	private Set<TransactionFlags> flags;

	@RsSerialized
	private int itemCount;

	@RsSerialized(fieldType = FieldType.INTEGER_UNSIGNED)
	private long updateTimestamp;

	private int timestamp; // Not serialized, used for timeout detection (XXX: I don't think I need it)

	@SuppressWarnings("unused")
	public GxsTransactionItem()
	{
	}

	public GxsTransactionItem(Set<TransactionFlags> flags, int itemCount, long updateTimestamp, int transactionId)
	{
		this.flags = flags;
		this.itemCount = itemCount;
		this.updateTimestamp = updateTimestamp;
		setTransactionId(transactionId);
	}

	public GxsTransactionItem(Set<TransactionFlags> flags, int transactionId)
	{
		this.flags = flags;
		setTransactionId(transactionId);
	}

	@Override
	public int getSubType()
	{
		return 64;
	}

	public Set<TransactionFlags> getFlags()
	{
		return flags;
	}

	public int getItemCount()
	{
		return itemCount;
	}

	public long getUpdateTimestamp()
	{
		return updateTimestamp;
	}

	public int getTimestamp()
	{
		return timestamp;
	}

	@Override
	public GxsTransactionItem clone()
	{
		return (GxsTransactionItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "GxsTransactionItem{" +
				"transactionFlag=" + flags +
				", itemCount=" + itemCount +
				", updateTimestamp=" + updateTimestamp +
				", super=" + super.toString() +
				'}';
	}
}
