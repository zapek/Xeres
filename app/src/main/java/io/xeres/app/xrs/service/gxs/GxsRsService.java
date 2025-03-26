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

import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemUtils;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxs.item.*;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import io.xeres.common.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
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
 * <img src="doc-files/transfer.png" alt="Transfer diagram">
 *
 * @param <G> the GxsGroupItem subclass
 * @param <M> the GxsMessageItem subclass
 */
public abstract class GxsRsService<G extends GxsGroupItem, M extends GxsMessageItem> extends RsService
{
	protected final Logger log = LoggerFactory.getLogger(getClass().getName());

	private static final int GXS_KEY_SIZE = 2048; // The RSA size of Gxs keys. Do not change unless you want everything to break.
	private static final int KEY_TRANSACTION_ID = 1; // This is stored per peer
	private static final int KEY_LAST_SYNC = 2; // This is stored per peer and per service

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
	private final IdentityManager identityManager;
	private final GxsUpdateService<G, M> gxsUpdateService;
	private final DatabaseSessionManager databaseSessionManager;

	private final Type itemGroupClass;
	private final Type itemMessageClass;

	private ScheduledExecutorService executorService;

	private final Map<G, Long> pendingGxsGroups = new ConcurrentHashMap<>();
	private final Map<M, Long> pendingGxsMessages = new ConcurrentHashMap<>();

	private AuthenticationRequirements authenticationRequirements;

	protected GxsRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager, IdentityManager identityManager, GxsUpdateService<G, M> gxsUpdateService)
	{
		super(rsServiceRegistry);
		this.gxsTransactionManager = gxsTransactionManager;
		this.peerConnectionManager = peerConnectionManager;
		this.databaseSessionManager = databaseSessionManager;
		this.identityManager = identityManager;
		this.gxsUpdateService = gxsUpdateService;

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
	 *
	 * @param ids the ids of updated groups and their update time that the peer has for us
	 * @return the subset of those groups that we actually want
	 */
	protected abstract Set<GxsId> onAvailableGroupListResponse(Map<GxsId, Instant> ids);

	/**
	 * Called when the peer wants specific groups to be transferred to him.
	 *
	 * @param ids the groups that the peer wants
	 * @return the groups that we have available within the requested set
	 */
	protected abstract List<G> onGroupListRequest(Set<GxsId> ids);

	/**
	 * Called when a group has been received.
	 *
	 * @param item the received group
	 * @return true if the group must be saved to disk
	 */
	protected abstract boolean onGroupReceived(G item);

	/**
	 * Called when the groups have been saved.
	 *
	 * @param items the list of groups that have been successfully saved to disk
	 */
	protected abstract void onGroupsSaved(List<G> items);

	/**
	 * Called when the peer wants a list of new messages within a group that we have for him.
	 *
	 * @param recipient the recipient of the result
	 * @param groupId   the group ID
	 * @param since     the time after which the messages are relevant. Everything before is ignored
	 * @return the available messages that we have
	 */
	protected abstract List<M> onPendingMessageListRequest(PeerConnection recipient, GxsId groupId, Instant since);

	/**
	 * Called when the peer wants specific messages to be transferred to him, within a group.
	 *
	 * @param groupId    the group ID
	 * @param messageIds the ids of messages that the peer wants
	 * @return the messages that we have available within the requested set
	 */
	protected abstract List<M> onMessageListRequest(GxsId groupId, Set<MessageId> messageIds);

	/**
	 * Called when a peer sends the list of new messages that might interest us, within a group.
	 *
	 * @param groupId    the group ID
	 * @param messageIds the ids of new messages
	 * @return the subset of those messages that we actually want
	 */
	protected abstract List<MessageId> onMessageListResponse(GxsId groupId, Set<MessageId> messageIds);

	/**
	 * Called when a message has been received.
	 *
	 * @param item the received message
	 */
	protected abstract boolean onMessageReceived(M item);

	protected abstract void onMessagesSaved(List<M> items);

	protected abstract AuthenticationRequirements getAuthenticationRequirements();

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
		authenticationRequirements = Objects.requireNonNull(getAuthenticationRequirements(), "AuthenticationRequirements cannot be null");

		executorService = ExecutorUtils.createFixedRateExecutor(this::checkPendingGroupsAndMessages,
				getInitPriority().getMaxTime() + PENDING_VERIFICATION_DELAY.toSeconds() / 2,
				PENDING_VERIFICATION_DELAY.toSeconds());
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		peerConnection.scheduleWithFixedDelay(
				() -> autoSync(peerConnection),
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
		}
		else
		{
			switch (item)
			{
				case GxsSyncGroupStatsItem gxsSyncGroupStatsItem -> log.debug("Would handle group statistics item (not implemented yet)"); // XXX:
				case GxsSyncNotifyItem gxsSyncNotifyItem -> handleGxsSyncNotifyItem(sender, gxsSyncNotifyItem);
				case null, default -> log.error("Not a GxsExchange item: {}, ignoring", item);
			}
		}
	}

	@Override
	public void cleanup()
	{
		ExecutorUtils.cleanupExecutor(executorService);
	}

	/**
	 * Syncs automatically each SYNCHRONIZATION_DELAY, unless a syncNow() was performed in between, in that case
	 * skip until the next one.
	 *
	 * @param peerConnection the peer connection
	 */
	private void autoSync(PeerConnection peerConnection)
	{
		var lastSync = (Instant) peerConnection.getServiceData(this, KEY_LAST_SYNC).orElse(Instant.EPOCH);
		if (Duration.between(lastSync, Instant.now()).compareTo(SYNCHRONIZATION_DELAY.minusSeconds(1)) > 0)
		{
			syncNow(peerConnection);
		}
	}

	private void syncNow(PeerConnection peerConnection)
	{
		var gxsSyncGroupRequestItem = new GxsSyncGroupRequestItem(gxsUpdateService.getLastPeerGroupsUpdate(peerConnection.getLocation(), getServiceType()));
		log.debug("Asking {} for last local sync {} for service {}", peerConnection, log.isDebugEnabled() ? Instant.ofEpochSecond(gxsSyncGroupRequestItem.getLastUpdated()) : null, getServiceType());
		peerConnectionManager.writeItem(peerConnection, gxsSyncGroupRequestItem, this);
		peerConnection.putServiceData(this, KEY_LAST_SYNC, Instant.now());
	}

	private void checkPendingGroupsAndMessages()
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			var randomPeer = peerConnectionManager.getRandomPeer();
			if (randomPeer != null)
			{
				verifyAndStoreGroups(randomPeer, pendingGxsGroups.keySet());
				verifyAndStoreMessages(randomPeer, pendingGxsMessages.keySet());
			}
			pendingGxsGroups.entrySet().removeIf(gxsGroupItemLongEntry -> gxsGroupItemLongEntry.getValue() < 0);
			pendingGxsMessages.entrySet().removeIf(gxsMessageItemLongEntry -> gxsMessageItemLongEntry.getValue() < 0);
		}
	}

	private void handleGxsSyncNotifyItem(PeerConnection peerConnection, GxsSyncNotifyItem item)
	{
		log.debug("Got {} from {}", item, peerConnection);

		syncNow(peerConnection);
	}

	protected void sendSyncNotification(PeerConnection peerConnection)
	{
		var gxsSyncNotifyItem = new GxsSyncNotifyItem();
		log.debug("Sending sync notification to {}", peerConnection);
		peerConnectionManager.writeItem(peerConnection, gxsSyncNotifyItem, this);
	}

	private void handleGxsSyncGroupRequestItem(PeerConnection peerConnection, GxsSyncGroupRequestItem item)
	{
		log.debug("{} sent {}", peerConnection, item);

		var transactionId = getNextTransactionId(peerConnection);
		var since = Instant.ofEpochSecond(item.getLastUpdated());
		if (areGxsUpdatesAvailableForPeer(since))
		{
			log.debug("Group updates available, sending ids...");
			List<GxsSyncGroupItem> items = new ArrayList<>();

			onAvailableGroupListRequest(peerConnection, since).forEach(gxsGroupItem -> {
				if (isGxsAllowedForPeer(peerConnection, gxsGroupItem))
				{
					log.debug("Adding group id of item: {}", gxsGroupItem);
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
					gxsUpdateService.getLastServiceGroupsUpdate(getServiceType()), // XXX: mGrpServerUpdate.grpUpdateTS... I think it's that but recheck
					transactionId,
					this
			);
		}
		else
		{
			log.debug("No group updates available");
		}

		// XXX: check if the peer is subscribed, encrypt or not the group, etc... it's rsgxsnetservice.cc/handleRecvSyncGroup we might not need that for gxsid transferts

		// XXX: to handle the synchronization we must know which tables to use, then it's generic
	}

	private void handleGxsSyncMessageRequestItem(PeerConnection peerConnection, GxsSyncMessageRequestItem item)
	{
		log.debug("{} sent {}", peerConnection, item);

		var transactionId = getNextTransactionId(peerConnection);
		var lastUpdated = Instant.ofEpochSecond(item.getLastUpdated());
		var since = Instant.ofEpochSecond(item.getCreateSince());
		if (areMessageUpdatesAvailableForPeer(item.getGroupId(), lastUpdated, since))
		{
			log.debug("New messages available, sending ids...");
			List<GxsSyncMessageItem> items = new ArrayList<>();

			onPendingMessageListRequest(peerConnection, item.getGroupId(), since).forEach(gxsMessageItem -> {
				log.debug("Adding message id of item {}", gxsMessageItem);
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
					gxsUpdateService.getLastServiceGroupsUpdate(getServiceType()), // XXX: not sure that's correct
					transactionId,
					this
			);
		}
		else
		{
			log.debug("No new messages");
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

	protected synchronized int getNextTransactionId(PeerConnection peerConnection)
	{
		var transactionId = (int) peerConnection.getPeerData(KEY_TRANSACTION_ID).orElse(0) + 1;
		peerConnection.putPeerData(KEY_TRANSACTION_ID, transactionId);
		return transactionId;
	}

	private boolean areGxsUpdatesAvailableForPeer(Instant lastPeerUpdate)
	{
		var lastServiceUpdate = gxsUpdateService.getLastServiceGroupsUpdate(getServiceType());
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

		var group = groupList.getFirst();
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
			log.debug("{} has the following group ids (new or updates) for us (total: {}): {} ...", peerConnection, gxsIdsMap.keySet().size(), gxsIdsMap.keySet().stream().limit(10).toList());
			requestGxsGroups(peerConnection, onAvailableGroupListResponse(gxsIdsMap));
		}
		else if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_GROUP_LIST_REQUEST))
		{
			@SuppressWarnings("unchecked")
			var gxsIds = ((List<GxsSyncGroupItem>) transaction.getItems()).stream()
					.map(GxsSyncGroupItem::getGroupId).collect(toSet());
			log.debug("{} wants the following group ids (total: {}): {} ...", peerConnection, gxsIds.size(), gxsIds.stream().limit(10).toList());
			sendGxsGroups(peerConnection, onGroupListRequest(gxsIds));
		}
		else if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_GROUPS))
		{
			@SuppressWarnings("unchecked")
			var gxsGroupItems = ((List<GxsTransferGroupItem>) transaction.getItems()).stream()
					.map(this::convertTransferGroupToGxsGroup)
					.toList();

			verifyAndStoreGroups(peerConnection, gxsGroupItems);
			if (!gxsGroupItems.isEmpty())
			{
				log.debug("{} sent groups", peerConnection);
				gxsUpdateService.setLastPeerGroupsUpdate(peerConnection.getLocation(), transaction.getUpdated(), getServiceType());
				gxsUpdateService.setLastServiceGroupsUpdateNow(getServiceType());
				peerConnectionManager.doForAllPeersExceptSender(this::sendSyncNotification, peerConnection, this);
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
			log.debug("{} has the following message ids for group {} (new) for us (total: {}): {} ...", peerConnection, groupId, messageIds.size(), messageIds.stream().limit(10).toList());
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
			log.debug("{} wants the following message ids for group {} (total: {}): {} ...", peerConnection, groupId, messageIds.size(), messageIds.stream().limit(10).toList());
			sendGxsMessages(peerConnection, onMessageListRequest(groupId, messageIds));
		}
		else if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_MESSAGES))
		{
			@SuppressWarnings("unchecked")
			var gxsMessageItems = ((List<GxsTransferMessageItem>) transaction.getItems()).stream()
					.map(this::convertTransferGroupToGxsMessage)
					.toList();

			verifyAndStoreMessages(peerConnection, gxsMessageItems);
			if (!gxsMessageItems.isEmpty())
			{
				log.debug("{} sent messages", peerConnection);
				gxsUpdateService.setLastPeerMessageUpdate(peerConnection.getLocation(), gxsMessageItems.getFirst().getGxsId(), transaction.getUpdated(), getServiceType());
				//setLastServiceGroupsUpdateNow(getServiceType()); XXX: should that be done? I'd say no but RS has some comment in the source about it
				peerConnectionManager.doForAllPeersExceptSender(this::sendSyncNotification, peerConnection, this);
			}
		}
	}

	private void verifyAndStoreGroups(PeerConnection peerConnection, Collection<G> gxsGroupItems)
	{
		List<G> savedGroups = new ArrayList<>(gxsGroupItems.size());

		for (var gxsGroupItem : gxsGroupItems)
		{
			var validation = VerificationStatus.OK;

			if (!gxsGroupItem.hasAdminPublicKey())
			{
				log.warn("Failed to validate group {}: missing admin key", gxsGroupItem);
				continue;
			}

			// Validate author signature
			if (gxsGroupItem.getAuthor() != null)
			{
				validation = verifyGroup(peerConnection, gxsGroupItem);
				if (validation == VerificationStatus.DELAYED)
				{
					continue;
				}
			}

			// If this is a group update, validate its admin signature using the public key we already have
			if (validation == VerificationStatus.OK)
			{
				validation = verifyGroupForUpdate(gxsGroupItem);
			}

			// Save the group if everything is OK
			if (validation == VerificationStatus.OK)
			{
				gxsUpdateService.saveGroup(gxsGroupItem, this::onGroupReceived).ifPresent(savedGroups::add);
			}

			// If the group verification was delayed, remove it
			pendingGxsGroups.computeIfPresent(gxsGroupItem, (group, delay) -> -1L);
		}

		if (!savedGroups.isEmpty())
		{
			onGroupsSaved(savedGroups);
		}
	}

	private VerificationStatus verifyGroup(PeerConnection peerConnection, G gxsGroupItem)
	{
		if (gxsGroupItem.getAuthorSignature() == null)
		{
			log.warn("Missing author signature for group {}", gxsGroupItem);
			return VerificationStatus.FAILED;
		}

		var authorIdentity = identityManager.getGxsGroup(peerConnection, gxsGroupItem.getAuthor());
		if (authorIdentity == null)
		{
			log.warn("Delaying verification of group {}", gxsGroupItem);
			var existingDelay = pendingGxsGroups.putIfAbsent(gxsGroupItem, PENDING_VERIFICATION_MAX.toSeconds());
			if (existingDelay != null)
			{
				var newDelay = existingDelay - PENDING_VERIFICATION_DELAY.toSeconds();
				pendingGxsGroups.put(gxsGroupItem, newDelay);
				if (newDelay < 0)
				{
					log.warn("Failed to validate group {}: timeout exceeded", gxsGroupItem);
				}
			}
			return VerificationStatus.DELAYED;
		}
		else
		{
			if (validateGroup(gxsGroupItem, authorIdentity.getAdminPublicKey(), gxsGroupItem.getAuthorSignature()))
			{
				return VerificationStatus.OK;
			}
			else
			{
				log.warn("Failed to validate group {}: wrong author signature", gxsGroupItem);
				return VerificationStatus.FAILED;
			}
		}
	}

	private VerificationStatus verifyGroupForUpdate(G gxsGroupItem)
	{
		var existingGroup = gxsUpdateService.getExistingGroup(gxsGroupItem);
		if (existingGroup.isPresent())
		{
			if (validateGroup(gxsGroupItem, existingGroup.get().getAdminPublicKey(), gxsGroupItem.getAdminSignature()))
			{
				return VerificationStatus.OK;
			}
			else
			{
				if (isSameKey(existingGroup.get().getAdminPublicKey(), gxsGroupItem.getAdminPublicKey()))
				{
					log.warn("Failed to validate group {} for update: wrong admin signature", gxsGroupItem);
				}
				else
				{
					log.warn("Failed to validate group {} for update: new public key doesn't match the old one", gxsGroupItem);
				}
				return VerificationStatus.FAILED;
			}
		}
		return VerificationStatus.OK;
	}

	private boolean validateGroup(G gxsGroupItem, PublicKey publicKey, byte[] signature)
	{
		var data = ItemUtils.serializeItemForSignature(gxsGroupItem, this);
		return RSA.verify(publicKey, signature, data);
	}

	private static boolean isSameKey(PublicKey a, PublicKey b)
	{
		return Arrays.equals(a.getEncoded(), b.getEncoded());
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
		var transactionId = getNextTransactionId(peerConnection);
		List<GxsTransferGroupItem> items = new ArrayList<>();
		gxsGroupItems.forEach(gxsGroupItem -> items.add(new GxsTransferGroupItem(gxsGroupItem, transactionId, getServiceType())));

		gxsTransactionManager.startOutgoingTransactionForGroupTransfer(
				peerConnection,
				items,
				gxsUpdateService.getLastServiceGroupsUpdate(getServiceType()),
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
		var transactionId = getNextTransactionId(peerConnection);
		List<GxsSyncGroupItem> items = new ArrayList<>();

		ids.forEach(gxsId -> items.add(new GxsSyncGroupItem(REQUEST, gxsId, transactionId)));

		gxsTransactionManager.startOutgoingTransactionForGroupIdRequest(peerConnection, items, transactionId, this);
	}

	private void verifyAndStoreMessages(PeerConnection peerConnection, Collection<M> gxsMessageItems)
	{
		List<M> savedMessages = new ArrayList<>(gxsMessageItems.size());

		for (var gxsMessageItem : gxsMessageItems)
		{
			var validation = VerificationStatus.OK;
			var publishSignature = false;

			if (gxsMessageItem.getParentId() != null)
			{
				publishSignature = authenticationRequirements.getPublicRequirements().contains(AuthenticationRequirements.Flags.CHILD_PUBLISH);
			}
			else
			{
				publishSignature = authenticationRequirements.getPublicRequirements().contains(AuthenticationRequirements.Flags.ROOT_PUBLISH);
			}

			if (publishSignature)
			{
				// XXX: implement... I think those keys are stored in the actual message (private groups?)
				log.error("Publish verification not implemented yet!");
			}

			if (gxsMessageItem.getAuthorId() != null)
			{
				validation = verifyMessage(peerConnection, gxsMessageItem);
				if (validation == VerificationStatus.DELAYED)
				{
					continue;
				}
			}

			// Save the message if everything is OK
			if (validation == VerificationStatus.OK)
			{
				gxsUpdateService.saveMessage(gxsMessageItem, this::onMessageReceived).ifPresent(savedMessages::add);
			}

			// If the message verification was delayed, remove it
			pendingGxsMessages.computeIfPresent(gxsMessageItem, (message, delay) -> -1L);
		}

		if (!savedMessages.isEmpty())
		{
			onMessagesSaved(savedMessages);
		}
	}

	private VerificationStatus verifyMessage(PeerConnection peerConnection, M gxsMessageItem)
	{
		if (gxsMessageItem.getAuthorSignature() == null)
		{
			log.warn("Missing author signature for message {}", gxsMessageItem);
			return VerificationStatus.FAILED;
		}

		var authorIdentity = identityManager.getGxsGroup(peerConnection, gxsMessageItem.getAuthorId());
		if (authorIdentity == null)
		{
			log.warn("Delaying verification of message {}", gxsMessageItem);
			var existingDelay = pendingGxsMessages.putIfAbsent(gxsMessageItem, PENDING_VERIFICATION_MAX.toSeconds());
			if (existingDelay != null)
			{
				var newDelay = existingDelay - PENDING_VERIFICATION_DELAY.toSeconds();
				pendingGxsMessages.put(gxsMessageItem, newDelay);
				if (newDelay < 0)
				{
					log.warn("Failed to validate group {}: timeout exceeded", gxsMessageItem);
				}
			}
			return VerificationStatus.DELAYED;
		}
		else
		{
			if (validateMessage(gxsMessageItem, authorIdentity.getAdminPublicKey(), gxsMessageItem.getAuthorSignature()))
			{
				// XXX: check for reputation here, if reputation is too low, remove
				return VerificationStatus.OK;
			}
			else
			{
				log.warn("Failed to validate message {}: wrong author signature", gxsMessageItem);
				return VerificationStatus.FAILED;
			}
		}
	}

	private boolean validateMessage(M gxsMessageItem, PublicKey publicKey, byte[] signature)
	{
		// Clear messageId and possibly originalMessageId because they're created after the signature
		// is made (they depend on the content)
		var savedMessageId = gxsMessageItem.getMessageId();
		var savedOriginalMessageId = savedMessageId.equals(gxsMessageItem.getOriginalMessageId()) ? gxsMessageItem.getOriginalMessageId() : null;
		gxsMessageItem.setMessageId(null);
		gxsMessageItem.setOriginalMessageId(null);

		var data = ItemUtils.serializeItemForSignature(gxsMessageItem, this);

		// And restore them
		gxsMessageItem.setMessageId(savedMessageId);
		gxsMessageItem.setOriginalMessageId(savedOriginalMessageId);

		return RSA.verify(publicKey, signature, data);
	}

	public void sendGxsMessages(PeerConnection peerConnection, List<M> gxsMessageItems)
	{
		var transactionId = getNextTransactionId(peerConnection);
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
		var transactionId = getNextTransactionId(peerConnection);
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
		return createGroup(name, adminKeyPair);
	}

	protected G createGroup(String name, KeyPair keyPair)
	{
		var adminPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
		var adminPublicKey = (RSAPublicKey) keyPair.getPublic();

		// The GxsId is from the public admin key (n and e)
		var gxsId = RSA.getGxsId(adminPublicKey);

		var gxsGroupItem = createGxsGroupItem();
		gxsGroupItem.setGxsId(gxsId);
		gxsGroupItem.setName(name);
		gxsGroupItem.updatePublished();
		gxsGroupItem.setAdminKeys(adminPrivateKey, adminPublicKey, gxsGroupItem.getPublished(), null);

		return gxsGroupItem;
	}

	protected void signGroupIfNeeded(GxsGroupItem gxsGroupItem)
	{
		if (gxsGroupItem.isExternal())
		{
			return; // Only sign our own groups
		}

		var data = ItemUtils.serializeItemForSignature(gxsGroupItem, this);
		var signature = RSA.sign(data, gxsGroupItem.getAdminPrivateKey());
		gxsGroupItem.setAdminSignature(signature);

		if (gxsGroupItem.getAuthor() != null)
		{
			var author = identityManager.getGxsGroup(gxsGroupItem.getAuthor());
			Objects.requireNonNull(author, "Couldn't get own identity. Shouldn't happen (tm)");
			var authorSignature = RSA.sign(data, author.getAdminPrivateKey());
			gxsGroupItem.setAuthorSignature(authorSignature);
		}
	}

	// XXX: remove!
	protected M createMessage(GxsId groupId, String name)
	{
		var gxsMessageItem = createGxsMessageItem();
		gxsMessageItem.setGxsId(groupId);
		gxsMessageItem.setName(name);
		gxsMessageItem.updatePublished();

		return gxsMessageItem;
	}

	protected final class MessageBuilder
	{
		private final M gxsMessageItem;
		private final PrivateKey privateKey;

		public MessageBuilder(PrivateKey privateKey, GxsId groupId, String name)
		{
			gxsMessageItem = createGxsMessageItem();
			gxsMessageItem.setGxsId(groupId);
			gxsMessageItem.setName(name);
			gxsMessageItem.updatePublished(); // XXX: do it at build time?
			this.privateKey = privateKey;
		}

		public MessageBuilder originalMessageId(MessageId originalMessageId)
		{
			// XXX: original message must exist!
			gxsMessageItem.setOriginalMessageId(originalMessageId);
			return this;
		}

		public MessageBuilder authorId(GxsId authorId)
		{
			gxsMessageItem.setAuthorId(authorId);
			return this;
		}

		public MessageBuilder parentId(MessageId parentId)
		{
			// XXX: if parentId != 0L, then threadId must be set
			gxsMessageItem.setParentId(parentId);
			return this;
		}

		public M getMessageItem()
		{
			return gxsMessageItem;
		}

		public M build()
		{
			// XXX: serviceType? how? how does group do it?

			// The identifier is the sha1 hash of the data and meta
			var data = ItemUtils.serializeItemForSignature(gxsMessageItem, GxsRsService.this);

			var md = new Sha1MessageDigest();
			md.update(data);
			gxsMessageItem.setMessageId(new MessageId(md.getBytes()));

			// The signature is performed afterwards
			signMessage(gxsMessageItem, data, privateKey);

			return gxsMessageItem;
		}

		private void signMessage(GxsMessageItem gxsMessageItem, byte[] data, PrivateKey privateKey)
		{
			// TODO: needs to handle publish sign (I think it's for the circles)
			var signature = RSA.sign(data, privateKey);
			gxsMessageItem.setAuthorSignature(signature);
			// XXX: publish signature is missing (I think it's for the circles)
		}
	}
}
