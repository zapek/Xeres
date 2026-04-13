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

package io.xeres.app.xrs.service.forum;

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.forum.ForumMessageItemSummary;
import io.xeres.app.database.model.gxs.*;
import io.xeres.app.database.repository.GxsForumGroupRepository;
import io.xeres.app.database.repository.GxsForumMessageRepository;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.notification.forum.ForumNotificationService;
import io.xeres.app.xrs.common.CommentMessageItem;
import io.xeres.app.xrs.common.VoteMessageItem;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.forum.item.ForumGroupItem;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.app.xrs.service.gxs.GxsAuthentication;
import io.xeres.app.xrs.service.gxs.GxsHelperService;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.gxs.item.GxsSyncMessageRequestItem;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.xeres.app.xrs.service.RsServiceType.GXS_FORUMS;
import static io.xeres.app.xrs.service.gxs.GxsAuthentication.Flags.CHILD_NEEDS_AUTHOR;
import static io.xeres.app.xrs.service.gxs.GxsAuthentication.Flags.ROOT_NEEDS_AUTHOR;

@Component
public class ForumRsService extends GxsRsService<ForumGroupItem, ForumMessageItem>
{
	private static final Duration SYNCHRONIZATION_INITIAL_DELAY = Duration.ofSeconds(30);
	private static final Duration SYNCHRONIZATION_DELAY = Duration.ofMinutes(1);

	private final GxsForumGroupRepository gxsForumGroupRepository;
	private final GxsForumMessageRepository gxsForumMessageRepository;
	private final GxsHelperService<ForumGroupItem, ForumMessageItem> gxsHelperService;
	private final DatabaseSessionManager databaseSessionManager;
	private final ForumNotificationService forumNotificationService;

	public ForumRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager, IdentityManager identityManager, GxsForumGroupRepository gxsForumGroupRepository, GxsForumMessageRepository gxsForumMessageRepository, GxsHelperService<ForumGroupItem, ForumMessageItem> gxsHelperService, ForumNotificationService forumNotificationService)
	{
		super(rsServiceRegistry, peerConnectionManager, gxsTransactionManager, databaseSessionManager, identityManager, gxsHelperService);
		this.gxsForumGroupRepository = gxsForumGroupRepository;
		this.gxsForumMessageRepository = gxsForumMessageRepository;
		this.gxsHelperService = gxsHelperService;
		this.databaseSessionManager = databaseSessionManager;
		this.forumNotificationService = forumNotificationService;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return GXS_FORUMS;
	}

	@Override
	protected GxsAuthentication getAuthentication()
	{
		return new GxsAuthentication.Builder()
				.withRequirements(EnumSet.of(ROOT_NEEDS_AUTHOR, CHILD_NEEDS_AUTHOR))
				.build();
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		super.initialize(peerConnection);
		peerConnection.scheduleWithFixedDelay(
				() -> syncMessages(peerConnection),
				SYNCHRONIZATION_INITIAL_DELAY.toSeconds(),
				SYNCHRONIZATION_DELAY.toSeconds(),
				TimeUnit.SECONDS
		);
	}

	@Override
	public void syncMessages(PeerConnection peerConnection)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			// Request new messages for all subscribed groups
			findAllSubscribedGroups().forEach(forumGroupItem -> {
				var gxsSyncMessageRequestItem = new GxsSyncMessageRequestItem(forumGroupItem.getGxsId(), gxsHelperService.getLastPeerMessagesUpdate(peerConnection.getLocation(), forumGroupItem.getGxsId(), getServiceType()), ChronoUnit.YEARS.getDuration());
				log.debug("Asking {} for new messages in {} since {} for {}", peerConnection, gxsSyncMessageRequestItem.getGroupId(), log.isDebugEnabled() ? Instant.ofEpochSecond(gxsSyncMessageRequestItem.getCreateSince()) : null, getServiceType());
				peerConnectionManager.writeItem(peerConnection, gxsSyncMessageRequestItem, this);
			});
		}
	}

	public void fixDuplicates()
	{
		findAllSubscribedGroups().forEach(forumGroupItem -> {
			gxsHelperService.fixHiddenMessages(forumGroupItem.getGxsId(), Instant.now().minus(Duration.ofDays(360))); // XXX: make the date range smaller... and move it somewhere else, perhaps
		});
	}

	@Override
	protected List<ForumGroupItem> onAvailableGroupListRequest(PeerConnection recipient, Instant since)
	{
		return findAllGroupsSubscribedAndPublishedSince(since);
	}

	@Override
	protected List<ForumGroupItem> onGroupListRequest(Set<GxsId> ids)
	{
		return findAllGroups(ids);
	}

	@Override
	protected Set<GxsId> onAvailableGroupListResponse(Map<GxsId, Instant> ids)
	{
		// We want new forums as well as updated ones
		var existingMap = findAllGroups(ids.keySet()).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, forumGroupItem -> forumGroupItem.getPublished().truncatedTo(ChronoUnit.SECONDS)));

		ids.entrySet().removeIf(gxsIdInstantEntry -> {
			var existing = existingMap.get(gxsIdInstantEntry.getKey());
			return existing != null && !gxsIdInstantEntry.getValue().isAfter(existing);
		});
		return ids.keySet();
	}

	@Override
	protected boolean onGroupReceived(ForumGroupItem item)
	{
		log.debug("Received {}, saving/updating...", item);
		return true;
	}

	@Override
	protected void onGroupsSaved(List<ForumGroupItem> items)
	{
		forumNotificationService.addOrUpdateGroups(items);
	}

	@Override
	protected List<ForumMessageItem> onPendingMessageListRequest(PeerConnection recipient, GxsId groupId, Instant since)
	{
		return findAllMessagesInGroupSince(groupId, since);
	}

	@Override
	protected List<? extends GxsMessageItem> onMessageListRequest(GxsId groupId, Set<MessageId> messageIds)
	{
		return findAllMessages(groupId, messageIds);
	}

	@Override
	protected List<MessageId> onMessageListResponse(GxsId groupId, Set<MessageId> messageIds)
	{
		var existing = findAllMessagesIncludingOlds(groupId, messageIds).stream()
				.map(GxsMessageItem::getMessageId)
				.collect(Collectors.toSet());

		messageIds.removeAll(existing);

		return messageIds.stream().toList();
	}

	@Override
	protected boolean onMessageReceived(ForumMessageItem item)
	{
		log.debug("Received message {}, saving...", item);
		return true;
	}

	@Override
	protected void onMessagesSaved(List<ForumMessageItem> items)
	{
		forumNotificationService.addOrUpdateMessages(items);
	}

	@Override
	protected boolean onCommentReceived(CommentMessageItem item)
	{
		return false;
	}

	@Override
	protected void onCommentsSaved(List<CommentMessageItem> items)
	{
		// Nothing to do
	}

	@Override
	protected boolean onVoteReceived(VoteMessageItem item)
	{
		return false;
	}

	@Override
	protected void onVotesSaved(List<VoteMessageItem> items)
	{
		// Nothing to do
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		super.handleItem(sender, item); // This is required for the @Transactional to work
	}

	@Transactional
	public void subscribeToForumGroup(long id)
	{
		var forumGroupItem = findById(id).orElseThrow();
		forumGroupItem.setSubscribed(true);
		gxsHelperService.setLastServiceGroupsUpdateNow(GXS_FORUMS);
		// We don't need to send a sync notify here because it's not urgent.
		// The peers will poll normally to show if there's a new group available.
	}

	@Transactional
	public void unsubscribeFromForumGroup(long id)
	{
		var forumGroupItem = findById(id).orElseThrow();
		forumGroupItem.setSubscribed(false);
	}

	public Optional<ForumGroupItem> findById(long id)
	{
		return gxsForumGroupRepository.findById(id);
	}

	public List<ForumGroupItem> findAllGroups()
	{
		return gxsForumGroupRepository.findAll();
	}

	public List<ForumGroupItem> findAllSubscribedGroups()
	{
		return gxsForumGroupRepository.findAllBySubscribedIsTrue();
	}

	public List<ForumGroupItem> findAllGroups(Set<GxsId> gxsIds)
	{
		return gxsForumGroupRepository.findAllByGxsIdIn(gxsIds);
	}

	public List<ForumGroupItem> findAllGroupsSubscribedAndPublishedSince(Instant since)
	{
		return gxsForumGroupRepository.findAllBySubscribedIsTrueAndPublishedAfter(since);
	}

	public List<ForumMessageItem> findAllMessagesInGroupSince(GxsId groupId, Instant since)
	{
		return gxsForumMessageRepository.findAllByGxsIdAndPublishedAfterAndHiddenFalse(groupId, since);
	}

	public List<ForumMessageItem> findAllMessages(GxsId groupId, Set<MessageId> messageIds)
	{
		return gxsForumMessageRepository.findAllByGxsIdAndMessageIdInAndHiddenFalse(groupId, messageIds);
	}

	public List<ForumMessageItem> findAllMessagesIncludingOlds(GxsId groupId, Set<MessageId> messageIds)
	{
		return gxsForumMessageRepository.findAllByGxsIdAndMessageIdIn(groupId, messageIds);
	}

	public List<ForumMessageItem> findAllMessages(long groupId, Set<MessageId> messageIds)
	{
		var forumGroup = gxsForumGroupRepository.findById(groupId).orElseThrow();
		return gxsForumMessageRepository.findAllByGxsIdAndMessageIdInAndHiddenFalse(forumGroup.getGxsId(), messageIds);
	}

	/**
	 * Finds all messages. Prefer the other variants as this one is slower.
	 *
	 * @param messageIds the list of message ids
	 * @return the messages
	 */
	public List<ForumMessageItem> findAllMessages(Set<MessageId> messageIds)
	{
		return gxsForumMessageRepository.findAllByMessageIdInAndHiddenFalse(messageIds);
	}

	public List<ForumMessageItem> findAllOldMessages(Set<MessageId> messageIds)
	{
		return gxsForumMessageRepository.findAllByMessageIdInAndHiddenTrue(messageIds);
	}

	public int getUnreadCount(long groupId)
	{
		var forumGroupItem = gxsForumGroupRepository.findById(groupId).orElseThrow();
		return gxsForumMessageRepository.countUnreadMessages(forumGroupItem.getGxsId());
	}

	@Transactional
	public Page<ForumMessageItemSummary> findAllMessagesSummary(long groupId, Pageable pageable)
	{
		var forumGroup = gxsForumGroupRepository.findById(groupId).orElseThrow();
		return gxsForumMessageRepository.findSummaryAllByGxsIdAndHiddenFalse(forumGroup.getGxsId(), pageable);
	}

	public ForumMessageItem findMessageById(long id)
	{
		return gxsForumMessageRepository.findById(id).orElseThrow();
	}

	private ForumMessageItem saveMessage(MessageBuilder messageBuilder)
	{
		var forumMessageItem = messageBuilder.build();

		forumMessageItem.setId(gxsForumMessageRepository.findByGxsIdAndMessageId(forumMessageItem.getGxsId(), forumMessageItem.getMessageId()).orElse(forumMessageItem).getId()); // XXX: not sure we should be able to overwrite a message. in which case is it correct? maybe throw?
		var savedMessage = gxsForumMessageRepository.save(forumMessageItem);
		markOriginalMessageAsHidden(List.of(savedMessage));
		var forumGroupItem = gxsForumGroupRepository.findByGxsId(forumMessageItem.getGxsId()).orElseThrow();
		forumGroupItem.setLastUpdated(Instant.now());
		gxsForumGroupRepository.save(forumGroupItem);
		return savedMessage;
	}

	@Transactional
	public long createForumGroup(GxsId identity, String name, String description)
	{
		var forumGroupItem = createGroup(name, false);
		forumGroupItem.setDescription(description);

		if (identity != null)
		{
			forumGroupItem.setAuthorId(identity);
		}

		forumGroupItem.setCircleType(GxsCircleType.PUBLIC); // XXX: I think...
		forumGroupItem.setSignatureFlags(Set.of(GxsSignatureFlags.NONE_REQUIRED, GxsSignatureFlags.AUTHENTICATION_REQUIRED));
		forumGroupItem.setDiffusionFlags(EnumSet.of(GxsPrivacyFlags.PUBLIC));

		// XXX: set list of moderators

		forumGroupItem.setSubscribed(true);

		forumGroupItem = saveForum(forumGroupItem);

		forumNotificationService.addOrUpdateGroups(List.of(forumGroupItem));

		return forumGroupItem.getId();
	}

	@Transactional
	public void updateForumGroup(long groupId, String name, String description)
	{
		var forumGroupItem = gxsForumGroupRepository.findById(groupId).orElseThrow();
		forumGroupItem.setName(name);
		forumGroupItem.setDescription(description);

		forumGroupItem = saveForum(forumGroupItem);
		forumNotificationService.addOrUpdateGroups(List.of(forumGroupItem));
	}

	private ForumGroupItem saveForum(ForumGroupItem forumGroupItem)
	{
		signGroupIfNeeded(forumGroupItem);
		var savedForum = gxsForumGroupRepository.save(forumGroupItem);
		gxsHelperService.setLastServiceGroupsUpdateNow(GXS_FORUMS);
		peerConnectionManager.doForAllPeers(this::sendSyncNotification, this);
		return savedForum;
	}

	@Transactional
	public long createForumMessage(IdentityGroupItem author, long forumId, String title, String content, long parentId, long originalId)
	{
		// XXX: check the size, like createBoardMessage()
		var group = gxsForumGroupRepository.findById(forumId).orElseThrow();

		var builder = new MessageBuilder(group, author, title);

		if (parentId != 0L)
		{
			builder.parentId(gxsForumMessageRepository.findById(parentId).orElseThrow().getMessageId());
		}

		if (originalId != 0L)
		{
			builder.originalMessageId(gxsForumMessageRepository.findById(originalId).orElseThrow().getMessageId());
		}

		builder.getMessageItem().setContent(content);

		var forumMessageItem = saveMessage(builder);

		forumNotificationService.addOrUpdateMessages(List.of(forumMessageItem));

		peerConnectionManager.doForAllPeers(this::sendSyncNotification, this);

		return forumMessageItem.getId();
	}

	@Transactional
	public void setMessagesReadState(Map<Long, Boolean> messageMap)
	{
		gxsForumMessageRepository.findAllById(messageMap.keySet()).forEach(forumMessageItem -> forumMessageItem.setRead(messageMap.get(forumMessageItem.getId())));
		forumNotificationService.setMessagesReadState(messageMap);
	}

	@Transactional
	public void setAllGroupMessagesReadState(long groupId, boolean read)
	{
		var group = gxsForumGroupRepository.findById(groupId).orElseThrow();
		gxsForumMessageRepository.setAllGroupMessagesReadState(group.getGxsId(), read);
		forumNotificationService.setGroupMessagesReadState(groupId, read);
	}

	@Override
	public void shutdown()
	{
		forumNotificationService.shutdown();
	}
}
