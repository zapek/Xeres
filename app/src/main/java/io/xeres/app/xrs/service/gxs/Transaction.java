/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.gxs;

import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.gxs.item.GxsExchange;
import io.xeres.app.xrs.service.gxs.item.TransactionFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * A Transaction is a way to group multiple items of the same type that have the same transaction id. Transactions can be outgoing or incoming and have
 * different states. Once a transaction is complete, its items can be accessed.
 *
 * @param <T> the GxsExchange type. GxsExchange for incoming transactions and a subclass for outgoing transactions.
 * @see GxsTransactionManager
 */
public class Transaction<T extends GxsExchange>
{
	private static final Logger log = LoggerFactory.getLogger(Transaction.class);

	public enum State
	{
		STARTING,
		RECEIVING,
		SENDING,
		COMPLETED,
		FAILED,
		WAITING_CONFIRMATION
	}

	public enum Direction
	{
		INCOMING,
		OUTGOING
	}

	public static final Duration TRANSACTION_TIMEOUT = Duration.ofSeconds(2000);

	private final int id;
	private State state;
	private final Direction direction;
	private final Set<TransactionFlags> transactionFlags;
	private final Instant start;
	private final Duration timeout;
	private final List<T> items;
	private final int itemCount;
	private final GxsRsService<? extends GxsGroupItem, ? extends GxsMessageItem> service;
	private Instant updated;

	Transaction(int id, Set<TransactionFlags> transactionFlags, List<T> items, int itemCount, GxsRsService<? extends GxsGroupItem, ? extends GxsMessageItem> service, Direction direction)
	{
		this.id = id;
		this.transactionFlags = transactionFlags;
		this.items = items;
		this.itemCount = itemCount;
		timeout = TRANSACTION_TIMEOUT;
		this.service = service;
		state = direction == Direction.OUTGOING ? State.WAITING_CONFIRMATION : State.STARTING;
		this.direction = direction;
		start = Instant.now();
	}

	public int getId()
	{
		return id;
	}

	public State getState()
	{
		return state;
	}

	public Direction getDirection()
	{
		return direction;
	}

	public void setState(State state)
	{
		this.state = state;
	}

	public List<T> getItems()
	{
		return items;
	}

	@SuppressWarnings("unchecked")
	public void addItem(GxsExchange item)
	{
		items.add((T) item);
	}

	public RsService getService()
	{
		return service;
	}

	public boolean hasAllItems()
	{
		log.trace("expected number of items: {}, current number of items: {}", itemCount, items.size());
		return itemCount == items.size();
	}

	public boolean hasTimeout()
	{
		return start.plus(timeout).isBefore(Instant.now());
	}

	public Set<TransactionFlags> getTransactionFlags()
	{
		return transactionFlags;
	}

	public Instant getUpdated()
	{
		return updated;
	}

	public void setUpdated(Instant updated)
	{
		this.updated = updated;
	}

	@Override
	public String toString()
	{
		return "Transaction{" +
				"id=" + id +
				", flags=" + transactionFlags +
				", state=" + state +
				", type=" + direction +
				", itemCount=" + itemCount +
				'}';
	}
}
