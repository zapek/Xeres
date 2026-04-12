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

package io.xeres.app.xrs.service.gxs;

import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.gxs.GxsCircleType;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.common.CommentMessageItem;
import io.xeres.app.xrs.common.VoteMessageItem;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemUtils;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxs.item.*;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import io.xeres.common.util.ExecutorUtils;
import io.xeres.common.util.NoSuppressedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static io.xeres.app.net.peer.PeerConnection.KEY_GXS_TRANSACTION_ID;
import static io.xeres.app.xrs.service.gxs.GxsAuthentication.Flags.*;
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
	private static final int KEY_LAST_SYNC_REQUEST = 1;

	/**
	 * When to perform synchronization run with a peer.
	 */
	private static final Duration SYNCHRONIZATION_DELAY_INITIAL_MIN = Duration.ofSeconds(10);
	private static final Duration SYNCHRONIZATION_DELAY_INITIAL_MAX = Duration.ofSeconds(15);
	private static final Duration SYNCHRONIZATION_DELAY = Duration.ofMinutes(1);

	private static final int MESSAGES_PER_TRANSACTIONS = 20;

	private static final Duration PENDING_VERIFICATION_MAX = Duration.ofMinutes(1);
	private static final Duration PENDING_VERIFICATION_DELAY = Duration.ofSeconds(10);

	private static final Duration GROUP_STATISTICS_DELAY = Duration.ofMinutes(2);

	private Instant lastGroupStatistics = Instant.EPOCH;

	protected final GxsTransactionManager gxsTransactionManager;
	protected final PeerConnectionManager peerConnectionManager;
	private final IdentityManager identityManager;
	private final GxsUpdateService<G, M> gxsUpdateService;
	private final DatabaseSessionManager databaseSessionManager;

	private final Class<G> itemGroupClass;
	private final Class<M> itemMessageClass;

	private ScheduledExecutorService executorService;

	private final Map<G, Long> pendingGxsGroups = new ConcurrentHashMap<>();
	private final Map<GxsMessageItem, Long> pendingGxsMessages = new ConcurrentHashMap<>();

	private final Set<GxsId> ongoingGxsMessageTransfers = ConcurrentHashMap.newKeySet();

	private final GxsAuthentication gxsAuthentication;

	@SuppressWarnings("unchecked")
	protected GxsRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager, IdentityManager identityManager, GxsUpdateService<G, M> gxsUpdateService)
	{
		super(rsServiceRegistry);
		this.gxsTransactionManager = gxsTransactionManager;
		this.peerConnectionManager = peerConnectionManager;
		this.databaseSessionManager = databaseSessionManager;
		this.identityManager = identityManager;
		this.gxsUpdateService = gxsUpdateService;

		// Type information is available when subclassing a class using a generic type, which means itemClass is the class of G
		itemGroupClass = (Class<G>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		itemMessageClass = (Class<M>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1]; // and M

		gxsAuthentication = Objects.requireNonNull(getAuthentication(), "Authentication cannot be null");
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
	protected abstract List<? extends GxsMessageItem> onMessageListRequest(GxsId groupId, Set<MessageId> messageIds);

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
	 * @return true if we want to save it
	 */
	protected abstract boolean onMessageReceived(M item);

	/**
	 * Called when the messages have been saved.
	 *
	 * @param items the list of saved messages
	 */
	protected abstract void onMessagesSaved(List<M> items);

	/**
	 * Called when a comment has been received.
	 *
	 * @param item the received comment
	 * @return true if we want to save it
	 */
	protected abstract boolean onCommentReceived(CommentMessageItem item);

	/**
	 * Called when the comments have been saved.
	 *
	 * @param items the list of saved comments
	 */
	protected abstract void onCommentsSaved(List<CommentMessageItem> items);

	/**
	 * Called when a vote has been received.
	 *
	 * @param item the received vote
	 * @return true if we want to save it
	 */
	protected abstract boolean onVoteReceived(VoteMessageItem item);

	/**
	 * Called when the votes have been saved.
	 *
	 * @param items the list of saved votes
	 */
	protected abstract void onVotesSaved(List<VoteMessageItem> items);

	/**
	 * Called to gather the authentication requirements for the service.
	 *
	 * @return the authentication requirements
	 */
	protected abstract GxsAuthentication getAuthentication();

	/**
	 * Called periodically (normally each minute, or when receiving a {@link GxsSyncNotifyItem}) to sync messages.
	 *
	 * @param recipient the peer to sync messages with
	 */
	protected abstract void syncMessages(PeerConnection recipient);

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
		executorService = ExecutorUtils.createFixedRateExecutor(this::manageAll,
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
				case GxsSyncGroupStatsItem gxsSyncGroupStatsItem -> handleGxsSyncGroupStats(sender, gxsSyncGroupStatsItem);
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
		var lastSync = (Instant) peerConnection.getServiceData(this, KEY_LAST_SYNC_REQUEST).orElse(Instant.EPOCH);
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
		peerConnection.putServiceData(this, KEY_LAST_SYNC_REQUEST, Instant.now());
	}

	private void manageAll()
	{
		checkPendingGroupsAndMessages();
		askGroupStatisticsIfNeeded();
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

	private void askGroupStatisticsIfNeeded()
	{
		var now = Instant.now();
		if (Duration.between(lastGroupStatistics, now).compareTo(GROUP_STATISTICS_DELAY) <= 0)
		{
			return;
		}
		lastGroupStatistics = now;

		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			var randomPeer = peerConnectionManager.getRandomPeer();
			if (randomPeer != null)
			{
				var ids = gxsUpdateService.findGroupsToRequestStats(now, GROUP_STATISTICS_DELAY);
				ids.forEach(gxsId -> peerConnectionManager.writeItem(randomPeer, new GxsSyncGroupStatsItem(RequestType.REQUEST, gxsId), this));
			}
		}
	}

	private void handleGxsSyncNotifyItem(PeerConnection peerConnection, GxsSyncNotifyItem item)
	{
		log.debug("Got sync notify {} from {}", item, peerConnection);

		syncNow(peerConnection);
		syncMessages(peerConnection);
	}

	private void handleGxsSyncGroupStats(PeerConnection peerConnection, GxsSyncGroupStatsItem item)
	{
		log.debug("Got group stat {} from {}", item, peerConnection);

		if (item.getRequestType() == RequestType.REQUEST)
		{
			gxsUpdateService.findGroupStatsByGxsId(item.getGroupId())
					.ifPresent(gxsSyncGroupStatsItem -> peerConnectionManager.writeItem(peerConnection, gxsSyncGroupStatsItem, this));
		}
		else if (item.getRequestType() == RequestType.RESPONSE)
		{
			gxsUpdateService.updateGroupStats(item);
		}
	}

	protected void sendSyncNotification(PeerConnection peerConnection)
	{
		CompletableFuture.runAsync((NoSuppressedRunnable) () -> {
			try
			{
				TimeUnit.SECONDS.sleep(1);
			}
			catch (InterruptedException _)
			{
				Thread.currentThread().interrupt();
				return;
			}

			var gxsSyncNotifyItem = new GxsSyncNotifyItem();
			log.debug("Sending sync notification to {}", peerConnection);
			peerConnectionManager.writeItem(peerConnection, gxsSyncNotifyItem, this);
		});
	}

	private void handleGxsSyncGroupRequestItem(PeerConnection peerConnection, GxsSyncGroupRequestItem item)
	{
		log.debug("{} sent group {}", peerConnection, item);

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

			log.debug("Calling transaction for group, number of items: {}", items.size());
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
		log.debug("{} sent message {}", peerConnection, item);

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

			log.debug("Calling transaction for message, number of items: {}", items.size());
			gxsTransactionManager.startOutgoingTransactionForMessageIdResponse(
					peerConnection,
					items,
					gxsUpdateService.getLastServiceGroupsUpdate(getServiceType()), // XXX: not sure that's correct
					transactionId,
					this
			);
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
		// The transaction id needs to be stored globally on the peer connection as multiple services can use them
		var transactionId = (int) peerConnection.getPeerData(KEY_GXS_TRANSACTION_ID).orElse(0) + 1;
		peerConnection.putPeerData(KEY_GXS_TRANSACTION_ID, transactionId);
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
			log.debug("Peer requested unavailable group {}", groupId); // Switched severity do debug instead of warn because RS seems to request without checking
			return false;
		}

		var group = groupList.getFirst();
		return group.isSubscribed() &&
				group.getLastUpdated() != null &&
				lastPeerUpdate.isBefore(group.getLastUpdated()) &&
				group.getLastUpdated().isAfter(since);
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
			log.debug("{} has no items in the transaction", peerConnection);
			return; // nothing to do
		}

		if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_GROUP_LIST_RESPONSE))
		{
			@SuppressWarnings("unchecked")
			var gxsIdsMap = ((List<GxsSyncGroupItem>) transaction.getItems()).stream()
					.collect(toMap(GxsSyncGroupItem::getGroupId, gxsSyncGroupItem -> Instant.ofEpochSecond(gxsSyncGroupItem.getPublishTimestamp())));
			log.debug("{} has the following group ids (new or updates) for us (total: {}): {} ...", peerConnection, gxsIdsMap.size(), gxsIdsMap.keySet().stream().limit(10).toList());
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
			var messagesWanted = onMessageListResponse(groupId, messageIds);
			requestGxsMessages(peerConnection, groupId, messagesWanted);
			if (messagesWanted.isEmpty())
			{
				// If there was no message, it means we got them all already (from another peer, etc...). We can set the timestamp.
				setLastMessageUpdate(peerConnection, groupId, transaction.getUpdated());
			}
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
			// This contains the message items, the votes and the comments
			@SuppressWarnings("unchecked")
			var gxsMessageItems = ((List<GxsTransferMessageItem>) transaction.getItems()).stream()
					.map(this::convertTransferGroupToGxsMessage)
					.sorted(Comparator.comparing(GxsMessageItem::getPublished)) // Get older message first to facilitate marking messages as edited
					.toList();

			verifyAndStoreMessages(peerConnection, gxsMessageItems);
			if (!gxsMessageItems.isEmpty())
			{
				var group = gxsMessageItems.getFirst().getGxsId();

				log.debug("{} sent messages for group {}", peerConnection, group);
				if (!ongoingGxsMessageTransfers.contains(group))
				{
					// If there's no more ongoing transfer for those messages, we can mark them as finished.
					setLastMessageUpdate(peerConnection, group, transaction.getUpdated());
				}
			}
		}
		else
		{
			log.debug("Unknown transaction {}", transaction);
		}
	}

	private void setLastMessageUpdate(PeerConnection peerConnection, GxsId group, Instant when)
	{
		gxsUpdateService.setLastPeerMessageUpdate(peerConnection.getLocation(), group, when, getServiceType());
		peerConnectionManager.doForAllPeersExceptSender(this::sendSyncNotification, peerConnection, this);
	}

	private void verifyAndStoreGroups(PeerConnection peerConnection, Collection<G> groups)
	{
		List<G> savedGroups = new ArrayList<>(groups.size());

		for (var group : groups)
		{
			var data = ItemUtils.serializeItemForSignature(group, this);
			var validation = verifyGroupAdmin(group, data);

			// Validate author signature, if needed
			if (validation == VerificationStatus.OK && (group.getAuthorId() != null || gxsAuthentication.isAuthorSigningGroups()))
			{
				if (group.getAuthorId() == null)
				{
					log.warn("Failed to validate group {}: missing author id", group);
					continue;
				}

				validation = verifyGroupAuthor(peerConnection, group, data);
				if (validation == VerificationStatus.DELAYED)
				{
					continue;
				}
			}

			// If this is a group update, validate its admin signature using the public key we already have
			if (validation == VerificationStatus.OK)
			{
				validation = verifyGroupForUpdate(peerConnection, group, data);
			}

			// Save the group if everything is OK
			if (validation == VerificationStatus.OK)
			{
				gxsUpdateService.saveGroup(group, this::onGroupReceived).ifPresent(savedGroups::add);
			}

			// If the group verification was delayed, remove it
			pendingGxsGroups.computeIfPresent(group, (_, _) -> -1L);
		}

		if (!savedGroups.isEmpty())
		{
			onGroupsSaved(savedGroups);
		}
	}

	private VerificationStatus verifyGroupAdmin(G group, byte[] data)
	{
		var adminPublicKey = group.getAdminPublicKey();
		if (adminPublicKey == null)
		{
			log.warn("Failed to validate group {}: missing admin key", group);
			return VerificationStatus.FAILED;
		}

		var adminSignature = group.getAdminSignature();
		if (adminSignature == null)
		{
			log.warn("Failed to validate group {}: missing admin signature", group);
			return VerificationStatus.FAILED;
		}

		if (!RSA.verify(adminPublicKey, adminSignature, data))
		{
			log.warn("Failed to validate group {}: wrong admin signature", group);
			return VerificationStatus.FAILED;
		}
		return VerificationStatus.OK;
	}

	private VerificationStatus verifyGroupAuthor(PeerConnection peerConnection, G gxsGroupItem, byte[] data)
	{
		if (gxsGroupItem.getAuthorSignature() == null)
		{
			log.warn("Missing author signature for group {}", gxsGroupItem);
			return VerificationStatus.FAILED;
		}

		var authorIdentity = identityManager.getGxsGroup(peerConnection, gxsGroupItem.getAuthorId());
		if (authorIdentity == null)
		{
			log.warn("Delaying verification of group {} (author: {})", gxsGroupItem, gxsGroupItem.getAuthorId());
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
			var authorAdminPublicKey = authorIdentity.getAdminPublicKey();
			if (authorAdminPublicKey == null)
			{
				log.warn("Failed to validate group {}: missing author admin key", gxsGroupItem);
				return VerificationStatus.FAILED;
			}

			var authorSignature = gxsGroupItem.getAuthorSignature();
			if (authorSignature == null)
			{
				log.warn("Missing author signature for group {}", gxsGroupItem);
				return VerificationStatus.FAILED;
			}

			if (RSA.verify(authorAdminPublicKey, authorSignature, data))
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

	private VerificationStatus verifyGroupForUpdate(PeerConnection peerConnection, G group, byte[] data)
	{
		var existingGroup = gxsUpdateService.getExistingGroup(group);
		if (existingGroup.isPresent())
		{
			var oldGroup = existingGroup.get();
			// Validate the new group using the old key to certify this is an upgrade
			var existingAdminPublicKey = oldGroup.getAdminPublicKey();
			if (!group.getPublished().isAfter(oldGroup.getPublished()))
			{
				log.warn("Failed to validate group {} for update: new group timestamp {} <= old group timestamp {}", group.getPublished(), oldGroup.getPublished(), group.getPublished());
				return VerificationStatus.FAILED;
			}

			if (RSA.verify(existingAdminPublicKey, group.getAdminSignature(), data))
			{
				// Copy the fields we want to retain.
				group.retainValues(oldGroup);
				// XXX: private keys? do we have groups with private keys? update should not replace them but keep the old ones
				if (group.getCircleType() == GxsCircleType.YOUR_FRIENDS_ONLY)
				{
					group.setOriginator(peerConnection.getLocation().getLocationIdentifier());
				}
				return VerificationStatus.OK;
			}
			else
			{
				if (isSameKey(existingAdminPublicKey, group.getAdminPublicKey()))
				{
					log.warn("Failed to validate group {} for update: wrong admin signature", group);
				}
				else
				{
					log.warn("Failed to validate group {} for update: new public key doesn't match the old one", group);
				}
				return VerificationStatus.FAILED;
			}
		}
		else
		{
			group.setOriginator(peerConnection.getLocation().getLocationIdentifier());
			return VerificationStatus.OK;
		}
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
			gxsGroupItem = itemGroupClass.getDeclaredConstructor().newInstance();
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException _)
		{
			throw new IllegalArgumentException("Failed to instantiate " + itemGroupClass.getSimpleName() + " missing empty constructor?");
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

	private void verifyAndStoreMessages(PeerConnection peerConnection, Collection<GxsMessageItem> messages)
	{
		List<M> savedMessages = new ArrayList<>();
		List<CommentMessageItem> savedComments = new ArrayList<>();
		List<VoteMessageItem> savedVotes = new ArrayList<>();
		Instant lastPosted = Instant.EPOCH;

		for (var message : messages)
		{
			var data = ItemUtils.serializeItemForSignature(message, this);
			var validation = VerificationStatus.OK;

			if (gxsAuthentication.getRequirements().contains(message.isChild() ? CHILD_NEEDS_PUBLISH : ROOT_NEEDS_PUBLISH))
			{
				validation = verifyMessagePublish(message, data);
			}

			// Check requirements, but if the message has been signed anyway, we still need to validate it
			if (validation == VerificationStatus.OK && (message.hasAuthor() || gxsAuthentication.getRequirements().contains(message.isChild() ? CHILD_NEEDS_AUTHOR : ROOT_NEEDS_AUTHOR)))
			{
				validation = verifyMessageAuthor(peerConnection, message, data);
				if (validation == VerificationStatus.DELAYED)
				{
					continue;
				}
			}

			// Save the message if everything is OK
			if (validation == VerificationStatus.OK)
			{
				switch (message)
				{
					case CommentMessageItem commentMessageItem -> gxsUpdateService.saveComment(commentMessageItem, this::onCommentReceived).ifPresent(savedComments::add);
					case VoteMessageItem voteMessageItem -> gxsUpdateService.saveVote(voteMessageItem, this::onVoteReceived).ifPresent(savedVotes::add);
					default ->
						//noinspection unchecked
							gxsUpdateService.saveMessage((M) message, this::onMessageReceived).ifPresent(savedMessages::add);
				}
				if (message.getPublished().isAfter(lastPosted))
				{
					lastPosted = message.getPublished();
				}
			}

			// If the message verification was delayed, remove it
			pendingGxsMessages.computeIfPresent(message, (_, _) -> -1L);
		}

		if (!savedMessages.isEmpty())
		{
			markOriginalMessageAsHidden(savedMessages);
			onMessagesSaved(savedMessages);
		}
		if (!savedComments.isEmpty())
		{
			markOriginalMessageAsHidden(savedComments);
			onCommentsSaved(savedComments);
		}
		if (!savedVotes.isEmpty())
		{
			markOriginalMessageAsHidden(savedVotes);
			onVotesSaved(savedVotes);
		}

		if (!lastPosted.equals(Instant.EPOCH))
		{
			Instant finalLastPosted = lastPosted;
			messages.stream().findFirst().ifPresent(gxsMessageItem -> gxsUpdateService.updateLastPosted(gxsMessageItem.getGxsId(), finalLastPosted));
		}
	}

	protected void markOriginalMessageAsHidden(Collection<? extends GxsMessageItem> gxsMessageItems)
	{
		gxsMessageItems.forEach(gxsMessageItem -> {
			if (gxsMessageItem.getOriginalMessageId() != null && !gxsMessageItem.getOriginalMessageId().equals(gxsMessageItem.getMessageId()))
			{
				gxsUpdateService.overrideMessage(gxsMessageItem.getGxsId(), gxsMessageItem.getOriginalMessageId(), gxsMessageItem.getAuthorId());
			}
		});
	}

	private VerificationStatus verifyMessagePublish(GxsMessageItem message, byte[] data)
	{
		var group = gxsUpdateService.findGroupByGxsId(message.getGxsId());
		if (group == null)
		{
			log.warn("Failed to find group for message: {}, dropping", message);
			return VerificationStatus.FAILED;
		}
		var publicKey = group.getPublishPublicKey();
		if (publicKey == null)
		{
			log.warn("Failed to find group publish public key for message: {}, dropping", message);
			return VerificationStatus.FAILED;
		}
		var signature = message.getPublishSignature();
		if (signature == null)
		{
			log.warn("Missing publish signature for message: {}, dropping", message);
			return VerificationStatus.FAILED;
		}

		if (RSA.verify(publicKey, signature, data))
		{
			return VerificationStatus.OK;
		}
		else
		{
			log.warn("Failed to validate message {}: wrong publish signature", message);
			return VerificationStatus.FAILED;
		}
	}

	private VerificationStatus verifyMessageAuthor(PeerConnection peerConnection, GxsMessageItem message, byte[] data)
	{
		var signature = message.getAuthorSignature();
		if (signature == null)
		{
			log.warn("Missing author signature for message {}", message);
			return VerificationStatus.FAILED;
		}

		var authorIdentity = identityManager.getGxsGroup(peerConnection, message.getAuthorId());
		if (authorIdentity == null)
		{
			log.warn("Delaying verification of message {}", message);
			var existingDelay = pendingGxsMessages.putIfAbsent(message, PENDING_VERIFICATION_MAX.toSeconds());
			if (existingDelay != null)
			{
				var newDelay = existingDelay - PENDING_VERIFICATION_DELAY.toSeconds();
				pendingGxsMessages.put(message, newDelay);
				if (newDelay < 0)
				{
					log.warn("Failed to validate message {}: timeout exceeded", message);
				}
			}
			return VerificationStatus.DELAYED;
		}
		else
		{
			var publicKey = authorIdentity.getAdminPublicKey();
			if (publicKey == null)
			{
				log.warn("Failed to find author admin public key for message {}", message);
				return VerificationStatus.FAILED;
			}
			if (RSA.verify(publicKey, signature, data))
			{
				// XXX: check for reputation here, if reputation is too low, remove
				return VerificationStatus.OK;
			}
			else
			{
				log.warn("Failed to validate message {}: wrong author signature", message);
				return VerificationStatus.FAILED;
			}
		}
	}

	public void sendGxsMessages(PeerConnection peerConnection, List<? extends GxsMessageItem> gxsMessageItems)
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

		// Ask for MESSAGES_PER_TRANSACTIONS messages at a time. This is done to avoid
		// overflowing the peer's queue.
		var count = 0;
		for (var messageId : messageIds)
		{
			items.add(new GxsSyncMessageItem(GxsSyncMessageItem.REQUEST, groupId, messageId, transactionId));

			if (++count == MESSAGES_PER_TRANSACTIONS)
			{
				break;
			}
		}

		// Mark/unmark as ongoing transaction to make sure
		// we update the peer timestamp when needed.
		if (count == MESSAGES_PER_TRANSACTIONS && messageIds.size() > MESSAGES_PER_TRANSACTIONS)
		{
			ongoingGxsMessageTransfers.add(groupId);
		}
		else
		{
			ongoingGxsMessageTransfers.remove(groupId);
		}

		gxsTransactionManager.startOutgoingTransactionForMessageIdRequest(peerConnection, items, transactionId, this);
	}

	private M createGxsMessageItem()
	{
		M gxsMessageItem;

		try
		{
			gxsMessageItem = itemMessageClass.getDeclaredConstructor().newInstance();
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException _)
		{
			throw new IllegalArgumentException("Failed to instantiate " + itemMessageClass.getSimpleName() + " missing empty constructor?");
		}
		return gxsMessageItem;
	}

	private GxsMessageItem convertTransferGroupToGxsMessage(GxsTransferMessageItem fromItem)
	{
		var subType = fromItem.getMessageType();
		var toItem = switch (subType)
		{
			case CommentMessageItem.SUBTYPE -> new CommentMessageItem();
			case VoteMessageItem.SUBTYPE -> new VoteMessageItem();
			default -> createGxsMessageItem();
		};
		return fromItem.toGxsMessageItem(toItem);
	}

	protected G createGroup(String name, boolean needsPublish)
	{
		KeyPair adminKeyPair = RSA.generateKeys(GXS_KEY_SIZE);
		KeyPair publishKeyPair = null;
		if (needsPublish)
		{
			publishKeyPair = RSA.generateKeys(GXS_KEY_SIZE);
		}
		return createGroup(name, adminKeyPair, publishKeyPair);
	}

	protected G createGroup(String name, KeyPair adminKeyPair, KeyPair publishKeyPair)
	{
		var adminPrivateKey = (RSAPrivateKey) adminKeyPair.getPrivate();
		var adminPublicKey = (RSAPublicKey) adminKeyPair.getPublic();

		// The GxsId is from the public admin key (n and e)
		var gxsId = RSA.getGxsId(adminPublicKey);

		var gxsGroupItem = createGxsGroupItem();
		gxsGroupItem.setGxsId(gxsId);
		gxsGroupItem.setName(name);
		gxsGroupItem.updatePublished(); // Needs to be called before we set any key (validFrom is computed from it)
		gxsGroupItem.setAdminKeys(adminPrivateKey, adminPublicKey, gxsGroupItem.getPublished(), null);
		if (publishKeyPair != null)
		{
			var publishPrivateKey = (RSAPrivateKey) publishKeyPair.getPrivate();
			var publishPublicKey = (RSAPublicKey) publishKeyPair.getPublic();
			gxsGroupItem.setPublishKeys(RSA.getGxsId(publishPublicKey), publishPrivateKey, publishPublicKey, gxsGroupItem.getPublished(), null);
		}
		return gxsGroupItem;
	}

	protected void signGroupIfNeeded(GxsGroupItem group)
	{
		if (group.isExternal())
		{
			return; // Only sign our own groups
		}

		// Sign as admin
		var data = ItemUtils.serializeItemForSignature(group, this);
		var signature = RSA.sign(group.getAdminPrivateKey(), data);
		group.setAdminSignature(signature);

		if (group.getAuthorId() != null || gxsAuthentication.isAuthorSigningGroups())
		{
			if (group.getAuthorId() == null)
			{
				throw new IllegalArgumentException("Missing author id for signing group " + group);
			}
			var author = identityManager.getGxsGroup(group.getAuthorId());
			Objects.requireNonNull(author, "Couldn't get own identity. Shouldn't happen (tm)");
			var authorSignature = RSA.sign(author.getAdminPrivateKey(), data);
			group.setAuthorSignature(authorSignature);
		}
	}

	protected final class MessageBuilder
	{
		private final M gxsMessageItem;
		private final GxsGroupItem group;
		private final IdentityGroupItem author;

		public MessageBuilder(GxsGroupItem group, IdentityGroupItem author, String name)
		{
			this.group = group;
			this.author = author;
			gxsMessageItem = createGxsMessageItem();
			gxsMessageItem.setGxsId(group.getGxsId());
			gxsMessageItem.setName(name);
			if (author != null)
			{
				gxsMessageItem.setAuthorId(author.getGxsId());
			}
		}

		public MessageBuilder originalMessageId(MessageId originalMessageId)
		{
			Objects.requireNonNull(originalMessageId, "originalMessageId must not be null");
			gxsMessageItem.setOriginalMessageId(originalMessageId);
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
			gxsMessageItem.updatePublished();
			// XXX: serviceType? how? how does group do it?

			// The identifier is the sha1 hash of the data and meta (note: do not set any serialized fields after that call!)
			var data = ItemUtils.serializeItemForSignature(gxsMessageItem, GxsRsService.this);

			var md = new Sha1MessageDigest();
			md.update(data);
			gxsMessageItem.setMessageId(new MessageId(md.getBytes()));

			// The signature is performed afterwards
			signMessage(gxsMessageItem, data);

			return gxsMessageItem;
		}

		private void signMessage(GxsMessageItem message, byte[] data)
		{
			if (gxsAuthentication.getRequirements().contains(message.isChild() ? CHILD_NEEDS_PUBLISH : ROOT_NEEDS_PUBLISH))
			{
				var publishPrivateKey = group.getPublishPrivateKey();
				if (publishPrivateKey == null)
				{
					throw new IllegalArgumentException("Message " + message + " requires a publish key but there's none");
				}
				var signature = RSA.sign(publishPrivateKey, data);
				message.setPublishSignature(signature);
			}

			if (author != null || gxsAuthentication.getRequirements().contains(message.isChild() ? CHILD_NEEDS_AUTHOR : ROOT_NEEDS_AUTHOR))
			{
				if (author == null)
				{
					throw new IllegalArgumentException("Message " + message + " requires an author but there's none");
				}
				var signature = RSA.sign(author.getAdminPrivateKey(), data);
				message.setAuthorSignature(signature);
			}
		}
	}
}
