/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.channel;

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.gxs.*;
import io.xeres.app.database.repository.GxsBoardMessageRepository;
import io.xeres.app.database.repository.GxsChannelGroupRepository;
import io.xeres.app.database.repository.GxsChannelMessageRepository;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.notification.channel.ChannelNotificationService;
import io.xeres.app.util.GxsUtils;
import io.xeres.app.xrs.common.CommentMessageItem;
import io.xeres.app.xrs.common.VoteMessageItem;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.channel.item.ChannelGroupItem;
import io.xeres.app.xrs.service.channel.item.ChannelMessageItem;
import io.xeres.app.xrs.service.gxs.AuthenticationRequirements;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.gxs.GxsUpdateService;
import io.xeres.app.xrs.service.gxs.item.GxsSyncMessageRequestItem;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import io.xeres.common.util.image.ImageUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.xeres.app.util.RsUtils.replaceImageLines;
import static io.xeres.app.xrs.service.RsServiceType.CHANNELS;
import static io.xeres.app.xrs.service.gxs.AuthenticationRequirements.Flags.*;

@Component
public class ChannelRsService extends GxsRsService<ChannelGroupItem, ChannelMessageItem>
{
	private static final int IMAGE_GROUP_SIDE_SIZE = 128;

	private static final int IMAGE_MESSAGE_WIDTH = 320; // XXX: how much?! it's some aspect ratio thing, see below
	private static final int IMAGE_MESSAGE_HEIGHT = 240; // XXX: ditto...

	private static final Duration SYNCHRONIZATION_INITIAL_DELAY = Duration.ofSeconds(90);
	private static final Duration SYNCHRONIZATION_DELAY = Duration.ofMinutes(1);

	private final GxsChannelGroupRepository gxsChannelGroupRepository;
	private final GxsChannelMessageRepository gxsChannelMessageRepository;
	private final GxsUpdateService<ChannelGroupItem, ChannelMessageItem> gxsUpdateService;
	private final DatabaseSessionManager databaseSessionManager;
	private final ChannelNotificationService channelNotificationService;
	private final GxsBoardMessageRepository gxsBoardMessageRepository;

	public ChannelRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager, IdentityManager identityManager, GxsUpdateService<ChannelGroupItem, ChannelMessageItem> gxsUpdateService, GxsChannelGroupRepository gxsChannelGroupRepository, GxsChannelMessageRepository gxsChannelMessageRepository, GxsUpdateService<ChannelGroupItem, ChannelMessageItem> gxsUpdateService1, DatabaseSessionManager databaseSessionManager1, ChannelNotificationService channelNotificationService, GxsBoardMessageRepository gxsBoardMessageRepository)
	{
		super(rsServiceRegistry, peerConnectionManager, gxsTransactionManager, databaseSessionManager, identityManager, gxsUpdateService);
		this.gxsChannelGroupRepository = gxsChannelGroupRepository;
		this.gxsChannelMessageRepository = gxsChannelMessageRepository;
		this.gxsUpdateService = gxsUpdateService1;
		this.databaseSessionManager = databaseSessionManager1;
		this.channelNotificationService = channelNotificationService;
		this.gxsBoardMessageRepository = gxsBoardMessageRepository;
	}

	// XXX: don't forget about the comments and votes!

	// XXX: other users cannot write messages on a channel we own

	@Override
	public RsServiceType getServiceType()
	{
		return CHANNELS;
	}

	@Override
	protected AuthenticationRequirements getAuthenticationRequirements()
	{
		return new AuthenticationRequirements.Builder()
				.withPublic(EnumSet.of(ROOT_PUBLISH, CHILD_AUTHOR))
				.withRestricted(EnumSet.of(ROOT_PUBLISH, CHILD_AUTHOR, CHILD_PUBLISH))
				.withPrivate(EnumSet.of(ROOT_PUBLISH, CHILD_AUTHOR, CHILD_PUBLISH))
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
			findAllSubscribedGroups().forEach(channelGroupItem -> {
				var gxsSyncMessageRequestItem = new GxsSyncMessageRequestItem(channelGroupItem.getGxsId(), gxsUpdateService.getLastPeerMessagesUpdate(recipient.getLocation(), channelGroupItem.getGxsId(), getServiceType()), ChronoUnit.YEARS.getDuration());
				log.debug("Asking {} for new messages in {} since {} for {}", recipient, gxsSyncMessageRequestItem.getGroupId(), log.isDebugEnabled() ? Instant.ofEpochSecond(gxsSyncMessageRequestItem.getCreateSince()) : null, getServiceType());
				peerConnectionManager.writeItem(recipient, gxsSyncMessageRequestItem, this);
			});
		}
	}

	@Override
	protected List<ChannelGroupItem> onAvailableGroupListRequest(PeerConnection recipient, Instant since)
	{
		return findAllGroupsSubscribedAndPublishedSince(since);
	}

	@Override
	protected List<ChannelGroupItem> onGroupListRequest(Set<GxsId> ids)
	{
		return findAllGroups(ids);
	}

	@Override
	protected Set<GxsId> onAvailableGroupListResponse(Map<GxsId, Instant> ids)
	{
		// We want new channels as well as updated ones
		var existingMap = findAllGroups(ids.keySet()).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, channelGroupItem -> channelGroupItem.getPublished().truncatedTo(ChronoUnit.SECONDS)));

		ids.entrySet().removeIf(gxsIdInstantEntry -> {
			var existing = existingMap.get(gxsIdInstantEntry.getKey());
			return existing != null && !gxsIdInstantEntry.getValue().isAfter(existing);
		});
		return ids.keySet();
	}

	@Override
	protected boolean onGroupReceived(ChannelGroupItem item)
	{
		log.debug("Received {}, saving/updating...", item);
		return true;
	}

	@Override
	protected void onGroupsSaved(List<ChannelGroupItem> items)
	{
		//channelNotificationService.addChannelGroups(items); XXX
	}

	@Override
	protected List<ChannelMessageItem> onPendingMessageListRequest(PeerConnection recipient, GxsId groupId, Instant since)
	{
		return findAllMessagesInGroupSince(groupId, since);
	}

	@Override
	protected List<? extends GxsMessageItem> onMessageListRequest(GxsId groupId, Set<MessageId> messageIds)
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
	protected boolean onMessageReceived(ChannelMessageItem item)
	{
		if (item.hasImage())
		{
			// Set the dimensions in the database so that images don't cause layout
			// problems when displaying them in long lists without fixed size.
			var dimension = ImageUtils.getImageDimension(new ByteArrayInputStream(item.getImage()));
			if (dimension != null)
			{
				item.setImageWidth((int) dimension.getWidth());
				item.setImageHeight((int) dimension.getHeight());
			}
		}
		log.debug("Received message {}, saving...", item);
		return true;
	}

	@Override
	protected void onMessagesSaved(List<ChannelMessageItem> items)
	{
		channelNotificationService.addOrUpdateChannelMessages(items);
	}

	@Override
	protected boolean onCommentReceived(CommentMessageItem item)
	{
		return true;
	}

	@Override
	protected void onCommentsSaved(List<CommentMessageItem> items)
	{
		// XXX: channelNotificationService.addChannelComments(items);
	}

	@Override
	protected boolean onVoteReceived(VoteMessageItem item)
	{
		return true;
	}

	@Override
	protected void onVotesSaved(List<VoteMessageItem> items)
	{
		// XXX: channelNotificationService.addChannelVotes(items);
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		super.handleItem(sender, item); // This is required for the @Transactional to work
	}

	public Optional<ChannelGroupItem> findById(long id)
	{
		return gxsChannelGroupRepository.findById(id);
	}

	public List<ChannelGroupItem> findAllGroups()
	{
		return gxsChannelGroupRepository.findAll();
	}

	public List<ChannelGroupItem> findAllSubscribedGroups()
	{
		return gxsChannelGroupRepository.findAllBySubscribedIsTrue();
	}

	public List<ChannelGroupItem> findAllGroups(Set<GxsId> gxsIds)
	{
		return gxsChannelGroupRepository.findAllByGxsIdIn(gxsIds);
	}

	public List<ChannelGroupItem> findAllGroupsSubscribedAndPublishedSince(Instant since)
	{
		return gxsChannelGroupRepository.findAllBySubscribedIsTrueAndPublishedAfter(since);
	}

	public List<ChannelMessageItem> findAllMessagesInGroupSince(GxsId groupId, Instant since)
	{
		return gxsChannelMessageRepository.findAllByGxsIdAndPublishedAfter(groupId, since);
	}

	public List<ChannelMessageItem> findAllMessages(GxsId groupId, Set<MessageId> messageIds)
	{
		return gxsChannelMessageRepository.findAllByGxsIdAndMessageIdIn(groupId, messageIds);
	}

	/**
	 * Finds all messages. Prefer the other variants as this one is slower.
	 *
	 * @param messageIds the list of message ids
	 * @return the messages
	 */
	public List<ChannelMessageItem> findAllMessages(Set<MessageId> messageIds)
	{
		return gxsChannelMessageRepository.findAllByMessageIdIn(messageIds);
	}

	public List<ChannelMessageItem> findAllMessages(long groupId, Set<MessageId> messageIds)
	{
		var channelGroup = gxsChannelGroupRepository.findById(groupId).orElseThrow();
		return gxsChannelMessageRepository.findAllByGxsIdAndMessageIdIn(channelGroup.getGxsId(), messageIds);
	}

	@Transactional
	public Page<ChannelMessageItem> findAllMessages(long groupId, Pageable pageable)
	{
		var channelGroup = gxsChannelGroupRepository.findById(groupId).orElseThrow();
		return gxsChannelMessageRepository.findAllByGxsId(channelGroup.getGxsId(), pageable);
	}

	public Optional<ChannelMessageItem> findMessageById(long id)
	{
		return gxsChannelMessageRepository.findById(id);
	}

	public int getUnreadCount(long groupId)
	{
		var channelGroupItem = gxsChannelGroupRepository.findById(groupId).orElseThrow();
		return gxsChannelMessageRepository.countUnreadMessages(channelGroupItem.getGxsId());
	}

	@Transactional
	public long createChannelGroup(GxsId identity, String name, String description, MultipartFile imageFile) throws IOException
	{
		var channelGroupItem = createGroup(name);
		channelGroupItem.setDescription(description);

		if (imageFile != null && !imageFile.isEmpty())
		{
			channelGroupItem.setImage(GxsUtils.getScaledGroupImage(imageFile, IMAGE_GROUP_SIDE_SIZE));
		}

		if (identity != null)
		{
			channelGroupItem.setAuthor(identity);
		}

		channelGroupItem.setCircleType(GxsCircleType.PUBLIC); // XXX: implement "YOUR_FRIENDS_ONLY"? but based on trust instead
		channelGroupItem.setSignatureFlags(Set.of(GxsSignatureFlags.NONE_REQUIRED, GxsSignatureFlags.AUTHENTICATION_REQUIRED));
		channelGroupItem.setDiffusionFlags(EnumSet.of(GxsPrivacyFlags.PUBLIC));

		//channelGroupItem.setInternalCircle(); XXX: needs that for "YOUR_FRIENDS_ONLY". check what RS does for createBoardV2(), how it is called

		channelGroupItem.setSubscribed(true);

		channelGroupItem = saveChannel(channelGroupItem);

		channelNotificationService.addOrUpdateChannelGroups(List.of(channelGroupItem));

		return channelGroupItem.getId();
	}

	@Transactional
	public void updateChannelGroup(long groupId, String name, String description, MultipartFile imageFile, boolean updateImage) throws IOException
	{
		var channelGroupItem = gxsChannelGroupRepository.findById(groupId).orElseThrow();
		channelGroupItem.setName(name);
		channelGroupItem.setDescription(description);
		if (updateImage)
		{
			if (imageFile != null)
			{
				if (!imageFile.isEmpty())
				{
					channelGroupItem.setImage(GxsUtils.getScaledGroupImage(imageFile, IMAGE_GROUP_SIDE_SIZE));
				}
			}
			else
			{
				channelGroupItem.setImage(null); // Remove the image
			}
		}

		channelGroupItem = saveChannel(channelGroupItem);
		channelNotificationService.addOrUpdateChannelGroups(List.of(channelGroupItem));
	}

	@Transactional
	public ChannelGroupItem saveChannel(ChannelGroupItem channelGroupItem)
	{
		signGroupIfNeeded(channelGroupItem);
		var savedChannel = gxsChannelGroupRepository.save(channelGroupItem);
		gxsUpdateService.setLastServiceGroupsUpdateNow(CHANNELS);
		peerConnectionManager.doForAllPeers(this::sendSyncNotification, this);
		return savedChannel;
	}

	@Transactional
	public long createChannelMessage(IdentityGroupItem author, long channelId, String title, String content, MultipartFile imageFile, long originalId) throws IOException
	{
		// XXX: check the size, like createBoardMessage(), etc...

		var builder = new MessageBuilder(author.getAdminPrivateKey(), gxsChannelGroupRepository.findById(channelId).orElseThrow().getGxsId(), title)
				.authorId(author.getGxsId());

		// XXX: for the image, there are 3 aspect ratio: 1:1, 3:4 and 16:9 (and auto, which picks up the closest one of the original image?)

		if (originalId != 0L)
		{
			builder.originalMessageId(gxsChannelMessageRepository.findById(originalId).orElseThrow().getMessageId());
		}

		builder.getMessageItem().setContent(replaceImageLines(content));

		var channelMessageItem = builder.build();

		var savedMessageId = saveMessage(channelMessageItem).getId();

		channelMessageItem.setId(savedMessageId);
		channelNotificationService.addOrUpdateChannelMessages(List.of(channelMessageItem));

		peerConnectionManager.doForAllPeers(this::sendSyncNotification, this);

		return savedMessageId;
	}

	@Transactional
	public ChannelMessageItem saveMessage(ChannelMessageItem channelMessageItem)
	{
		channelMessageItem.setId(gxsChannelMessageRepository.findByGxsIdAndMessageId(channelMessageItem.getGxsId(), channelMessageItem.getMessageId()).orElse(channelMessageItem).getId()); // XXX: not sure we should be able to overwrite a message. in which case is it correct? maybe throw?
		var savedMessage = gxsChannelMessageRepository.save(channelMessageItem);
		var channelGroupItem = gxsChannelGroupRepository.findByGxsId(channelMessageItem.getGxsId()).orElseThrow();
		channelGroupItem.setLastPosted(Instant.now());
		gxsChannelGroupRepository.save(channelGroupItem);
		return savedMessage;
	}

	@Transactional
	public void subscribeToChannelGroup(long id)
	{
		var channelGroupItem = findById(id).orElseThrow();
		channelGroupItem.setSubscribed(true);
		gxsUpdateService.setLastServiceGroupsUpdateNow(CHANNELS);
		// We don't need to send a sync notify here because it's not urgent.
		// The peers will poll normally to show if there's a new group available.
	}

	@Transactional
	public void unsubscribeFromChannelGroup(long id)
	{
		var channelGroupItem = findById(id).orElseThrow();
		channelGroupItem.setSubscribed(false);
	}

	@Transactional
	public void setChannelMessagesAsRead(Map<Long, Boolean> messageMap)
	{
		gxsChannelMessageRepository.findAllById(messageMap.keySet()).forEach(channelMessageItem -> channelMessageItem.setRead(messageMap.get(channelMessageItem.getId())));
		channelNotificationService.markChannelMessagesAsRead(messageMap);
	}

	@Transactional
	public void setAllChannelMessagesAsRead(long groupId, boolean read)
	{
		var group = gxsChannelGroupRepository.findById(groupId).orElseThrow();
		var numberOfUpdatedMessages = gxsChannelMessageRepository.markAllMessagesAsRead(group.getGxsId(), read);
		channelNotificationService.markAllChannelMessagesAsRead(groupId, read ? -numberOfUpdatedMessages : numberOfUpdatedMessages);
	}

	@Override
	public void shutdown()
	{
		channelNotificationService.shutdown();
	}
}
