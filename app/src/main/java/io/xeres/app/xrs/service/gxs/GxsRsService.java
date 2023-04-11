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
import io.xeres.app.database.model.gxs.GxsMessageItem;
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
import io.xeres.common.id.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static io.xeres.app.xrs.service.gxs.item.GxsSyncGroupItem.REQUEST;
import static io.xeres.app.xrs.service.gxs.item.GxsSyncGroupItem.RESPONSE;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/**
 * This abstract class is used by all Gxs services. The transfer system goes the following way, for example
 * if Juergen asks Heike every minute if she has new groups for him:
 * <p>
 * <img src="doc-files/transfer.png">
 *
 * @param <G> the GxsGroupItem subclass
 * @param <M> the GxsMessageItem subclass
 */
public abstract class GxsRsService<G extends GxsGroupItem, M extends GxsMessageItem> extends RsService
{
	protected final Logger log = LoggerFactory.getLogger(getClass().getName());

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

	private final Type itemClass;

	protected GxsRsService(Environment environment, PeerConnectionManager peerConnectionManager, GxsExchangeService gxsExchangeService, GxsTransactionManager gxsTransactionManager)
	{
		super(environment);
		this.gxsExchangeService = gxsExchangeService;
		this.gxsTransactionManager = gxsTransactionManager;
		this.peerConnectionManager = peerConnectionManager;

		// Type information is available when subclassing a class using a generic type, which means itemClass is the class of T
		itemClass = ((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	/**
	 * Called when the peer wants a list of new or updated groups that we have for him.
	 * @param recipient the recipient of the result
	 * @param since     the time after which the groups are relevant. Everything before is ignored
	 * @return the available groups that we have
	 */
	protected abstract List<G> onAvailableGroupListRequest(PeerConnection recipient, Instant since);

	/**
	 * Called when a peer sends the list of new or updated groups that might interest us.
	 * @param ids the ids of updated groups and their update time that the peer has for us
	 * @return the subset of those groups that we actually want
	 */
	protected abstract Set<GxsId> onAvailableGroupListResponse(Map<GxsId, Instant> ids);

	/**
	 * Called when the peer wants specific groups to be transferred to him.
	 * @param ids the groups that the peer wants
	 * @return the groups that we have available within the requested set
	 */
	protected abstract List<G> onGroupListRequest(Set<GxsId> ids);

	/**
	 * Called when a group has been received.
	 * @param item the received group
	 */
	protected abstract void onGroupReceived(G item);

	/**
	 * Called when the peer wants a list of new messages within a group that we have for him.
	 * @param recipient the recipient of the result
	 * @param groupId the group ID
	 * @param since the time after which the messages are relevant. Everything before is ignored
	 * @return the available messages that we have
	 */
	protected abstract List<M> onPendingMessageListRequest(PeerConnection recipient, GxsId groupId, Instant since);

	/**
	 * Called when a peer sends the list of new messages that might interest us, within a group.
	 * @param groupId the group ID
	 * @param messageIds the ids of new messages
	 * @return the subset of those messages that we actually want
	 */
	protected abstract List<M> onMessageListRequest(GxsId groupId, Set<MessageId> messageIds);

	/**
	 * Called when the peer wants specific messages to be transferred to him, within a group.
	 * @param groupId the group ID
	 * @param messageIds the ids of messages that the peer wants
	 * @return the messages that we have available within the requested set
	 */
	protected abstract List<MessageId> onMessageListResponse(GxsId groupId, Set<MessageId> messageIds);

	/**
	 * Called when a message has been received.
	 * @param item the received message
	 */
	protected abstract void onMessageReceived(M item);

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
				GxsSyncMessageItem.class, 8,
				GxsSyncMessageRequestItem.class, 16,
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
			else if (item instanceof GxsSyncMessageRequestItem gxsSyncMessageRequestItem)
			{
				handleGxsSyncMessageRequestItem(sender, gxsSyncMessageRequestItem);
			}
			else if (item instanceof GxsSyncGroupStatsItem gxsSyncGroupStatsItem)
			{
				log.debug("Would handle group statistics item (not implemented yet)");
				// XXX:
			}
		}
		else
		{
			log.error("Not a GxsExchange item: {}, ignoring", item);
		}
	}

	private void sync(PeerConnection peerConnection)
	{
		var gxsSyncGroupRequestItem = new GxsSyncGroupRequestItem(gxsExchangeService.getLastPeerGroupsUpdate(peerConnection.getLocation(), getServiceType())); // TODO: use the RTT's (RTT service) delta time value to compute the proper time on the remote's end
		log.debug("Asking peer {} for last local sync {} for service {}", peerConnection, gxsSyncGroupRequestItem.getLastUpdated(), getServiceType());
		peerConnectionManager.writeItem(peerConnection, gxsSyncGroupRequestItem, this);
	}

	// XXX: maybe have some Gxs dedicated methods...

	private void handleGxsSyncGroupRequestItem(PeerConnection peerConnection, GxsSyncGroupRequestItem item)
	{
		log.debug("Got group sync request item {} from peer {}", item, peerConnection);

		var transactionId = getTransactionId(peerConnection);
		var since = Instant.ofEpochSecond(item.getLastUpdated());
		if (areGxsUpdatesAvailableForPeer(since))
		{
			log.debug("Updates available for peer, sending...");
			List<GxsSyncGroupItem> items = new ArrayList<>();

			onAvailableGroupListRequest(peerConnection, since).forEach(gxsGroupItem -> {
				log.debug("Adding groupId of item: {}", gxsGroupItem);
				if (isGxsAllowedForPeer(peerConnection, gxsGroupItem))
				{
					var gxsSyncGroupItem = new GxsSyncGroupItem(
							RESPONSE,
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
					gxsExchangeService.getLastServiceGroupsUpdate(getServiceType()), // XXX: mGrpServerUpdate.grpUpdateTS... I think it's that but recheck
					transactionId,
					this
			);
		}
		else
		{
			log.debug("No update available for peer");
		}

		// XXX: check if the peer is subscribed, encrypt or not the group, etc... it's rsgxsnetservice.cc/handleRecvSyncGroup we might not need that for gxsid transferts

		// XXX: to handle the synchronization we must know which tables to use, then it's generic
	}

	private void handleGxsSyncMessageRequestItem(PeerConnection peerConnection, GxsSyncMessageRequestItem item)
	{
		log.debug("Got message sync request item {} from peer {}", item, peerConnection);

		var transactionId = getTransactionId(peerConnection);
		var lastUpdated = Instant.ofEpochSecond(item.getLastUpdated());
		var since = Instant.ofEpochSecond(item.getCreateSince());
		if (areMessageUpdatesAvailableForPeer(item.getGroupId(), lastUpdated, since))
		{
			log.debug("Messages available for peer, sending...");
			List<GxsSyncMessageItem> items = new ArrayList<>();

			onPendingMessageListRequest(peerConnection, item.getGroupId(), since).forEach(gxsMessageItem -> {
				var gxsSyncMessageItem = new GxsSyncMessageItem(
						GxsSyncMessageItem.RESPONSE,
						gxsMessageItem,
						transactionId);

				items.add(gxsSyncMessageItem);
			});

			log.debug("Calling transaction, number of items: {}", items.size());
			gxsTransactionManager.startOutgoingTransactionForMessageIdResponse(
					peerConnection,
					items,
					gxsExchangeService.getLastServiceGroupsUpdate(getServiceType()), // XXX: not sure that's correct
					transactionId,
					this
			);
		}
		else
		{
			log.debug("No messages available for peer");
		}

		// XXX: maybe some more to do, check rsgxsnetservice.cc/handleRecvSyncMsg
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
				gxsExchangeService.setLastPeerGroupsUpdate(peerConnection.getLocation(), getServiceType());
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
		var lastServiceUpdate = gxsExchangeService.getLastServiceGroupsUpdate(getServiceType());
		log.debug("Comparing stored peer's last update: {} to peer's advertised last update: {}", lastServiceUpdate, lastPeerUpdate);
		// XXX: there should be a way to detect if the peer is sending a lastPeerUpdate several times (means the transaction isn't complete yet)
		return lastPeerUpdate.isBefore(lastServiceUpdate);
	}

	private boolean areMessageUpdatesAvailableForPeer(GxsId groupId, Instant lastPeerUpdate, Instant since)
	{
		var groupList = onGroupListRequest(Set.of(groupId));
		if (groupList.isEmpty())
		{
			log.warn("Peer requested unavailable group {}", groupId);
			return false;
		}

		var group = groupList.get(0);
		return group.isSubscribed() &&
				group.getLastPosted() != null &&
				lastPeerUpdate.isBefore(group.getLastPosted()) &&
				group.getLastPosted().isAfter(since);
	}

	private boolean isGxsAllowedForPeer(PeerConnection peerConnection, G item)
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
			return; // nothing to do
		}

		if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_GROUP_LIST_RESPONSE))
		{
			@SuppressWarnings("unchecked")
			var gxsIdsMap = ((List<GxsSyncGroupItem>) transaction.getItems()).stream()
					.collect(toMap(GxsSyncGroupItem::getGroupId, gxsSyncGroupItem -> Instant.ofEpochSecond(gxsSyncGroupItem.getPublishTimestamp())));
			log.debug("Peer has the following gxsIds (new or updates) for us (total: {}): {} ...", gxsIdsMap.keySet().size(), gxsIdsMap.keySet().stream().limit(10).toList());
			requestGxsGroups(peerConnection, onAvailableGroupListResponse(gxsIdsMap));
		}
		else if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_GROUP_LIST_REQUEST))
		{
			@SuppressWarnings("unchecked")
			var gxsIds = ((List<GxsSyncGroupItem>) transaction.getItems()).stream()
					.map(GxsSyncGroupItem::getGroupId).collect(toSet());
			log.debug("Peer wants the following gxs ids (total: {}): {} ...", gxsIds.size(), gxsIds.stream().limit(10).toList());
			sendGxsGroups(peerConnection, onGroupListRequest(gxsIds));
		}
		else if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_GROUPS))
		{
			@SuppressWarnings("unchecked")
			var transferItems = (List<GxsTransferGroupItem>) transaction.getItems();
			transferItems.forEach(gxsTransferGroupItem -> onGroupReceived(convertTransferGroupToGxsGroup(gxsTransferGroupItem)));
			gxsExchangeService.setLastServiceGroupsUpdateNow(getServiceType());
		}
		else if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_MESSAGE_LIST_RESPONSE))
		{
			@SuppressWarnings("unchecked")
			var groupId = ((List<GxsSyncMessageItem>) transaction.getItems()).stream()
					.map(GxsSyncMessageItem::getGroupId).findFirst().orElse(null);
			@SuppressWarnings("unchecked")
			var messageIds = ((List<GxsSyncMessageItem>) transaction.getItems()).stream()
					.map(GxsSyncMessageItem::getMessageId).collect(toSet());
			log.debug("Peer has the following messageIds for groupId {} (new) for us (total: {}): {} ...", groupId, messageIds.size(), messageIds.stream().limit(10).toList());
			requestGxsMessages(peerConnection, groupId, onMessageListResponse(groupId, messageIds));
		}
		else if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_MESSAGE_LIST_REQUEST))
		{
			@SuppressWarnings("unchecked")
			var groupId = ((List<GxsSyncMessageItem>) transaction.getItems()).stream()
					.map(GxsSyncMessageItem::getGroupId).findFirst().orElse(null);
			@SuppressWarnings("unchecked")
			var messageIds = ((List<GxsSyncMessageItem>) transaction.getItems()).stream()
					.map(GxsSyncMessageItem::getMessageId).collect(toSet());
			log.debug("Peer wants the following messageIds for groupId {} (total: {}): {} ...", groupId, messageIds.size(), messageIds.stream().limit(10).toList());
			sendGxsMessages(peerConnection, onMessageListRequest(groupId, messageIds));
		}
		else if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_MESSAGES))
		{
			@SuppressWarnings("unchecked")
			var transferItems = (List<GxsTransferMessageItem>) transaction.getItems();
			transferItems.forEach(gxsTransferMessageItem -> onMessageReceived(convertTransferGroupToGxsMessage(gxsTransferMessageItem)));
			//gxsExchangeService.setLastServiceGroupsUpdateNow(getServiceType()); XXX: should that be done? I'd say no but RS has some comment in the source about it
		}
	}

	private G convertTransferGroupToGxsGroup(GxsTransferGroupItem fromItem)
	{
		G toItem;

		try
		{
			//noinspection unchecked
			toItem = ((Class<G>)itemClass).getDeclaredConstructor().newInstance();
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
		{
			throw new IllegalArgumentException("Failed to instantiate " + ((Class<?>) itemClass).getSimpleName() + " missing empty constructor?");
		}

		var buf = Unpooled.copiedBuffer(fromItem.getMeta(), fromItem.getGroup()); //XXX: use ctx().alloc()?
		Serializer.deserializeGxsMetaAndDataItem(buf, toItem);
		buf.release();

		return toItem;
	}

	private void sendGxsGroups(PeerConnection peerConnection, List<G> gxsGroupItems)
	{
		var transactionId = getTransactionId(peerConnection);
		List<GxsTransferGroupItem> items = new ArrayList<>();
		gxsGroupItems.forEach(gxsGroupItem -> {
			signGroupIfNeeded(gxsGroupItem);
			var groupBuf = Unpooled.buffer();
			// Write that damn header
			var groupSize = 0;
			groupSize += Serializer.serialize(groupBuf, (byte) 2);
			groupSize += Serializer.serialize(groupBuf, (short) gxsGroupItem.getService().getServiceType().getType());
			groupSize += Serializer.serialize(groupBuf, (byte) 2);
			var sizeOffset = groupBuf.writerIndex();
			groupSize += Serializer.serialize(groupBuf, 0); // write size at end

			groupSize += gxsGroupItem.writeDataObject(groupBuf, EnumSet.noneOf(SerializationFlags.class));
			groupBuf.setInt(sizeOffset, groupSize); // write group size

			var metaBuf = Unpooled.buffer();
			gxsGroupItem.writeMetaObject(metaBuf, EnumSet.noneOf(SerializationFlags.class));
			var gxsTransferGroupItem = new GxsTransferGroupItem(gxsGroupItem.getGxsId(), getArray(groupBuf), getArray(metaBuf), transactionId, this);
			groupBuf.release();
			metaBuf.release();
			items.add(gxsTransferGroupItem);
		});

		gxsTransactionManager.startOutgoingTransactionForGroupTransfer(
				peerConnection,
				items,
				Instant.now(), // XXX: not sure about that one... recheck. I think it has to be when our group last changed
				transactionId,
				this
		);
	}

	public void requestGxsGroups(PeerConnection peerConnection, Collection<GxsId> ids) // XXX: maybe use a future to know when the group arrived? it's possible by keeping a list of transactionIds then answering once the answer comes back
	{
		if (isEmpty(ids))
		{
			return;
		}
		var transactionId = getTransactionId(peerConnection);
		List<GxsSyncGroupItem> items = new ArrayList<>();

		ids.forEach(gxsId -> items.add(new GxsSyncGroupItem(REQUEST, gxsId, transactionId)));

		gxsTransactionManager.startOutgoingTransactionForGroupIdRequest(peerConnection, items, transactionId, this);
	}

	public void sendGxsMessages(PeerConnection peerConnection, List<M> gxsMessageItems)
	{
		var transactionId = getTransactionId(peerConnection);
		List<GxsTransferMessageItem> items = new ArrayList<>();
		gxsMessageItems.forEach(gxsMessageItem -> {
			// XXX: sign the message. add the signature stuff to GxsMessageItem
			var messageBuf = Unpooled.buffer();
			// Write that damn header
			var messageSize = 0;
			messageSize += Serializer.serialize(messageBuf, (byte) 2);
			messageSize += Serializer.serialize(messageBuf, (short) gxsMessageItem.getService().getServiceType().getType());
			messageSize += Serializer.serialize(messageBuf, (byte) 2);
			var sizeOffset = messageBuf.writerIndex();
			messageSize += Serializer.serialize(messageBuf, 0); // write size at end

			messageSize += gxsMessageItem.writeDataObject(messageBuf, EnumSet.noneOf(SerializationFlags.class));
			messageBuf.setInt(sizeOffset, messageSize); // write message size

			var metaBuf = Unpooled.buffer();
			gxsMessageItem.writeMetaObject(metaBuf, EnumSet.noneOf(SerializationFlags.class));
			var gxsTransferMessageItem = new GxsTransferMessageItem(gxsMessageItem.getGxsId(), gxsMessageItem.getMessageId(), getArray(messageBuf), getArray(metaBuf), transactionId, this);
			messageBuf.release();
			metaBuf.release();
			items.add(gxsTransferMessageItem);
		});

		gxsTransactionManager.startOutgoingTransactionForMessageTransfer(
				peerConnection,
				items,
				Instant.now(), // XXX: not sure, see group transfer
				transactionId,
				this
		);
	}

	public void requestGxsMessages(PeerConnection peerConnection, GxsId groupId, Collection<MessageId> messageIds)
	{
		if (isEmpty(messageIds))
		{
			return;
		}
		var transactionId = getTransactionId(peerConnection);
		List<GxsSyncMessageItem> items = new ArrayList<>();

		messageIds.forEach(messageId -> items.add(new GxsSyncMessageItem(GxsSyncMessageItem.REQUEST, groupId, messageId, transactionId)));

		gxsTransactionManager.startOutgoingTransactionForMessageIdRequest(peerConnection, items, transactionId, this);
	}

	private M convertTransferGroupToGxsMessage(GxsTransferMessageItem fromItem)
	{
		M toItem;

		try
		{
			//noinspection unchecked
			toItem = ((Class<M>)itemClass).getDeclaredConstructor().newInstance();
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
		{
			throw new IllegalArgumentException("Failed to instantiate " + ((Class<?>) itemClass).getSimpleName() + " missing empty constructor?");
		}

		var buf = Unpooled.copiedBuffer(fromItem.getMeta(), fromItem.getMessage()); //XXX: use ctx().alloc()?
		Serializer.deserializeGxsMetaAndDataItem(buf, toItem);
		buf.release();

		return toItem;
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
		gxsGroupItem.setSerialization(Unpooled.buffer().alloc(), 2, gxsGroupItem.getService().getServiceType(), 1); // XXX: not very nice to have those arguments hardcoded here
		var buf = gxsGroupItem.serializeItem(EnumSet.of(SerializationFlags.SIGNATURE)).getBuffer();
		var data = new byte[buf.writerIndex()];
		buf.getBytes(0, data);
		buf.release();
		return data;
	}
}
