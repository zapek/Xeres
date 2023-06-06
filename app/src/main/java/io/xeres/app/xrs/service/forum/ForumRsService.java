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

package io.xeres.app.xrs.service.forum;

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.forum.ForumMessageItemSummary;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.database.repository.GxsForumGroupRepository;
import io.xeres.app.database.repository.GxsForumMessageRepository;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.forum.item.ForumGroupItem;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.gxs.GxsUpdateService;
import io.xeres.app.xrs.service.gxs.item.GxsSyncMessageRequestItem;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.xeres.app.xrs.service.RsServiceType.FORUMS;

@Component
public class ForumRsService extends GxsRsService<ForumGroupItem, ForumMessageItem>
{
	private static final Duration SYNCHRONIZATION_INITIAL_DELAY = Duration.ofSeconds(30);
	private static final Duration SYNCHRONIZATION_DELAY = Duration.ofMinutes(1);

	private final GxsForumGroupRepository gxsForumGroupRepository;
	private final GxsForumMessageRepository gxsForumMessageRepository;
	private final GxsUpdateService<ForumGroupItem> gxsUpdateService;
	private final DatabaseSessionManager databaseSessionManager;

	public ForumRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager, IdentityManager identityManager, GxsForumGroupRepository gxsForumGroupRepository, GxsForumMessageRepository gxsForumMessageRepository, GxsUpdateService<ForumGroupItem> gxsUpdateService)
	{
		super(rsServiceRegistry, peerConnectionManager, gxsTransactionManager, databaseSessionManager, identityManager, gxsUpdateService);
		this.gxsForumGroupRepository = gxsForumGroupRepository;
		this.gxsForumMessageRepository = gxsForumMessageRepository;
		this.gxsUpdateService = gxsUpdateService;
		this.databaseSessionManager = databaseSessionManager;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return FORUMS;
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

	private void syncMessages(PeerConnection peerConnection)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			// Request new messages for all subscribed groups
			findAllSubscribedGroups().forEach(forumGroupItem -> {
				var gxsSyncMessageRequestItem = new GxsSyncMessageRequestItem(forumGroupItem.getGxsId(), gxsUpdateService.getLastPeerMessagesUpdate(peerConnection.getLocation(), forumGroupItem.getGxsId(), getServiceType()));
				log.debug("Asking peer {} for new messages in group {} since {} for service {}", peerConnection, gxsSyncMessageRequestItem.getGroupId(), gxsSyncMessageRequestItem.getLastUpdated(), getServiceType());
				peerConnectionManager.writeItem(peerConnection, gxsSyncMessageRequestItem, this);
			});
		}
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
		log.debug("Received group {}, saving/updating...", item);
		return true;
	}

	@Override
	protected List<ForumMessageItem> onPendingMessageListRequest(PeerConnection recipient, GxsId groupId, Instant since)
	{
		return findAllMessagesInGroupSince(groupId, since);
	}

	@Override
	protected List<ForumMessageItem> onMessageListRequest(GxsId groupId, Set<MessageId> messageIds)
	{
		return findAllMessages(groupId, messageIds);
	}

	@Override
	protected List<MessageId> onMessageListResponse(GxsId groupId, Set<MessageId> messageIds)
	{
		var existing = findAllMessages(groupId, messageIds).stream()
				.map(GxsMessageItem::getMessageId)
				.collect(Collectors.toSet());

		messageIds.removeIf(existing::contains);

		return messageIds.stream().toList();
	}

	@Override
	protected void onMessageReceived(PeerConnection sender, ForumMessageItem item)
	{
		log.debug("Received message {}, saving...", item);
		save(item);
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
		// XXX: setLastServiceUpdate()? how will the client know there's new groups available otherwise?
		var forumGroupItem = findById(id).orElseThrow();
		forumGroupItem.setSubscribed(true);
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

	public void save(ForumGroupItem forumGroupItem)
	{
		gxsForumGroupRepository.save(forumGroupItem);
			// XXX: setLastServiceUpdate() ! (though, it seems to work already?) and I also should do it for messages
	}

	public List<ForumMessageItem> findAllMessagesInGroupSince(GxsId groupId, Instant since)
	{
		return gxsForumMessageRepository.findAllByGxsIdAndPublishedAfter(groupId, since);
	}

	public List<ForumMessageItem> findAllMessages(GxsId groupId, Set<MessageId> messageIds)
	{
		return gxsForumMessageRepository.findAllByGxsIdAndMessageIdIn(groupId, messageIds);
	}

	@Transactional
	public List<ForumMessageItemSummary> findAllMessagesSummary(long groupId)
	{
		var forumGroup = gxsForumGroupRepository.findById(groupId).orElseThrow();
		return gxsForumMessageRepository.findSummaryAllByGxsId(forumGroup.getGxsId());
	}

	public ForumMessageItem findMessageById(long id)
	{
		return gxsForumMessageRepository.findById(id).orElseThrow();
	}

	@Transactional
	public void save(ForumMessageItem forumMessageItem)
	{
		forumMessageItem.setId(gxsForumMessageRepository.findByGxsIdAndMessageId(forumMessageItem.getGxsId(), forumMessageItem.getMessageId()).orElse(forumMessageItem).getId()); // XXX: not sure we should be able to overwrite a message. in which case is it correct? maybe throw?
		gxsForumMessageRepository.save(forumMessageItem);
		// XXX: setLastServiceUpdate() ? I think so actually!
	}
}
