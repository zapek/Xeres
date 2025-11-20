/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.board;

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.gxs.*;
import io.xeres.app.database.repository.GxsBoardGroupRepository;
import io.xeres.app.database.repository.GxsBoardMessageRepository;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.notification.board.BoardNotificationService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.board.item.BoardGroupItem;
import io.xeres.app.xrs.service.board.item.BoardMessageItem;
import io.xeres.app.xrs.service.gxs.AuthenticationRequirements;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.xeres.app.xrs.service.RsServiceType.POSTED;
import static io.xeres.app.xrs.service.gxs.AuthenticationRequirements.Flags.*;

@Component
public class BoardRsService extends GxsRsService<BoardGroupItem, BoardMessageItem>
{
	private static final Duration SYNCHRONIZATION_INITIAL_DELAY = Duration.ofSeconds(60);
	private static final Duration SYNCHRONIZATION_DELAY = Duration.ofMinutes(1);

	private final GxsBoardGroupRepository gxsBoardGroupRepository;
	private final GxsBoardMessageRepository gxsBoardMessageRepository;
	private final GxsUpdateService<BoardGroupItem, BoardMessageItem> gxsUpdateService;
	private final DatabaseSessionManager databaseSessionManager;
	private final BoardNotificationService boardNotificationService;

	public BoardRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, GxsBoardGroupRepository gxsBoardGroupRepository, DatabaseSessionManager databaseSessionManager, IdentityManager identityManager, GxsBoardMessageRepository gxsBoardMessageRepository, GxsUpdateService<BoardGroupItem, BoardMessageItem> gxsUpdateService, BoardNotificationService boardNotificationService)
	{
		super(rsServiceRegistry, peerConnectionManager, gxsTransactionManager, databaseSessionManager, identityManager, gxsUpdateService);
		this.gxsBoardGroupRepository = gxsBoardGroupRepository;
		this.gxsBoardMessageRepository = gxsBoardMessageRepository;
		this.gxsUpdateService = gxsUpdateService;
		this.databaseSessionManager = databaseSessionManager;
		this.boardNotificationService = boardNotificationService;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return POSTED;
	}

	@Override
	protected AuthenticationRequirements getAuthenticationRequirements()
	{
		return new AuthenticationRequirements.Builder()
				.withPublic(EnumSet.of(ROOT_AUTHOR, CHILD_AUTHOR))
				.withRestricted(EnumSet.of(ROOT_PUBLISH, CHILD_PUBLISH))
				.withPrivate(EnumSet.of(ROOT_PUBLISH, CHILD_PUBLISH))
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
	protected void syncMessages(PeerConnection recipient)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			// Request new messages for all subscribed groups
			findAllSubscribedGroups().forEach(boardGroupItem -> {
				var gxsSyncMessageRequestItem = new GxsSyncMessageRequestItem(boardGroupItem.getGxsId(), gxsUpdateService.getLastPeerMessagesUpdate(recipient.getLocation(), boardGroupItem.getGxsId(), getServiceType()), ChronoUnit.YEARS.getDuration());
				log.debug("Asking {} for new messages in {} since {} for {}", recipient, gxsSyncMessageRequestItem.getGroupId(), log.isDebugEnabled() ? Instant.ofEpochSecond(gxsSyncMessageRequestItem.getCreateSince()) : null, getServiceType());
				peerConnectionManager.writeItem(recipient, gxsSyncMessageRequestItem, this);
			});
		}
	}

	// XXX: don't forget about the comments and votes!

	// XXX: also beware, other users cannot write messages on a group we own (well, they can actually... I'm thinking of channels!)

	@Override
	protected List<BoardGroupItem> onAvailableGroupListRequest(PeerConnection recipient, Instant since)
	{
		return findAllGroupsSubscribedAndPublishedSince(since);
	}

	@Override
	protected List<BoardGroupItem> onGroupListRequest(Set<GxsId> ids)
	{
		return findAllGroups(ids);
	}

	@Override
	protected Set<GxsId> onAvailableGroupListResponse(Map<GxsId, Instant> ids)
	{
		// We want new boards as well as updated ones
		var existingMap = findAllGroups(ids.keySet()).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, boardGroupItem -> boardGroupItem.getPublished().truncatedTo(ChronoUnit.SECONDS)));

		ids.entrySet().removeIf(gxsIdInstantEntry -> {
			var existing = existingMap.get(gxsIdInstantEntry.getKey());
			return existing != null && !gxsIdInstantEntry.getValue().isAfter(existing);
		});
		return ids.keySet();
	}

	@Override
	protected boolean onGroupReceived(BoardGroupItem item)
	{
		log.debug("Received {}, saving/updating...", item);
		return true;
	}

	@Override
	protected void onGroupsSaved(List<BoardGroupItem> items)
	{
		//boardNotificationService.addBoardGroups(items); XXX
	}

	@Override
	protected List<BoardMessageItem> onPendingMessageListRequest(PeerConnection recipient, GxsId groupId, Instant since)
	{
		return findAllMessagesInGroupSince(groupId, since);
	}

	@Override
	protected List<BoardMessageItem> onMessageListRequest(GxsId groupId, Set<MessageId> messageIds)
	{
		// XXX: as well as comments!
		return findAllMessages(groupId, messageIds);
	}

	@Override
	protected List<MessageId> onMessageListResponse(GxsId groupId, Set<MessageId> messageIds)
	{
		// XXX: comments too?
		var existing = findAllMessages(groupId, messageIds).stream()
				.map(GxsMessageItem::getMessageId)
				.collect(Collectors.toSet());

		messageIds.removeAll(existing);

		return messageIds.stream().toList();
	}

	@Override
	protected boolean onMessageReceived(BoardMessageItem item)
	{
		log.debug("Received message {}, saving...", item);
		return true;
	}

	@Override
	protected void onMessagesSaved(List<BoardMessageItem> items)
	{
		// XXX
		//boardNotificationService.addForumMessages(items);
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		super.handleItem(sender, item); // This is required for the @Transactional to work
	}

	public Optional<BoardGroupItem> findById(long id)
	{
		return gxsBoardGroupRepository.findById(id);
	}

	public List<BoardGroupItem> findAllGroups()
	{
		return gxsBoardGroupRepository.findAll();
	}

	public List<BoardGroupItem> findAllSubscribedGroups()
	{
		return gxsBoardGroupRepository.findAllBySubscribedIsTrue();
	}

	public List<BoardGroupItem> findAllGroups(Set<GxsId> gxsIds)
	{
		return gxsBoardGroupRepository.findAllByGxsIdIn(gxsIds);
	}

	public List<BoardGroupItem> findAllGroupsSubscribedAndPublishedSince(Instant since)
	{
		return gxsBoardGroupRepository.findAllBySubscribedIsTrueAndPublishedAfter(since);
	}

	public List<BoardMessageItem> findAllMessagesInGroupSince(GxsId groupId, Instant since)
	{
		return gxsBoardMessageRepository.findAllByGxsIdAndPublishedAfter(groupId, since);
	}

	public List<BoardMessageItem> findAllMessages(GxsId groupId, Set<MessageId> messageIds)
	{
		return gxsBoardMessageRepository.findAllByGxsIdAndMessageIdIn(groupId, messageIds);
	}

	public List<BoardMessageItem> findAllMessages(long groupId, Set<MessageId> messageIds)
	{
		var forumGroup = gxsBoardGroupRepository.findById(groupId).orElseThrow();
		return gxsBoardMessageRepository.findAllByGxsIdAndMessageIdIn(forumGroup.getGxsId(), messageIds);
	}

	public int getUnreadCount(long groupId)
	{
		var forumGroupItem = gxsBoardGroupRepository.findById(groupId).orElseThrow();
		return gxsBoardMessageRepository.countUnreadMessages(forumGroupItem.getGxsId());
	}

	@Transactional
	public long createBoardGroup(GxsId identity, String name, String description)
	{
		var boardGroupItem = createGroup(name);
		boardGroupItem.setDescription(description);

		if (identity != null)
		{
			boardGroupItem.setAuthor(identity);
		}

		boardGroupItem.setCircleType(GxsCircleType.PUBLIC); // XXX: implement "YOUR_FRIENDS_ONLY"? but based on trust instead
		boardGroupItem.setSignatureFlags(Set.of(GxsSignatureFlags.NONE_REQUIRED, GxsSignatureFlags.AUTHENTICATION_REQUIRED));
		boardGroupItem.setDiffusionFlags(EnumSet.of(GxsPrivacyFlags.PUBLIC));

		//boardGroupItem.setInternalCircle(); XXX: needs that for "YOUR_FRIENDS_ONLY". check what RS does for createBoardV2(), how it is called

		boardGroupItem.setSubscribed(true);

		var savedBoardId = saveBoard(boardGroupItem).getId();

		boardGroupItem.setId(savedBoardId);
		boardNotificationService.addBoardGroups(List.of(boardGroupItem));

		return savedBoardId;
	}

	@Transactional
	public BoardGroupItem saveBoard(BoardGroupItem boardGroupItem)
	{
		signGroupIfNeeded(boardGroupItem);
		var savedForum = gxsBoardGroupRepository.save(boardGroupItem);
		gxsUpdateService.setLastServiceGroupsUpdateNow(POSTED);
		peerConnectionManager.doForAllPeers(this::sendSyncNotification, this);
		return savedForum;
	}
}
