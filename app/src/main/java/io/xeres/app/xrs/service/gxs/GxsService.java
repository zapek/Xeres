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

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.GxsExchangeService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxs.item.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class GxsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(GxsService.class);

	private static final int KEY_TRANSACTION_ID = 1;

	/**
	 * When to perform synchronization run with a peer.
	 */
	private static final Duration SYNCHRONIZATION_DELAY = Duration.ofMinutes(1);

	private final GxsExchangeService gxsExchangeService;
	protected final GxsTransactionManager gxsTransactionManager;
	private final DatabaseSessionManager databaseSessionManager;

	protected GxsService(Environment environment, PeerConnectionManager peerConnectionManager, GxsExchangeService gxsExchangeService, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager)
	{
		super(environment, peerConnectionManager);
		this.gxsExchangeService = gxsExchangeService;
		this.gxsTransactionManager = gxsTransactionManager;
		this.databaseSessionManager = databaseSessionManager;
	}

	/**
	 * Gets the Gxs implementation of the group.
	 *
	 * @return the subclass of the GxsGroupItem
	 */
	public abstract Class<? extends GxsGroupItem> getGroupClass();

	/**
	 * Gets the Gxs implementation of the message.
	 *
	 * @return the subclass of the GxsMessageItem
	 */
	public abstract Class<? extends GxsMessageItem> getMessageClass();

	/**
	 * Gets the list of Gxs groups to transfer.
	 *
	 * @param recipient the recipient of the groups
	 * @param since     the time after which the groups are relevant
	 * @return the pending groups
	 */
	public abstract List<? extends GxsGroupItem> getPendingGroups(PeerConnection recipient, Instant since);

	/**
	 * Processes the items of the transaction.
	 *
	 * @param peerConnection the peer connection who sent the items
	 * @param items the items to process
	 */
	public abstract void processItems(PeerConnection peerConnection, List<? extends GxsExchange> items);

	@Override
	public RsServiceType getServiceType()
	{
		throw new IllegalStateException("Must override getServiceType()");
	}

	@Override
	public Map<Class<? extends Item>, Integer> getSupportedItems()
	{
		return Map.of(
				GxsSyncGroupRequestItem.class, 1,
				GxsSyncGroupItem.class, 2,
				GxsSyncGroupStatsItem.class, 3,
				GxsTransferGroupItem.class, 4,
				GxsTransactionItem.class, 64
		);
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.LOW;
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		peerConnection.scheduleWithFixedDelay(
				() -> sync(peerConnection),
				SYNCHRONIZATION_DELAY.toSeconds(), // XXX: add some randomness to avoid global peer sync? maybe also for chatservice
				SYNCHRONIZATION_DELAY.toSeconds(),
				TimeUnit.SECONDS
		);
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		log.debug("Got item: {}", item);
		if (item instanceof GxsExchange gxsExchangeItem)
		{
			if (gxsExchangeItem.getTransactionId() != 0)
			{
				handleTransaction(sender, gxsExchangeItem);
			}
			else if (item instanceof GxsSyncGroupRequestItem gxsSyncGroupRequestItem)
			{
				handleGxsSyncGroupRequestItem(sender, gxsSyncGroupRequestItem);
			}
		}
		else
		{
			log.error("Not a GxsExchange item: {}, ignoring", item);
		}
	}

	private void sync(PeerConnection peerConnection)
	{
		var gxsSyncGroupRequestItem = new GxsSyncGroupRequestItem(gxsExchangeService.getLastPeerUpdate(peerConnection.getLocation(), getServiceType()));
		log.debug("Asking peer {} for last local sync {} for service {}", peerConnection, gxsExchangeService.getLastPeerUpdate(peerConnection.getLocation(), getServiceType()), getServiceType());
		writeItem(peerConnection, gxsSyncGroupRequestItem);
	}

	// XXX: maybe have some Gxs dedicated methods...

	private void handleGxsSyncGroupRequestItem(PeerConnection peerConnection, GxsSyncGroupRequestItem item)
	{
		log.debug("Got sync request item: {} from peer {}", item, peerConnection);

		int transactionId = getTransactionId(peerConnection);
		Instant since = Instant.ofEpochSecond(item.getUpdateTimestamp());
		if (areGxsUpdatesAvailableForPeer(since))
		{
			try (var session = new DatabaseSession(databaseSessionManager))
			{
				log.debug("Updates available for peer, sending...");
				List<GxsExchange> items = new ArrayList<>();

				// XXX: check if the group is subscribed (subscribeFlags & SUBSCRIBED)... what to do with gxsid? seems subscribe to all groups?
				getPendingGroups(peerConnection, since).forEach(gxsGroupItem -> {
					log.debug("Adding groupId of item: {}", gxsGroupItem);
					if (isGxsAllowedForPeer(peerConnection, gxsGroupItem))
					{
						var gxsSyncGroupItem = new GxsSyncGroupItem(
								EnumSet.of(SyncFlags.RESPONSE),
								gxsGroupItem,
								transactionId);

						items.add(gxsSyncGroupItem);
					}
				});
				// the items are included in a transaction (they all have the same transaction number)

				log.debug("Calling transaction, number of items: {}", items.size());
				gxsTransactionManager.startOutgoingTransaction(
						peerConnection,
						items,
						gxsExchangeService.getLastServiceUpdate(getServiceType()), // XXX: mGrpServerUpdate.grpUpdateTS... I think it's that but recheck
						transactionId,
						this
				);
			}
		}
		else
		{
			log.debug("No update available for peer"); // XXX: remove...
		}

		// XXX: check if the peer is subscribed, encrypt or not the group, etc... it's rsgxsnetservice.cc/handleRecvSyncGroup we might not need that for gxsid transferts

		// XXX: to handle the synchronization we must know which tables to use, then it's generic
	}

	private void handleTransaction(PeerConnection peerConnection, GxsExchange item)
	{
		if (item instanceof GxsTransactionItem gxsTransactionItem)
		{
			gxsTransactionManager.processTransaction(peerConnection, gxsTransactionItem, this);
		}
		else
		{
			if (gxsTransactionManager.addContent(peerConnection, item, this))
			{
				gxsExchangeService.setLastPeerUpdate(peerConnection.getLocation(), getServiceType(), Instant.now());
			}
		}
	}

	protected int getTransactionId(PeerConnection peerConnection)
	{
		int transactionId = (int) peerConnection.getServiceData(this, KEY_TRANSACTION_ID).orElse(1);
		peerConnection.putServiceData(this, KEY_TRANSACTION_ID, ++transactionId);
		return transactionId;
	}

	private boolean areGxsUpdatesAvailableForPeer(Instant lastPeerUpdate)
	{
		log.debug("Comparing our last update: {} to peer's last update: {}", gxsExchangeService.getLastServiceUpdate(getServiceType()), lastPeerUpdate);
		// XXX: there should be a way to detect if the peer is sending a lastPeerUpdate several times (means the transaction isn't complete yet)
		return lastPeerUpdate.isBefore(gxsExchangeService.getLastServiceUpdate(getServiceType()));
	}

	private boolean isGxsAllowedForPeer(PeerConnection peerConnection, GxsGroupItem item)
	{
		return true; // XXX: later one we should compare with the circles (though that can be done on the SQL request too)
	}
}
