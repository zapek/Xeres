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

import io.netty.buffer.Unpooled;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.model.gxs.GxsClientUpdate;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.database.model.gxs.GxsServiceSetting;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.repository.GxsClientUpdateRepository;
import io.xeres.app.database.repository.GxsGroupItemRepository;
import io.xeres.app.database.repository.GxsServiceSettingRepository;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxs.item.*;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import io.xeres.common.util.NoSuppressedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

import static io.xeres.app.net.peer.packet.Packet.HEADER_SIZE;
import static io.xeres.app.xrs.service.gxs.item.GxsSyncGroupItem.REQUEST;
import static io.xeres.app.xrs.service.gxs.item.GxsSyncGroupItem.RESPONSE;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/**
 * This abstract class is used by all Gxs services. The transfer system goes the following way, for example
 * if Juergen asks Heike every minute if she has new groups for him:
 * <p>
 * <img src="doc-files/transfer.png" alt="Transfer diagram">
 *
 * @param <G> the GxsGroupItem subclass
 * @param <M> the GxsMessageItem subclass
 */
public abstract class GxsRsService<G extends GxsGroupItem, M extends GxsMessageItem> extends RsService
{
	protected final Logger log = LoggerFactory.getLogger(getClass().getName());

	private static final int GXS_KEY_SIZE = 2048; // The RSA size of Gxs keys. Do not change unless you want everything to break.
	private static final int KEY_TRANSACTION_ID = 1;

	/**
	 * When to perform synchronization run with a peer.
	 */
	private static final Duration SYNCHRONIZATION_DELAY_INITIAL_MIN = Duration.ofSeconds(10);
	private static final Duration SYNCHRONIZATION_DELAY_INITIAL_MAX = Duration.ofSeconds(15);
	private static final Duration SYNCHRONIZATION_DELAY = Duration.ofMinutes(1);

	private static final Duration PENDING_VERIFICATION_MAX = Duration.ofMinutes(1);
	private static final Duration PENDING_VERIFICATION_DELAY = Duration.ofSeconds(10);

	protected final GxsTransactionManager gxsTransactionManager;
	protected final PeerConnectionManager peerConnectionManager;
	private final GxsClientUpdateRepository gxsClientUpdateRepository;
	private final GxsServiceSettingRepository gxsServiceSettingRepository;
	private final IdentityManager identityManager;
	private final GxsGroupItemRepository gxsGroupItemRepository;
	private final TransactionTemplate transactionTemplate;

	private final Type itemGroupClass;
	private final Type itemMessageClass;

	private ScheduledExecutorService executorService;

	private final Map<G, Long> pendingGxsGroups = new ConcurrentHashMap<>();

	protected GxsRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, GxsClientUpdateRepository gxsClientUpdateRepository, GxsServiceSettingRepository gxsServiceSettingRepository, IdentityManager identityManager, GxsGroupItemRepository gxsGroupItemRepository, TransactionTemplate transactionTemplate)
	{
		super(rsServiceRegistry);
		this.gxsTransactionManager = gxsTransactionManager;
		this.peerConnectionManager = peerConnectionManager;
		this.gxsClientUpdateRepository = gxsClientUpdateRepository;
		this.gxsServiceSettingRepository = gxsServiceSettingRepository;
		this.identityManager = identityManager;
		this.gxsGroupItemRepository = gxsGroupItemRepository;
		this.transactionTemplate = transactionTemplate;

		// Type information is available when subclassing a class using a generic type, which means itemClass is the class of G
		itemGroupClass = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		itemMessageClass = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1]; // and M
	}

	protected enum VerificationStatus
	{
		OK,
		FAILED,
		DELAYED
	}

	/**
	 * Called when the peer wants a list of new or updated groups that we have for him.
	 *
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
	 *
	 * @param item   the received group
	 */
	protected abstract boolean onGroupReceived(G item);

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
	 *
	 * @param sender the sender of the group
	 * @param item   the received message
	 */
	protected abstract void onMessageReceived(PeerConnection sender, M item);

	@Override
	public RsServiceType getServiceType()
	{
		throw new IllegalStateException("Must override getServiceType()");
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.LOW;
	}

	@Override
	public void initialize()
	{
		executorService = Executors.newSingleThreadScheduledExecutor();

		executorService.scheduleAtFixedRate((NoSuppressedRunnable) this::checkPendingGroups,
				getInitPriority().getMaxTime() + PENDING_VERIFICATION_DELAY.toSeconds() / 2,
				PENDING_VERIFICATION_DELAY.toSeconds(),
				TimeUnit.SECONDS);
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

	@Override
	public void cleanup()
	{
		if (executorService != null) // Can happen when running tests
		{
			executorService.shutdownNow();
		}
	}

	private void sync(PeerConnection peerConnection)
	{
		var gxsSyncGroupRequestItem = new GxsSyncGroupRequestItem(getLastPeerGroupsUpdate(peerConnection.getLocation(), getServiceType()));
		log.debug("Asking peer {} for last local sync {} for service {}", peerConnection, gxsSyncGroupRequestItem.getLastUpdated(), getServiceType());
		peerConnectionManager.writeItem(peerConnection, gxsSyncGroupRequestItem, this);
	}

	private void checkPendingGroups()
	{
		pendingGxsGroups.forEach((gxsGroupItem, delay) -> {
			var author = identityManager.getGxsGroup(peerConnectionManager.getRandomPeer(), gxsGroupItem.getAuthor());
			if (author != null)
			{
				var status = verifyGroup(gxsGroupItem, author);
				if (status == VerificationStatus.OK)
				{
					saveGroup(gxsGroupItem);
				}
				else
				{
					log.warn("Failed to validate {}, wrong signature", gxsGroupItem);
				}
				pendingGxsGroups.put(gxsGroupItem, -1L); // Remove the entry
			}
			else
			{
				var newDelay = delay - PENDING_VERIFICATION_DELAY.toSeconds();
				pendingGxsGroups.put(gxsGroupItem, newDelay);
				if (newDelay < 0)
				{
					log.warn("Failed to validate {}. Timeout exceeded", gxsGroupItem);
				}
			}
		});
		pendingGxsGroups.entrySet().removeIf(gxsGroupItemLongEntry -> gxsGroupItemLongEntry.getValue() < 0);
	}

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
					getLastServiceGroupsUpdate(getServiceType()), // XXX: mGrpServerUpdate.grpUpdateTS... I think it's that but recheck
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
					getLastServiceGroupsUpdate(getServiceType()), // XXX: not sure that's correct
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
			gxsTransactionManager.addIncomingItemToTransaction(peerConnection, item, this);
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
		var lastServiceUpdate = getLastServiceGroupsUpdate(getServiceType());
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
			transferItems.forEach(gxsTransferGroupItem -> verifyAndSaveGroup(peerConnection, convertTransferGroupToGxsGroup(gxsTransferGroupItem)));
			if (!transferItems.isEmpty())
			{
				setLastPeerGroupsUpdate(peerConnection.getLocation(), transaction.getUpdated(), getServiceType());
				setLastServiceGroupsUpdateNow(getServiceType());
			}
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
			transferItems.forEach(gxsTransferMessageItem -> {
				var gxsMessageItem = convertTransferGroupToGxsMessage(gxsTransferMessageItem);
				identityManager.getGxsGroup(peerConnection, gxsMessageItem.getAuthorId()); // Prefetch the identity that we will need later
				onMessageReceived(peerConnection, gxsMessageItem);
			});
			if (!transferItems.isEmpty())
			{
				setLastPeerMessageUpdate(peerConnection.getLocation(), transferItems.get(0).getGroupId(), transaction.getUpdated(), getServiceType());
				//setLastServiceGroupsUpdateNow(getServiceType()); XXX: should that be done? I'd say no but RS has some comment in the source about it
			}
		}
	}

	private void verifyAndSaveGroup(PeerConnection peerConnection, G gxsGroupItem)
	{
		var verified = gxsGroupItem.getAuthor() == null ? VerificationStatus.OK : verifyGroup(gxsGroupItem, identityManager.getGxsGroup(peerConnection, gxsGroupItem.getAuthor()));
		if (verified == VerificationStatus.FAILED)
		{
			log.error("Failed to verify group {}", gxsGroupItem);
		}
		else if (verified == VerificationStatus.DELAYED)
		{
			log.warn("Delaying verification of group {}", gxsGroupItem);
			pendingGxsGroups.putIfAbsent(gxsGroupItem, PENDING_VERIFICATION_MAX.toSeconds());
		}
		else
		{
			saveGroup(gxsGroupItem);
		}
	}

	private void saveGroup(G gxsGroupItem)
	{
		transactionTemplate.executeWithoutResult(transactionStatus -> {
			gxsGroupItem.setId(gxsGroupItemRepository.findByGxsId(gxsGroupItem.getGxsId()).orElse(gxsGroupItem).getId());
			if (onGroupReceived(gxsGroupItem) && gxsGroupItem.getAdminPrivateKey() == null) // Don't overwrite our own groups
			{
				gxsGroupItemRepository.save(gxsGroupItem);
			}
		});
	}

	private G createGxsGroupItem()
	{
		G gxsGroupItem;

		try
		{
			//noinspection unchecked
			gxsGroupItem = ((Class<G>) itemGroupClass).getDeclaredConstructor().newInstance();
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
		{
			throw new IllegalArgumentException("Failed to instantiate " + ((Class<?>) itemGroupClass).getSimpleName() + " missing empty constructor?");
		}
		return gxsGroupItem;
	}

	private G convertTransferGroupToGxsGroup(GxsTransferGroupItem fromItem)
	{
		var toItem = createGxsGroupItem();

		fromItem.toGxsGroupItem(toItem);

		return toItem;
	}

	private void sendGxsGroups(PeerConnection peerConnection, List<G> gxsGroupItems)
	{
		var transactionId = getTransactionId(peerConnection);
		List<GxsTransferGroupItem> items = new ArrayList<>();
		gxsGroupItems.forEach(gxsGroupItem -> items.add(new GxsTransferGroupItem(gxsGroupItem, transactionId, getServiceType())));

		gxsTransactionManager.startOutgoingTransactionForGroupTransfer(
				peerConnection,
				items,
				getLastServiceGroupsUpdate(getServiceType()),
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
		gxsMessageItems.forEach(gxsMessageItem -> items.add(new GxsTransferMessageItem(gxsMessageItem, transactionId, getServiceType())));

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

	private M createGxsMessageItem()
	{
		M gxsMessageItem;

		try
		{
			//noinspection unchecked
			gxsMessageItem = ((Class<M>) itemMessageClass).getDeclaredConstructor().newInstance();
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
		{
			throw new IllegalArgumentException("Failed to instantiate " + ((Class<?>) itemMessageClass).getSimpleName() + " missing empty constructor?");
		}
		return gxsMessageItem;
	}

	private M convertTransferGroupToGxsMessage(GxsTransferMessageItem fromItem)
	{
		var toItem = createGxsMessageItem();

		fromItem.toGxsMessageItem(toItem);

		return toItem;
	}

	protected G createGroup(String name)
	{
		var adminKeyPair = RSA.generateKeys(GXS_KEY_SIZE);

		var adminPrivateKey = (RSAPrivateKey) adminKeyPair.getPrivate();
		var adminPublicKey = (RSAPublicKey) adminKeyPair.getPublic();

		// The GxsId is from the public admin key (n and e)
		var gxsId = RSA.getGxsId(adminPublicKey);

		var gxsGroupItem = createGxsGroupItem();
		gxsGroupItem.setGxsId(gxsId);
		gxsGroupItem.setName(name);
		gxsGroupItem.setAdminPrivateKey(adminPrivateKey);
		gxsGroupItem.setAdminPublicKey(adminPublicKey);
		gxsGroupItem.updatePublished();

		return gxsGroupItem;
	}

	protected void signGroupIfNeeded(GxsGroupItem gxsGroupItem)
	{
		if (gxsGroupItem.getAdminPrivateKey() == null)
		{
			return; // Only sign our own groups
		}

		var data = serializeItemForSignature(gxsGroupItem);
		var signature = RSA.sign(data, gxsGroupItem.getAdminPrivateKey());
		gxsGroupItem.setAdminSignature(signature);
	}

	private VerificationStatus verifyGroup(GxsGroupItem gxsGroupItem, IdentityGroupItem author)
	{
		if (gxsGroupItem.getAuthorSignature() != null)
		{
			if (author == null)
			{
				return VerificationStatus.DELAYED;
			}
			var data = serializeItemForSignature(gxsGroupItem);
			if (!RSA.verify(author.getAdminPublicKey(), gxsGroupItem.getAuthorSignature(), data))
			{
				return VerificationStatus.FAILED;
			}
		}
		return VerificationStatus.OK;
	}

	protected void signMessage(GxsMessageItem gxsMessageItem)
	{
		// TODO: implement (need to check authorId, etc...). do it for authorSignature, publishSignature too but it's for circles I think
	}

	private byte[] serializeItemForSignature(Item item)
	{
		item.setSerialization(Unpooled.buffer().alloc(), this);
		var buf = item.serializeItem(EnumSet.of(SerializationFlags.SIGNATURE)).getBuffer();
		// Skip the header
		var data = new byte[buf.writerIndex() - HEADER_SIZE];
		buf.getBytes(HEADER_SIZE, data);
		buf.release();
		return data;
	}

	/**
	 * Gets the last update time of the peer's groups. The peer's time is always used, not our local time.
	 *
	 * @param location    the peer's location
	 * @param serviceType the service type
	 * @return the time when the peer last updated its groups, in peer's time
	 */
	public Instant getLastPeerGroupsUpdate(Location location, RsServiceType serviceType)
	{
		return gxsClientUpdateRepository.findByLocationAndServiceType(location, serviceType.getType())
				.map(GxsClientUpdate::getLastSynced)
				.orElse(Instant.EPOCH).truncatedTo(ChronoUnit.SECONDS);
	}

	public Instant getLastPeerMessagesUpdate(Location location, GxsId groupId, RsServiceType serviceType)
	{
		return gxsClientUpdateRepository.findByLocationAndServiceType(location, serviceType.getType())
				.map(gxsClientUpdate -> gxsClientUpdate.getMessageUpdate(groupId))
				.orElse(Instant.EPOCH).truncatedTo(ChronoUnit.SECONDS);
	}

	/**
	 * Sets the last update time of the peer's groups. The peer's time is always used, not our local time.
	 *
	 * @param location    the peer's location
	 * @param update      the peer's last update time, in peer's time (so given by the peer itself). Never supply a time computed locally
	 * @param serviceType the service type
	 */
	public void setLastPeerGroupsUpdate(Location location, Instant update, RsServiceType serviceType)
	{
		transactionTemplate.executeWithoutResult(status -> gxsClientUpdateRepository.findByLocationAndServiceType(location, serviceType.getType())
				.ifPresentOrElse(gxsClientUpdate -> {
					gxsClientUpdate.setLastSynced(update);
					gxsClientUpdateRepository.save(gxsClientUpdate);
				}, () -> gxsClientUpdateRepository.save(new GxsClientUpdate(location, serviceType.getType(), update))));
	}

	public void setLastPeerMessageUpdate(Location location, GxsId groupId, Instant update, RsServiceType serviceType)
	{
		transactionTemplate.executeWithoutResult(status -> gxsClientUpdateRepository.findByLocationAndServiceType(location, serviceType.getType())
				.ifPresentOrElse(gxsClientUpdate -> {
					gxsClientUpdate.addMessageUpdate(groupId, update);
					gxsClientUpdateRepository.save(gxsClientUpdate);
				}, () -> gxsClientUpdateRepository.save(new GxsClientUpdate(location, serviceType.getType(), update))));
	}

	/**
	 * Gets the last time our service's groups were updated. This uses the local time.
	 *
	 * @param serviceType the service type
	 * @return the last time
	 */
	public Instant getLastServiceGroupsUpdate(RsServiceType serviceType)
	{
		return gxsServiceSettingRepository.findById(serviceType.getType())
				.map(GxsServiceSetting::getLastUpdated)
				.orElse(Instant.EPOCH).truncatedTo(ChronoUnit.SECONDS);
	}

	/**
	 * Sets the last time our service's groups were updated.
	 *
	 * @param serviceType the service type
	 */
	public void setLastServiceGroupsUpdateNow(RsServiceType serviceType)
	{
		var now = Instant.now(); // we always use local time
		transactionTemplate.executeWithoutResult(status -> gxsServiceSettingRepository.findById(serviceType.getType())
				.ifPresentOrElse(gxsServiceSetting -> {
					gxsServiceSetting.setLastUpdated(Instant.now());
					gxsServiceSettingRepository.save(gxsServiceSetting);
				}, () -> gxsServiceSettingRepository.save(new GxsServiceSetting(serviceType.getType(), now))));
	}
}
