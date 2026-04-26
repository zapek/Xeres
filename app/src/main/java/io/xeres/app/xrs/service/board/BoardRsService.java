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

package io.xeres.app.xrs.service.board;

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.gxs.*;
import io.xeres.app.database.repository.GxsBoardGroupRepository;
import io.xeres.app.database.repository.GxsBoardMessageRepository;
import io.xeres.app.database.repository.GxsCommentMessageRepository;
import io.xeres.app.database.repository.GxsVoteMessageRepository;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.notification.board.BoardNotificationService;
import io.xeres.app.util.GxsUtils;
import io.xeres.app.xrs.common.CommentMessageItem;
import io.xeres.app.xrs.common.VoteMessageItem;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.board.item.BoardGroupItem;
import io.xeres.app.xrs.service.board.item.BoardMessageItem;
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
import static io.xeres.app.xrs.service.RsServiceType.GXS_BOARDS;
import static io.xeres.app.xrs.service.gxs.GxsAuthentication.Flags.CHILD_NEEDS_AUTHOR;
import static io.xeres.app.xrs.service.gxs.GxsAuthentication.Flags.ROOT_NEEDS_AUTHOR;

@Component
public class BoardRsService extends GxsRsService<BoardGroupItem, BoardMessageItem>
{
	private static final int IMAGE_GROUP_SIDE_SIZE = 128;

	private static final int IMAGE_MESSAGE_WIDTH = 640;
	private static final int IMAGE_MESSAGE_HEIGHT = 480;

	private static final Duration SYNCHRONIZATION_INITIAL_DELAY = Duration.ofMinutes(1);
	private static final Duration SYNCHRONIZATION_DELAY = Duration.ofMinutes(1);

	private final GxsBoardGroupRepository gxsBoardGroupRepository;
	private final GxsBoardMessageRepository gxsBoardMessageRepository;
	private final GxsHelperService<BoardGroupItem, BoardMessageItem> gxsHelperService;
	private final DatabaseSessionManager databaseSessionManager;
	private final BoardNotificationService boardNotificationService;
	private final GxsCommentMessageRepository gxsCommentMessageRepository;
	private final GxsVoteMessageRepository gxsVoteMessageRepository;

	public BoardRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, GxsBoardGroupRepository gxsBoardGroupRepository, DatabaseSessionManager databaseSessionManager, IdentityManager identityManager, GxsBoardMessageRepository gxsBoardMessageRepository, GxsHelperService<BoardGroupItem, BoardMessageItem> gxsHelperService, BoardNotificationService boardNotificationService, GxsCommentMessageRepository gxsCommentMessageRepository, GxsVoteMessageRepository gxsVoteMessageRepository)
	{
		super(rsServiceRegistry, peerConnectionManager, gxsTransactionManager, databaseSessionManager, identityManager, gxsHelperService);
		this.gxsBoardGroupRepository = gxsBoardGroupRepository;
		this.gxsBoardMessageRepository = gxsBoardMessageRepository;
		this.gxsHelperService = gxsHelperService;
		this.databaseSessionManager = databaseSessionManager;
		this.boardNotificationService = boardNotificationService;
		this.gxsCommentMessageRepository = gxsCommentMessageRepository;
		this.gxsVoteMessageRepository = gxsVoteMessageRepository;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return GXS_BOARDS;
	}

	@Override
	protected GxsAuthentication getAuthentication()
	{
		// Anybody can post on a board
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
	protected void syncMessages(PeerConnection recipient)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			// Request new messages for all subscribed groups
			findAllSubscribedGroups().forEach(boardGroupItem -> {
				var request = new GxsSyncMessageRequestItem(boardGroupItem.getGxsId(), gxsHelperService.getLastPeerMessagesUpdate(recipient.getLocation(), boardGroupItem.getGxsId(), getServiceType()), ChronoUnit.YEARS.getDuration());
				log.debug("Asking {} for new messages in {} ({}) since {} for {}", recipient, boardGroupItem.getName(), boardGroupItem.getGxsId(), log.isDebugEnabled() ? Instant.ofEpochSecond(request.getCreateSince()) : null, getServiceType());
				peerConnectionManager.writeItem(recipient, request, this);
			});
		}
	}

	// XXX: don't forget about the comments and votes!

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
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, GxsGroupItem::getPublished));

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
		boardNotificationService.addOrUpdateGroups(items);
	}

	@Override
	protected List<BoardMessageItem> onPendingMessageListRequest(PeerConnection recipient, GxsId gxsId, Instant since)
	{
		return findAllMessagesInGroupSince(gxsId, since); // Don't return old messages, they're unimportant
	}

	@Override
	protected List<? extends GxsMessageItem> onMessageListRequest(GxsId gxsId, Set<MsgId> msgIds)
	{
		return findAllMessagesVotesAndCommentsIncludingOlds(gxsId, msgIds);
	}

	@Transactional(readOnly = true)
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
	protected boolean onMessageReceived(BoardMessageItem item)
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
	protected void onMessagesSaved(List<BoardMessageItem> items)
	{
		boardNotificationService.addOrUpdateMessages(items);
	}

	@Override
	protected boolean onCommentReceived(CommentMessageItem item)
	{
		return true;
	}

	@Override
	protected void onCommentsSaved(List<CommentMessageItem> items)
	{
		// XXX: boardNotificationService.addBoardComments(items);
	}

	@Override
	protected boolean onVoteReceived(VoteMessageItem item)
	{
		return true;
	}

	@Override
	protected void onVotesSaved(List<VoteMessageItem> items)
	{
		// XXX: boardNotificationService.addBoardVotes(items);
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

	public List<BoardMessageItem> findAllMessagesInGroupSince(GxsId gxsId, Instant since)
	{
		return gxsBoardMessageRepository.findAllByGxsIdAndPublishedAfterAndHiddenFalse(gxsId, since);
	}

	public List<BoardMessageItem> findAllMessages(GxsId gxsId, Set<MsgId> msgIds)
	{
		return gxsBoardMessageRepository.findAllByGxsIdAndMsgIdInAndHiddenFalse(gxsId, msgIds);
	}

	public List<BoardMessageItem> findAllMessagesIncludingOlds(GxsId gxsId, Set<MsgId> msgIds)
	{
		return gxsBoardMessageRepository.findAllByGxsIdAndMsgIdIn(gxsId, msgIds);
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

	@Transactional
	public Page<BoardMessageItem> findAllMessages(long groupId, Pageable pageable)
	{
		var boardGroup = gxsBoardGroupRepository.findById(groupId).orElseThrow();
		return gxsBoardMessageRepository.findAllByGxsIdAndHiddenFalse(boardGroup.getGxsId(), pageable);
	}

	public Optional<BoardMessageItem> findMessageById(long id)
	{
		return gxsBoardMessageRepository.findById(id);
	}

	/**
	 * Finds all messages. Prefer the other variants as this one is slower.
	 *
	 * @param msgIds the list of message ids
	 * @return the messages
	 */
	public List<BoardMessageItem> findAllMessages(Set<MsgId> msgIds)
	{
		return gxsBoardMessageRepository.findAllByMsgIdInAndHiddenFalse(msgIds);
	}

	public List<BoardMessageItem> findAllMessages(long groupId, Set<MsgId> msgIds)
	{
		var boardGroup = gxsBoardGroupRepository.findById(groupId).orElseThrow();
		return gxsBoardMessageRepository.findAllByGxsIdAndMsgIdInAndHiddenFalse(boardGroup.getGxsId(), msgIds);
	}

	public int getUnreadCount(long groupId)
	{
		var boardGroupItem = gxsBoardGroupRepository.findById(groupId).orElseThrow();
		return gxsBoardMessageRepository.countUnreadMessages(boardGroupItem.getGxsId());
	}

	@Transactional
	public long createBoardGroup(GxsId identity, String name, String description, MultipartFile imageFile) throws IOException
	{
		var group = createGroup(name, false);
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
		group.setSignatureFlags(Set.of(GxsSignatureFlags.NONE_REQUIRED, GxsSignatureFlags.AUTHENTICATION_REQUIRED));
		group.setDiffusionFlags(EnumSet.of(GxsPrivacyFlags.PUBLIC));

		//boardGroupItem.setInternalCircle(); XXX: needs that for "YOUR_FRIENDS_ONLY". check what RS does for createBoardV2(), how it is called

		group.setSubscribed(true);

		group = saveBoard(group);

		boardNotificationService.addOrUpdateGroups(List.of(group));

		return group.getId();
	}

	@Transactional
	public void updateBoardGroup(long groupId, String name, String description, MultipartFile imageFile, boolean updateImage) throws IOException
	{
		var boardGroupItem = gxsBoardGroupRepository.findById(groupId).orElseThrow();
		boardGroupItem.setName(name);
		boardGroupItem.setDescription(description);
		if (updateImage)
		{
			if (imageFile != null)
			{
				if (!imageFile.isEmpty())
				{
					boardGroupItem.setImage(GxsUtils.getScaledGroupImage(imageFile, IMAGE_GROUP_SIDE_SIZE));
				}
			}
			else
			{
				boardGroupItem.setImage(null); // Remove the image
			}
		}

		boardGroupItem = saveBoard(boardGroupItem);
		boardNotificationService.addOrUpdateGroups(List.of(boardGroupItem));
	}

	@Transactional
	public BoardGroupItem saveBoard(BoardGroupItem boardGroupItem)
	{
		signGroupIfNeeded(boardGroupItem);
		var savedBoard = gxsBoardGroupRepository.save(boardGroupItem);
		gxsHelperService.setLastServiceGroupsUpdateNow(GXS_BOARDS);
		peerConnectionManager.doForAllPeers(this::sendSyncNotification, this);
		return savedBoard;
	}

	@Transactional
	public long createBoardMessage(IdentityGroupItem author, long boardId, String title, String content, String link, MultipartFile imageFile) throws IOException
	{
		int size = title.length();

		var group = gxsBoardGroupRepository.findById(boardId).orElseThrow();
		var builder = new MessageBuilder(group, author, title);

		if (StringUtils.isNotBlank(content))
		{
			builder.getMessageItem().setContent(content);
			size += content.length();
		}
		if (StringUtils.isNotEmpty(link))
		{
			builder.getMessageItem().setLink(link);
			size += link.length();
		}

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

		if (size >= MAXIMUM_GXS_MESSAGE_SIZE)
		{
			throw new IllegalArgumentException("The message is too large. Reduce the content and/or the image.");
		}

		var boardMessageItem = saveMessage(builder);

		boardNotificationService.addOrUpdateMessages(List.of(boardMessageItem));

		peerConnectionManager.doForAllPeers(this::sendSyncNotification, this);

		return boardMessageItem.getId();
	}

	private BoardMessageItem saveMessage(MessageBuilder messageBuilder)
	{
		var boardMessageItem = messageBuilder.build();

		boardMessageItem.setId(gxsBoardMessageRepository.findByGxsIdAndMsgId(boardMessageItem.getGxsId(), boardMessageItem.getMsgId()).orElse(boardMessageItem).getId()); // XXX: not sure we should be able to overwrite a message. in which case is it correct? maybe throw?
		var savedMessage = gxsBoardMessageRepository.save(boardMessageItem);
		markOriginalMessageAsHidden(List.of(savedMessage));
		var boardGroupItem = gxsBoardGroupRepository.findByGxsId(boardMessageItem.getGxsId()).orElseThrow();
		boardGroupItem.setLastUpdated(Instant.now());
		gxsBoardGroupRepository.save(boardGroupItem);
		return savedMessage;
	}

	@Transactional
	public void subscribeToBoardGroup(long id)
	{
		var boardGroupItem = findById(id).orElseThrow();
		boardGroupItem.setSubscribed(true);
		gxsHelperService.setLastServiceGroupsUpdateNow(GXS_BOARDS);
		// We don't need to send a sync notify here because it's not urgent.
		// The peers will poll normally to show if there's a new group available.
	}

	@Transactional
	public void unsubscribeFromBoardGroup(long id)
	{
		var boardGroupItem = findById(id).orElseThrow();
		boardGroupItem.setSubscribed(false);
	}

	@Transactional
	public void setMessageReadState(long messageId, boolean read)
	{
		var message = gxsBoardMessageRepository.findById(messageId).orElseThrow();
		message.setRead(read);
		var group = gxsBoardGroupRepository.findByGxsId(message.getGxsId()).orElseThrow();
		boardNotificationService.setMessageReadState(group.getId(), message.getId(), read);
	}

	@Transactional
	public void setAllGroupMessagesReadState(long groupId, boolean read)
	{
		var group = gxsBoardGroupRepository.findById(groupId).orElseThrow();
		gxsBoardMessageRepository.setAllGroupMessagesReadState(group.getGxsId(), read);
		boardNotificationService.setGroupMessagesReadState(groupId, read);
	}

	@Override
	public void shutdown()
	{
		boardNotificationService.shutdown();
	}
}
