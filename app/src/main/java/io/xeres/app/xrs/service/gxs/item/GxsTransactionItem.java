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

import io.xeres.app.xrs.serialization.FieldSize;
import io.xeres.app.xrs.serialization.RsSerialized;

import java.util.Set;

/**
 * This item is used to make a transaction, which guarantees
 * that a collection of items have been received.
 */
public class GxsTransactionItem extends GxsExchange
{
	@RsSerialized(fieldSize = FieldSize.SHORT)
	private Set<TransactionFlags> flags;

	@RsSerialized
	private int itemCount;

	@RsSerialized
	private int updateTimestamp;

	private int timestamp; // Not serialized, used for timeout detection (XXX: I don't think I need it)

	public GxsTransactionItem()
	{
		// Needed
	}

	public GxsTransactionItem(Set<TransactionFlags> flags, int itemCount, int updateTimestamp, int transactionId)
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

	public Set<TransactionFlags> getFlags()
	{
		return flags;
	}

	public int getItemCount()
	{
		return itemCount;
	}

	public int getUpdateTimestamp()
	{
		return updateTimestamp;
	}

	public int getTimestamp()
	{
		return timestamp;
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
