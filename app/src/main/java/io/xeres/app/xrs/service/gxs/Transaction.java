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

package io.xeres.app.xrs.service.gxs;

import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.gxs.item.GxsExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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

	public enum Type
	{
		INCOMING,
		OUTGOING
	}

	private static final Duration TRANSACTION_TIMEOUT = Duration.ofSeconds(2000);

	private final int id;
	private State state;
	private final Type type;
	private final Instant start;
	private final Duration timeout;
	private final List<T> items;
	private final int itemCount;
	private final GxsService service;

	Transaction(int id, List<T> items, int itemCount, GxsService service, Type type)
	{
		if (itemCount == 0)
		{
			throw new IllegalArgumentException("Can't create an empty transaction");
		}
		this.id = id;
		this.items = items;
		this.itemCount = itemCount;
		this.timeout = TRANSACTION_TIMEOUT;
		this.service = service;
		this.state = type == Type.OUTGOING ? State.WAITING_CONFIRMATION : State.STARTING;
		this.type = type;
		this.start = Instant.now();
	}

	public int getId()
	{
		return id;
	}

	public State getState()
	{
		return state;
	}

	public Type getType()
	{
		return type;
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
		log.debug("expected number of items: {}, current number of items: {}", itemCount, items.size());
		return itemCount == items.size();
	}

	public boolean hasTimeout()
	{
		return start.plus(timeout).isBefore(Instant.now());
	}
}
