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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.GxsExchangeService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxs.item.*;
import io.xeres.common.id.GxsId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public abstract class GxsRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(GxsRsService.class);

	private static final int KEY_TRANSACTION_ID = 1;

	/**
	 * When to perform synchronization run with a peer.
	 */
	private static final Duration SYNCHRONIZATION_DELAY_INITIAL_MIN = Duration.ofSeconds(10);
	private static final Duration SYNCHRONIZATION_DELAY_INITIAL_MAX = Duration.ofSeconds(15);
	private static final Duration SYNCHRONIZATION_DELAY = Duration.ofMinutes(1);

	private final GxsExchangeService gxsExchangeService;
	protected final GxsTransactionManager gxsTransactionManager;
	private final PeerConnectionManager peerConnectionManager;

	protected GxsRsService(Environment environment, PeerConnectionManager peerConnectionManager, GxsExchangeService gxsExchangeService, GxsTransactionManager gxsTransactionManager)
	{
		super(environment);
		this.gxsExchangeService = gxsExchangeService;
		this.gxsTransactionManager = gxsTransactionManager;
		this.peerConnectionManager = peerConnectionManager;
	}

	/**
	 * Gets the list of Gxs groups to transfer.
	 *
	 * @param recipient the recipient of the groups
	 * @param since     the time after which the groups are relevant
	 * @return the pending groups
	 */
	public abstract List<? extends GxsGroupItem> getPendingGroups(PeerConnection recipient, Instant since);

	protected abstract List<? extends GxsGroupItem> onGroupListRequest(Set<GxsId> ids);

	protected abstract Set<GxsId> onGroupListResponse(Map<GxsId, Instant> ids);

	protected abstract void onGroupReceived(GxsTransferGroupItem item);

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
				ThreadLocalRandom.current().nextLong(SYNCHRONIZATION_DELAY_INITIAL_MIN.toSeconds(), SYNCHRONIZATION_DELAY_INITIAL_MAX.toSeconds() + 1),
				SYNCHRONIZATION_DELAY.toSeconds(),
				TimeUnit.SECONDS
		);
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
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
		log.debug("Asking peer {} for last local sync {} for service {}", peerConnection, gxsSyncGroupRequestItem.getUpdateTimestamp(), getServiceType());
		peerConnectionManager.writeItem(peerConnection, gxsSyncGroupRequestItem, this);
	}

	// XXX: maybe have some Gxs dedicated methods...

	private void handleGxsSyncGroupRequestItem(PeerConnection peerConnection, GxsSyncGroupRequestItem item)
	{
		log.debug("Got sync request item {} from peer {}", item, peerConnection);

		var transactionId = getTransactionId(peerConnection);
		var since = Instant.ofEpochSecond(item.getUpdateTimestamp());
		if (areGxsUpdatesAvailableForPeer(since))
		{
			log.debug("Updates available for peer, sending...");
			List<GxsSyncGroupItem> items = new ArrayList<>();

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
			gxsTransactionManager.startOutgoingTransactionForGroupIdResponse(
					peerConnection,
					items,
					gxsExchangeService.getLastServiceUpdate(getServiceType()), // XXX: mGrpServerUpdate.grpUpdateTS... I think it's that but recheck
					transactionId,
					this
			);
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
			gxsTransactionManager.processIncomingTransaction(peerConnection, gxsTransactionItem, this);
		}
		else
		{
			if (gxsTransactionManager.addIncomingItemToTransaction(peerConnection, item, this))
			{
				gxsExchangeService.setLastPeerUpdate(peerConnection.getLocation(), getServiceType(), Instant.now());
			}
		}
	}

	protected int getTransactionId(PeerConnection peerConnection)
	{
		var transactionId = (int) peerConnection.getServiceData(this, KEY_TRANSACTION_ID).orElse(1);
		peerConnection.putServiceData(this, KEY_TRANSACTION_ID, ++transactionId);
		return transactionId;
	}

	private boolean areGxsUpdatesAvailableForPeer(Instant lastPeerUpdate)
	{
		log.debug("Comparing stored peer's last update: {} to peer's advertised last update: {}", gxsExchangeService.getLastServiceUpdate(getServiceType()), lastPeerUpdate);
		// XXX: there should be a way to detect if the peer is sending a lastPeerUpdate several times (means the transaction isn't complete yet)
		return lastPeerUpdate.isBefore(gxsExchangeService.getLastServiceUpdate(getServiceType()));
	}

	private boolean isGxsAllowedForPeer(PeerConnection peerConnection, GxsGroupItem item)
	{
		return true; // XXX: later one we should compare with the circles (though that can be done on the SQL request too)
	}

	/**
	 * Processes the transaction.
	 *
	 * @param peerConnection the peer connection who sent the items
	 * @param transaction    the transaction to process
	 */
	public void processItems(PeerConnection peerConnection, Transaction<?> transaction)
	{
		if (isEmpty(transaction.getItems()))
		{
			throw new IllegalArgumentException("Empty transaction items");
		}

		if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_GROUP_LIST_REQUEST))
		{
			@SuppressWarnings("unchecked")
			var gxsIds = ((List<GxsSyncGroupItem>) transaction.getItems()).stream().map(GxsSyncGroupItem::getGroupId).collect(toSet());
			log.debug("Peer wants the following gxs ids (total: {}): {} ...", gxsIds.size(), gxsIds.stream().limit(10).toList());
			sendGxsGroups(peerConnection, onGroupListRequest(gxsIds));
		}
		else if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_GROUP_LIST_RESPONSE))
		{
			@SuppressWarnings("unchecked")
			var gxsIdsMap = ((List<GxsSyncGroupItem>) transaction.getItems()).stream()
					.collect(toMap(GxsSyncGroupItem::getGroupId, gxsSyncGroupItem -> Instant.ofEpochSecond(gxsSyncGroupItem.getPublishTimestamp())));
			log.debug("Peer has the following gxsIds (new or updates) for us (total: {}): {} ...", gxsIdsMap.keySet().size(), gxsIdsMap.keySet().stream().limit(10).toList());
			requestGxsGroups(peerConnection, onGroupListResponse(gxsIdsMap));
		}
		else if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_GROUPS))
		{
			@SuppressWarnings("unchecked")
			var transferItems = (List<GxsTransferGroupItem>) transaction.getItems();
			transferItems.forEach(this::onGroupReceived);
			gxsExchangeService.setLastServiceUpdate(getServiceType(), Instant.now());
		}
	}

	private void sendGxsGroups(PeerConnection peerConnection, List<? extends GxsGroupItem> gxsGroupItems)
	{
		var transactionId = getTransactionId(peerConnection);
		List<GxsTransferGroupItem> items = new ArrayList<>();
		gxsGroupItems.forEach(gxsGroupItem -> {
			signGroupIfNeeded(gxsGroupItem);
			var groupBuf = Unpooled.buffer(); // XXX: size... well, it autogrows
			// Write that damn header
			var groupSize = 0;
			groupSize += Serializer.serialize(groupBuf, (byte) 2);
			groupSize += Serializer.serialize(groupBuf, (short) gxsGroupItem.getServiceType().getType());
			groupSize += Serializer.serialize(groupBuf, (byte) 2);
			var sizeOffset = groupBuf.writerIndex();
			groupSize += Serializer.serialize(groupBuf, 0); // write size at end

			groupSize += gxsGroupItem.writeGroupObject(groupBuf, EnumSet.noneOf(SerializationFlags.class));
			groupBuf.setInt(sizeOffset, groupSize); // write group size

			var metaBuf = Unpooled.buffer(); // XXX: size... autogrows as well
			gxsGroupItem.writeMetaObject(metaBuf, EnumSet.noneOf(SerializationFlags.class));
			var gxsTransferGroupItem = new GxsTransferGroupItem(gxsGroupItem.getGxsId(), getArray(groupBuf), getArray(metaBuf), transactionId, this);
			groupBuf.release();
			metaBuf.release();
			items.add(gxsTransferGroupItem);
		});

		if (isNotEmpty(items))
		{
			gxsTransactionManager.startOutgoingTransactionForGroupTransfer(
					peerConnection,
					items,
					Instant.now(), // XXX: not sure about that one... recheck. I think it has to be when our group last changed
					transactionId,
					this
			);
		}
	}

	public void requestGxsGroups(PeerConnection peerConnection, Collection<GxsId> ids) // XXX: maybe use a future to know when the group arrived? it's possible by keeping a list of transactionIds then answering once the answer comes back
	{
		if (isEmpty(ids))
		{
			return;
		}
		var transactionId = getTransactionId(peerConnection);
		List<GxsSyncGroupItem> items = new ArrayList<>();

		ids.forEach(gxsId -> items.add(new GxsSyncGroupItem(EnumSet.of(SyncFlags.REQUEST), gxsId, transactionId)));

		gxsTransactionManager.startOutgoingTransactionForGroupIdRequest(peerConnection, items, transactionId, this);
	}

	private static byte[] getArray(ByteBuf buf)
	{
		var out = new byte[buf.writerIndex()];
		buf.readBytes(out);
		return out;
	}

	// XXX: GXS messages will need publish/identity support here, not just admin
	private static void signGroupIfNeeded(GxsGroupItem gxsGroupItem)
	{
		if (gxsGroupItem.getAdminPrivateKey() != null)
		{
			var data = serializeItemForSignature(gxsGroupItem);
			var signature = RSA.sign(data, gxsGroupItem.getAdminPrivateKey());
			gxsGroupItem.setSignature(signature);
		}
	}

	private static byte[] serializeItemForSignature(GxsGroupItem gxsGroupItem)
	{
		gxsGroupItem.setSerialization(Unpooled.buffer().alloc(), 2, gxsGroupItem.getServiceType(), 1); // XXX: not very nice to have those arguments hardcoded here
		var buf = gxsGroupItem.serializeItem(EnumSet.of(SerializationFlags.SIGNATURE)).getBuffer();
		var data = new byte[buf.writerIndex()];
		buf.getBytes(0, data);
		buf.release();
		return data;
	}
}
