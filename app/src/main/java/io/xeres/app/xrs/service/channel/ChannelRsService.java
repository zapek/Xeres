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
import io.xeres.app.database.repository.GxsChannelGroupRepository;
import io.xeres.app.database.repository.GxsChannelMessageRepository;
import io.xeres.app.database.repository.GxsCommentMessageRepository;
import io.xeres.app.database.repository.GxsVoteMessageRepository;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.notification.channel.ChannelNotificationService;
import io.xeres.app.util.GxsUtils;
import io.xeres.app.xrs.common.CommentMessageItem;
import io.xeres.app.xrs.common.FileItem;
import io.xeres.app.xrs.common.VoteMessageItem;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.channel.item.ChannelGroupItem;
import io.xeres.app.xrs.service.channel.item.ChannelMessageItem;
import io.xeres.app.xrs.service.gxs.GxsAuthentication;
import io.xeres.app.xrs.service.gxs.GxsHelperService;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.gxs.item.GxsSyncMessageRequestItem;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;
import io.xeres.common.util.image.ImageUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.xeres.app.util.GxsUtils.IMAGE_MAX_INPUT_SIZE;
import static io.xeres.app.util.GxsUtils.MAXIMUM_GXS_MESSAGE_SIZE;
import static io.xeres.app.xrs.service.RsServiceType.GXS_CHANNELS;
import static io.xeres.app.xrs.service.gxs.GxsAuthentication.Flags.CHILD_NEEDS_AUTHOR;
import static io.xeres.app.xrs.service.gxs.GxsAuthentication.Flags.ROOT_NEEDS_PUBLISH;

@Component
public class ChannelRsService extends GxsRsService<ChannelGroupItem, ChannelMessageItem>
{
	private static final int IMAGE_GROUP_SIDE_SIZE = 128;

	private static final int IMAGE_MESSAGE_WIDTH = 128; // XXX: how much?! it's some aspect ratio thing, see below
	private static final int IMAGE_MESSAGE_HEIGHT = 128; // XXX: ditto...

	private static final Duration SYNCHRONIZATION_INITIAL_DELAY = Duration.ofSeconds(90);
	private static final Duration SYNCHRONIZATION_DELAY = Duration.ofMinutes(1);

	private final GxsChannelGroupRepository gxsChannelGroupRepository;
	private final GxsChannelMessageRepository gxsChannelMessageRepository;
	private final GxsHelperService<ChannelGroupItem, ChannelMessageItem> gxsHelperService;
	private final DatabaseSessionManager databaseSessionManager;
	private final ChannelNotificationService channelNotificationService;
	private final GxsCommentMessageRepository gxsCommentMessageRepository;
	private final GxsVoteMessageRepository gxsVoteMessageRepository;

	public ChannelRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager, IdentityManager identityManager, GxsHelperService<ChannelGroupItem, ChannelMessageItem> gxsHelperService, GxsChannelGroupRepository gxsChannelGroupRepository, GxsChannelMessageRepository gxsChannelMessageRepository, GxsHelperService<ChannelGroupItem, ChannelMessageItem> gxsHelperService1, DatabaseSessionManager databaseSessionManager1, ChannelNotificationService channelNotificationService, GxsCommentMessageRepository gxsCommentMessageRepository, GxsVoteMessageRepository gxsVoteMessageRepository)
	{
		super(rsServiceRegistry, peerConnectionManager, gxsTransactionManager, databaseSessionManager, identityManager, gxsHelperService);
		this.gxsChannelGroupRepository = gxsChannelGroupRepository;
		this.gxsChannelMessageRepository = gxsChannelMessageRepository;
		this.gxsHelperService = gxsHelperService1;
		this.databaseSessionManager = databaseSessionManager1;
		this.channelNotificationService = channelNotificationService;
		this.gxsCommentMessageRepository = gxsCommentMessageRepository;
		this.gxsVoteMessageRepository = gxsVoteMessageRepository;
	}

	// XXX: don't forget about the comments and votes!

	@Override
	public RsServiceType getServiceType()
	{
		return GXS_CHANNELS;
	}

	@Override
	protected GxsAuthentication getAuthentication()
	{
		// Only the channel owner can write new posts
		return new GxsAuthentication.Builder()
				.withRequirements(EnumSet.of(ROOT_NEEDS_PUBLISH, CHILD_NEEDS_AUTHOR))
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
				var request = new GxsSyncMessageRequestItem(channelGroupItem.getGxsId(), gxsHelperService.getLastPeerMessagesUpdate(recipient.getLocation(), channelGroupItem.getGxsId(), getServiceType()), ChronoUnit.YEARS.getDuration());
				log.debug("Asking {} for new messages in {} since {} for {}", recipient, request.getGxsId(), log.isDebugEnabled() ? Instant.ofEpochSecond(request.getCreateSince()) : null, getServiceType());
				peerConnectionManager.writeItem(recipient, request, this);
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
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, GxsGroupItem::getPublished));

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
		channelNotificationService.addOrUpdateGroups(items);
	}

	@Override
	protected List<ChannelMessageItem> onPendingMessageListRequest(PeerConnection recipient, GxsId gxsId, Instant since)
	{
		return findAllMessagesInGroupSince(gxsId, since); // Don't return old messages, they're unimportant
	}

	@Override
	protected List<? extends GxsMessageItem> onMessageListRequest(GxsId gxsId, Set<MsgId> msgIds)
	{
		return findAllMessagesVotesAndCommentsIncludingOlds(gxsId, msgIds);
	}

	@Override
	protected List<MsgId> onMessageListResponse(GxsId gxsId, Set<MsgId> msgIds)
	{
		var existing = findAllMessagesVotesAndCommentsIncludingOlds(gxsId, msgIds).stream()
				.map(GxsMessageItem::getMsgId)
				.collect(Collectors.toSet());

		msgIds.removeAll(existing);

		return msgIds.stream().toList();
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
		channelNotificationService.addOrUpdateMessages(items);
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

	public List<ChannelMessageItem> findAllMessagesInGroupSince(GxsId gxsId, Instant since)
	{
		return gxsChannelMessageRepository.findAllByGxsIdAndPublishedAfterAndHiddenFalse(gxsId, since);
	}

	public List<ChannelMessageItem> findAllMessages(GxsId gxsId, Set<MsgId> msgIds)
	{
		return gxsChannelMessageRepository.findAllByGxsIdAndMsgIdInAndHiddenFalse(gxsId, msgIds);
	}

	public List<ChannelMessageItem> findAllMessagesIncludingOlds(GxsId gxsId, Set<MsgId> msgIds)
	{
		return gxsChannelMessageRepository.findAllByGxsIdAndMsgIdIn(gxsId, msgIds);
	}

	public List<GxsMessageItem> findAllMessagesVotesAndCommentsIncludingOlds(GxsId gxsId, Set<MsgId> msgIds)
	{
		var messages = findAllMessagesIncludingOlds(gxsId, msgIds);
		var votes = gxsVoteMessageRepository.findAllByGxsIdAndMsgIdIn(gxsId, msgIds);
		var comments = gxsCommentMessageRepository.findAllByGxsIdAndMsgIdIn(gxsId, msgIds);

		return Stream.of(messages.stream(), votes.stream(), comments.stream())
				.flatMap(stream -> stream)
				.collect(Collectors.toList());
	}

	/**
	 * Finds all messages. Prefer the other variants as this one is slower.
	 *
	 * @param msgIds the list of message ids
	 * @return the messages
	 */
	public List<ChannelMessageItem> findAllMessages(Set<MsgId> msgIds)
	{
		return gxsChannelMessageRepository.findAllByMsgIdInAndHiddenFalse(msgIds);
	}

	public List<ChannelMessageItem> findAllMessages(long groupId, Set<MsgId> msgIds)
	{
		var channelGroup = gxsChannelGroupRepository.findById(groupId).orElseThrow();
		return gxsChannelMessageRepository.findAllByGxsIdAndMsgIdInAndHiddenFalse(channelGroup.getGxsId(), msgIds);
	}

	@Transactional
	public Page<ChannelMessageItem> findAllMessages(long groupId, Pageable pageable)
	{
		var channelGroup = gxsChannelGroupRepository.findById(groupId).orElseThrow();
		return gxsChannelMessageRepository.findAllByGxsIdAndHiddenFalse(channelGroup.getGxsId(), pageable);
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
		var group = createGroup(name, true);
		group.setDescription(description);

		if (imageFile != null && !imageFile.isEmpty())
		{
			group.setImage(GxsUtils.getScaledGroupImage(imageFile, IMAGE_GROUP_SIDE_SIZE));
		}

		if (identity != null)
		{
			group.setAuthorGxsId(identity);
		}

		group.setCircleType(GxsCircleType.PUBLIC); // XXX: implement "YOUR_FRIENDS_ONLY"? but based on trust instead
		group.setSignatureFlags(Set.of(GxsSignatureFlags.NONE_REQUIRED, GxsSignatureFlags.AUTHENTICATION_REQUIRED)); // XXX: correct?
		group.setDiffusionFlags(EnumSet.of(GxsPrivacyFlags.PUBLIC));

		//channelGroupItem.setInternalCircle(); XXX: needs that for "YOUR_FRIENDS_ONLY". check what RS does for createBoardV2(), how it is called

		group.setSubscribed(true);

		group = saveChannel(group);

		channelNotificationService.addOrUpdateGroups(List.of(group));

		return group.getId();
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
		channelNotificationService.addOrUpdateGroups(List.of(channelGroupItem));
	}

	private ChannelGroupItem saveChannel(ChannelGroupItem channelGroupItem)
	{
		signGroupIfNeeded(channelGroupItem);
		var savedChannel = gxsChannelGroupRepository.save(channelGroupItem);
		gxsHelperService.setLastServiceGroupsUpdateNow(GXS_CHANNELS);
		peerConnectionManager.doForAllPeers(this::sendSyncNotification, this);
		return savedChannel;
	}

	@Transactional
	public long createChannelMessage(IdentityGroupItem author, long channelId, String title, String content, MultipartFile imageFile, List<FileItem> files, long originalId) throws IOException
	{
		int size = title.length();

		var group = gxsChannelGroupRepository.findById(channelId).orElseThrow();

		var builder = new MessageBuilder(group, author, title);

		if (StringUtils.isNotBlank(content))
		{
			builder.getMessageItem().setContent(content);
			size += content.length();
		}

		// XXX: for the image, there are 3 aspect ratio: 1:1, 3:4 and 16:9 (and auto, which picks up the closest one of the original image?)
		if (imageFile != null && !imageFile.isEmpty())
		{
			if (imageFile.getSize() >= IMAGE_MAX_INPUT_SIZE)
			{
				throw new IllegalArgumentException("Board message image size is bigger than " + IMAGE_MAX_INPUT_SIZE + " bytes");
			}

			var image = ImageUtils.limitMaximumImageSize(ImageIO.read(imageFile.getInputStream()), IMAGE_MESSAGE_WIDTH * IMAGE_MESSAGE_HEIGHT);
			var imageOut = new ByteArrayOutputStream();
			if (!ImageUtils.writeImageAsJpeg(image, MAXIMUM_GXS_MESSAGE_SIZE - size, imageOut))
			{
				throw new IllegalArgumentException("Couldn't write the image. Unsupported format?");
			}

			var data = imageOut.toByteArray();
			builder.getMessageItem().setImage(data);
			size += data.length;
		}

		builder.getMessageItem().setFiles(files);

		if (originalId != 0L)
		{
			builder.originalMsgId(gxsChannelMessageRepository.findById(originalId).orElseThrow().getMsgId());
		}

		if (size >= MAXIMUM_GXS_MESSAGE_SIZE)
		{
			throw new IllegalArgumentException("The message is too large. Reduce the content.");
		}

		var channelMessageItem = saveMessage(builder);

		channelNotificationService.addOrUpdateMessages(List.of(channelMessageItem));

		peerConnectionManager.doForAllPeers(this::sendSyncNotification, this);

		return channelMessageItem.getId();
	}

	private ChannelMessageItem saveMessage(MessageBuilder messageBuilder)
	{
		var channelMessageItem = messageBuilder.build();

		channelMessageItem.setId(gxsChannelMessageRepository.findByGxsIdAndMsgId(channelMessageItem.getGxsId(), channelMessageItem.getMsgId()).orElse(channelMessageItem).getId()); // XXX: not sure we should be able to overwrite a message. in which case is it correct? maybe throw?
		var savedMessage = gxsChannelMessageRepository.save(channelMessageItem);
		markOriginalMessageAsHidden(List.of(savedMessage));
		var channelGroupItem = gxsChannelGroupRepository.findByGxsId(channelMessageItem.getGxsId()).orElseThrow();
		channelGroupItem.setLastUpdated(Instant.now());
		gxsChannelGroupRepository.save(channelGroupItem);
		return savedMessage;
	}

	@Transactional
	public void subscribeToChannelGroup(long id)
	{
		var channelGroupItem = findById(id).orElseThrow();
		channelGroupItem.setSubscribed(true);
		gxsHelperService.setLastServiceGroupsUpdateNow(GXS_CHANNELS);
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
	public void setMessageReadState(long messageId, boolean read)
	{
		var message = gxsChannelMessageRepository.findById(messageId).orElseThrow();
		message.setRead(read);
		var group = gxsChannelGroupRepository.findByGxsId(message.getGxsId()).orElseThrow();
		channelNotificationService.setMessageReadState(group.getId(), message.getId(), read);
	}

	@Transactional
	public void setAllGroupMessagesReadState(long groupId, boolean read)
	{
		var group = gxsChannelGroupRepository.findById(groupId).orElseThrow();
		gxsChannelMessageRepository.setAllGroupMessagesReadState(group.getGxsId(), read);
		channelNotificationService.setGroupMessagesReadState(groupId, read);
	}

	@Override
	public void shutdown()
	{
		channelNotificationService.shutdown();
	}
}
