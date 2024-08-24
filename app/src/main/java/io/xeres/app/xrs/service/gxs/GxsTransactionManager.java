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

package io.xeres.app.xrs.service.gxs;

import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.service.gxs.Transaction.Direction;
import io.xeres.app.xrs.service.gxs.item.*;
import io.xeres.common.id.LocationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.xeres.app.xrs.service.gxs.Transaction.Direction.INCOMING;
import static io.xeres.app.xrs.service.gxs.Transaction.Direction.OUTGOING;
import static io.xeres.app.xrs.service.gxs.Transaction.State;
import static io.xeres.app.xrs.service.gxs.item.TransactionFlags.*;

/**
 * Manages incoming and outgoing transactions.
 * Transactions work this way:
 * <p>
 * <b>Incoming transactions:</b>
 * <ul>
 *     <li>we receive a GxsTransactionItem with flag START which contains the expected number of items</li>
 *     <li>we send back a GxsTransactionItem with flag START_ACKNOWLEDGE</li>
 *     <li>the peer sends GxsExchange items</li>
 *     <li>once we have received all items, we send a GxsTransactionItem with flag END_SUCCESS</li>
 * </ul>
 * <p>
 * <b>Outgoing transactions:</b>
 * <ul>
 *     <li>we send a GxsTransactionItem with flag START which contains the expected number of items</li>
 *     <li>the peer sends back a GxsTransactionItem with flag START_ACKNOWLEDGE</li>
 *     <li>we send GxsExchange items to the peer</li>
 *     <li>once the peer has received all the items, it sends back a GxsTransactionItem with flag END_SUCCESS</li>
 * </ul>
 * <p>
 * <img src="doc-files/transaction.png" alt="Transaction diagram">
 * @see Transaction
 */
@Service
public class GxsTransactionManager
{
	private static final Logger log = LoggerFactory.getLogger(GxsTransactionManager.class);

	private final PeerConnectionManager peerConnectionManager;

	private final Map<LocationId, Map<Integer, Transaction<?>>> incomingTransactions = new ConcurrentHashMap<>();
	private final Map<LocationId, Map<Integer, Transaction<?>>> outgoingTransactions = new ConcurrentHashMap<>();

	public GxsTransactionManager(PeerConnectionManager peerConnectionManager)
	{
		this.peerConnectionManager = peerConnectionManager;
	}

	/**
	 * Starts an outgoing transaction to request a list of gxs group IDs that we want the peer to transfer to us.
	 *
	 * @param peerConnection the peer
	 * @param items          gxs group IDs
	 * @param transactionId  the transaction ID
	 * @param gxsRsService   the service the transaction is bound to
	 */
	public void startOutgoingTransactionForGroupIdRequest(PeerConnection peerConnection, List<GxsSyncGroupItem> items, int transactionId, GxsRsService<? extends GxsGroupItem, ? extends GxsMessageItem> gxsRsService)
	{
		var transaction = new Transaction<>(transactionId, EnumSet.of(START, TYPE_GROUP_LIST_REQUEST), items, items.size(), gxsRsService, OUTGOING);
		startOutgoingTransaction(peerConnection, transaction, Instant.EPOCH);
	}

	/**
	 * Starts an outgoing transaction to request a list of gxs message IDs that we want the peer to transfer to us.
	 *
	 * @param peerConnection the peer
	 * @param items          gxs message IDs
	 * @param transactionId  the transaction ID
	 * @param gxsRsService   the service the transaction is bound to
	 */
	public void startOutgoingTransactionForMessageIdRequest(PeerConnection peerConnection, List<GxsSyncMessageItem> items, int transactionId, GxsRsService<? extends GxsGroupItem, ? extends GxsMessageItem> gxsRsService)
	{
		var transaction = new Transaction<>(transactionId, EnumSet.of(START, TYPE_MESSAGE_LIST_REQUEST), items, items.size(), gxsRsService, OUTGOING);
		startOutgoingTransaction(peerConnection, transaction, Instant.EPOCH);
	}

	/**
	 * Starts an outgoing transaction to respond with a list of gxs group IDs that we have and their update time.
	 *
	 * @param peerConnection the peer
	 * @param items          gxs group IDs
	 * @param update         the last update of the list
	 * @param transactionId  the transaction ID
	 * @param gxsRsService   the service the transaction is bound to
	 */
	public void startOutgoingTransactionForGroupIdResponse(PeerConnection peerConnection, List<GxsSyncGroupItem> items, Instant update, int transactionId, GxsRsService<? extends GxsGroupItem, ? extends GxsMessageItem> gxsRsService)
	{
		var transaction = new Transaction<>(transactionId, EnumSet.of(START, TYPE_GROUP_LIST_RESPONSE), items, items.size(), gxsRsService, OUTGOING);
		startOutgoingTransaction(peerConnection, transaction, update);
	}

	/**
	 * Starts an outgoing transaction to respond with a list of gxs message IDs that we have and their update time.
	 *
	 * @param peerConnection the peer
	 * @param items          gxs message IDs
	 * @param update         the last update of the list
	 * @param transactionId  the transaction ID
	 * @param gxsRsService   the service the transaction is bound to
	 */
	public void startOutgoingTransactionForMessageIdResponse(PeerConnection peerConnection, List<GxsSyncMessageItem> items, Instant update, int transactionId, GxsRsService<? extends GxsGroupItem, ? extends GxsMessageItem> gxsRsService)
	{
		var transaction = new Transaction<>(transactionId, EnumSet.of(START, TYPE_MESSAGE_LIST_RESPONSE), items, items.size(), gxsRsService, OUTGOING);
		startOutgoingTransaction(peerConnection, transaction, update);
	}

	/**
	 * Starts an outgoing transaction to transfer gxs groups.
	 *
	 * @param peerConnection the peer
	 * @param items          gxs groups
	 * @param update         the last update of the groups
	 * @param transactionId  the transaction ID
	 * @param gxsRsService   the service the transaction is bound to
	 */
	public void startOutgoingTransactionForGroupTransfer(PeerConnection peerConnection, List<GxsTransferGroupItem> items, Instant update, int transactionId, GxsRsService<? extends GxsGroupItem, ? extends GxsMessageItem> gxsRsService)
	{
		var transaction = new Transaction<>(transactionId, EnumSet.of(START, TYPE_GROUPS), items, items.size(), gxsRsService, OUTGOING);
		startOutgoingTransaction(peerConnection, transaction, update);
	}

	/**
	 * Starts an outgoing transaction to transfer gxs messages.
	 *
	 * @param peerConnection the peer
	 * @param items          gxs messages
	 * @param update         the last update of the groups
	 * @param transactionId  the transaction ID
	 * @param gxsRsService   the service the transaction is bound to
	 */
	public void startOutgoingTransactionForMessageTransfer(PeerConnection peerConnection, List<GxsTransferMessageItem> items, Instant update, int transactionId, GxsRsService<? extends GxsGroupItem, ? extends GxsMessageItem> gxsRsService)
	{
		var transaction = new Transaction<>(transactionId, EnumSet.of(START, TYPE_MESSAGES), items, items.size(), gxsRsService, OUTGOING);
		startOutgoingTransaction(peerConnection, transaction, update);

	}

	/**
	 * Processes an incoming transactions (incoming, confirmation of outgoing, success confirmation).
	 *
	 * @param peerConnection the peer
	 * @param item           a transaction item (contains transaction type, timestamp and total number of items)
	 * @param gxsRsService   the service the transaction is bound to
	 */
	public void processIncomingTransaction(PeerConnection peerConnection, GxsTransactionItem item, GxsRsService<? extends GxsGroupItem, ? extends GxsMessageItem> gxsRsService)
	{
		if (item.getFlags().contains(START))
		{
			//  This is an incoming connection
			log.debug("Received INCOMING transaction {} from peer {}, sending back ACK", item, peerConnection);
			Set<TransactionFlags> transactionFlags = EnumSet.copyOf(item.getFlags());
			transactionFlags.retainAll(TransactionFlags.ofTypes());
			transactionFlags.add(START_ACKNOWLEDGE);

			var transaction = new Transaction<>(item.getTransactionId(), transactionFlags, new ArrayList<>(), item.getItemCount(), gxsRsService, INCOMING);
			transaction.setUpdated(Instant.ofEpochSecond(item.getUpdateTimestamp()));
			addTransaction(peerConnection, transaction, INCOMING);

			var readyTransactionItem = new GxsTransactionItem(
					transactionFlags,
					item.getTransactionId()
			);
			peerConnectionManager.writeItem(peerConnection, readyTransactionItem, transaction.getService());
			transaction.setState(State.RECEIVING);
		}
		else if (item.getFlags().contains(START_ACKNOWLEDGE))
		{
			// This is the confirmation by the peer of our outgoing connection
			log.debug("Confirmation of OUTGOING transaction {} from peer {}, sending items...", item, peerConnection);
			var transaction = getTransaction(peerConnection, item.getTransactionId(), OUTGOING);
			transaction.setState(State.SENDING);

			log.debug("{} items to go", transaction.getItems().size());
			transaction.getItems().forEach(gxsExchange -> peerConnectionManager.writeItem(peerConnection, gxsExchange, transaction.getService()));
			log.debug("done");

			transaction.setState(State.WAITING_CONFIRMATION);
		}
		else if (item.getFlags().contains(END_SUCCESS))
		{
			// The peer confirms success
			log.debug("Got END_SUCCESS transaction {} from peer {}, removing the transaction", item, peerConnection);
			var transaction = getTransaction(peerConnection, item.getTransactionId(), OUTGOING);
			transaction.setState(State.COMPLETED);
			removeTransaction(peerConnection, transaction);
		}
	}

	/**
	 * Adds an incoming item to an existing transaction.
	 *
	 * @param peerConnection the peer
	 * @param item           the item to add to the transaction
	 * @param gxsRsService   the service the transaction is bound to
	 */
	public void addIncomingItemToTransaction(PeerConnection peerConnection, GxsExchange item, GxsRsService<? extends GxsGroupItem, ? extends GxsMessageItem> gxsRsService)
	{
		log.debug("Adding transaction item: {}", item);
		var transaction = getTransaction(peerConnection, item.getTransactionId(), INCOMING);
		transaction.addItem(item);

		if (transaction.hasAllItems())
		{
			log.debug("Transaction successful, sending COMPLETED");
			transaction.setState(State.COMPLETED);
			var successTransactionItem = new GxsTransactionItem(
					EnumSet.of(END_SUCCESS),
					transaction.getId()
			);
			peerConnectionManager.writeItem(peerConnection, successTransactionItem, transaction.getService());

			gxsRsService.processItems(peerConnection, transaction); // XXX: how will processItems() know what the items are? should the transaction have something to know that? yes, the flag...

			removeTransaction(peerConnection, transaction);
			// XXX: in the case that interest us, GxsIdService would call requestGxsGroups()
		}
	}

	private void addTransaction(PeerConnection peerConnection, Transaction<?> transaction, Direction direction)
	{
		Map<LocationId, Map<Integer, Transaction<?>>> transactionList = switch (direction)
		{
			case OUTGOING -> outgoingTransactions;
			case INCOMING -> incomingTransactions;
		};

		var transactionMap = transactionList.computeIfAbsent(peerConnection.getLocation().getLocationId(), key -> new HashMap<>());
		if (transactionMap.put(transaction.getId(), transaction) != null && direction == OUTGOING)
		{
			throw new IllegalStateException("Transaction " + transaction.getId() + " (OUTGOING) for peer " + peerConnection + " already exists. Should not happen (tm)");
		}
	}

	private Transaction<?> getTransaction(PeerConnection peerConnection, int id, Direction direction)
	{
		var locationId = peerConnection.getLocation().getLocationId();

		var transactionMap = direction == INCOMING ? incomingTransactions.get(locationId) : outgoingTransactions.get(locationId);
		if (transactionMap == null)
		{
			throw new IllegalStateException("No existing transaction for peer " + peerConnection);
		}
		var transaction = transactionMap.get(id);
		if (transaction == null)
		{
			throw new IllegalStateException("No existing transaction for peer " + peerConnection);
		}
		if (transaction.hasTimeout())
		{
			throw new IllegalStateException("Transaction timed out for peer " + peerConnection);
		}
		return transaction;
	}

	private void removeTransaction(PeerConnection peerConnection, Transaction<?> transaction)
	{
		var locationId = peerConnection.getLocation().getLocationId();

		var transactionMap = transaction.getDirection() == INCOMING ? incomingTransactions.get(locationId) : outgoingTransactions.get(locationId);
		if (transactionMap == null)
		{
			throw new IllegalStateException("No existing transaction for removal for peer " + peerConnection);
		}
		if (!transactionMap.remove(transaction.getId(), transaction))
		{
			throw new IllegalStateException("No existing transaction for removal for peer " + peerConnection);
		}
		// XXX: remove, and possible check if the state is right before doing so (ie. COMPLETED, etc...)
	}

	private void startOutgoingTransaction(PeerConnection peerConnection, Transaction<? extends GxsExchange> transaction, Instant update)
	{
		log.debug("Starting outgoing transaction {} with peer {}", transaction, peerConnection);
		addTransaction(peerConnection, transaction, OUTGOING);

		var startTransactionItem = new GxsTransactionItem(
				transaction.getTransactionFlags(),
				transaction.getItems().size(),
				(int) update.getEpochSecond(),
				transaction.getId());

		peerConnectionManager.writeItem(peerConnection, startTransactionItem, transaction.getService());

		// XXX: periodically check for the timeout in case the peer doesn't answer anymore
	}
}
