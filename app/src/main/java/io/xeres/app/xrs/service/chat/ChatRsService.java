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

package io.xeres.app.xrs.service.chat;

import io.xeres.app.application.events.PeerConnectedEvent;
import io.xeres.app.application.events.PeerDisconnectedEvent;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.*;
import io.xeres.app.xrs.common.Signature;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemUtils;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.chat.item.*;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelRsClient;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelRsService;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelStatus;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Id;
import io.xeres.common.id.Identifier;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.message.MessageType;
import io.xeres.common.message.chat.*;
import io.xeres.common.util.ExecutorUtils;
import io.xeres.common.util.SecureRandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static io.xeres.app.xrs.service.RsServiceType.CHAT;
import static io.xeres.app.xrs.service.RsServiceType.GXS_TUNNEL;
import static io.xeres.common.location.Availability.AVAILABLE;
import static io.xeres.common.location.Availability.OFFLINE;
import static io.xeres.common.message.MessagePath.*;
import static io.xeres.common.message.MessageType.*;
import static io.xeres.common.tray.TrayNotificationType.BROADCAST;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Component
public class ChatRsService extends RsService implements GxsTunnelRsClient
{
	private static final Logger log = LoggerFactory.getLogger(ChatRsService.class);

	/**
	 * Time between housekeeping runs to clean up the message cache and so on.
	 */
	private static final Duration HOUSEKEEPING_DELAY = Duration.ofSeconds(10);

	/**
	 * Maximum time to keep message records.
	 */
	private static final Duration KEEP_MESSAGE_RECORD_MAX = Duration.ofMinutes(20);

	/**
	 * Maximum of chat rooms accepted by a peer.
	 * XXX: should be incremented one day
	 */
	private static final int CHATROOM_LIST_MAX = 50;

	/**
	 * When to refresh nearby chat rooms by asking peers.
	 */
	private static final Duration CHATROOM_NEARBY_REFRESH_INITIAL_MIN = Duration.ofSeconds(0);
	private static final Duration CHATROOM_NEARBY_REFRESH_INITIAL_MAX = Duration.ofSeconds(5);
	private static final Duration CHATROOM_NEARBY_REFRESH = Duration.ofMinutes(2);

	/**
	 * When to remove nearby chat rooms when no peers have them anymore.
	 */
	private static final Duration CHATROOM_NEARBY_TIMEOUT = Duration.ofMinutes(3);

	/**
	 * Time after which a keep alive packet is sent.
	 */
	private static final Duration KEEPALIVE_DELAY = Duration.ofMinutes(2);

	/**
	 * Minimum time between connection challenges.
	 */
	private static final Duration CONNECTION_CHALLENGE_MIN_DELAY = Duration.ofSeconds(15);

	/**
	 * Minimum number of connection challenge counts before one
	 * can be sent.
	 */
	private static final int CONNECTION_CHALLENGE_COUNT_MIN = 20;

	/**
	 * Maximum time difference allowed for messages in the past (this doesn't
	 * account for KEEP_MESSAGE_RECORD_MAX for the total).
	 */
	private static final Duration TIME_DRIFT_PAST_MAX = Duration.ofSeconds(100);

	/**
	 * Maximum time difference allowed for messages in the future.
	 */
	private static final Duration TIME_DRIFT_FUTURE_MAX = Duration.ofMinutes(10);

	/**
	 * Content sent with a typing notification. Note that Retroshare displays
	 * the text directly.
	 */
	private static final String MESSAGE_TYPING_CONTENT = "is typing...";

	private static final int KEY_PARTIAL_MESSAGE_LIST = 1;

	/**
	 * Retroshare puts some limit here.
	 */
	private static final int AVATAR_SIZE_MAX = 32767;

	private static final int DISTANT_CHAT_GXS_TUNNEL_SERVICE_ID = 0xa0001;

	private final Map<Long, ChatRoom> chatRooms = new ConcurrentHashMap<>();
	private final Map<Long, ChatRoom> availableChatRooms = new ConcurrentHashMap<>();
	private final Map<Long, ChatRoom> invitedChatRooms = new ConcurrentHashMap<>();

	private final Map<GxsId, DistantLocation> distantChatContacts = new ConcurrentHashMap<>();

	@Override
	public RsServiceType getMasterServiceType()
	{
		return GXS_TUNNEL;
	}

	private enum Invitation
	{
		PLAIN,
		FROM_CHALLENGE
	}

	private final RsServiceRegistry rsServiceRegistry;
	private final LocationService locationService;
	private final PeerConnectionManager peerConnectionManager;
	private final MessageService messageService;
	private final IdentityService identityService;
	private final DatabaseSessionManager databaseSessionManager;
	private final IdentityManager identityManager;
	private final UiBridgeService uiBridgeService;
	private final ChatRoomService chatRoomService;
	private final ChatBacklogService chatBacklogService;
	private final UnHtmlService unHtmlService;

	private ScheduledExecutorService executorService;
	private GxsTunnelRsService gxsTunnelRsService;

	ChatRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, LocationService locationService, MessageService messageService, IdentityService identityService, DatabaseSessionManager databaseSessionManager, IdentityManager identityManager, UiBridgeService uiBridgeService, ChatRoomService chatRoomService, ChatBacklogService chatBacklogService, UnHtmlService unHtmlService)
	{
		super(rsServiceRegistry);
		this.locationService = locationService;
		this.peerConnectionManager = peerConnectionManager;
		this.messageService = messageService;
		this.identityService = identityService;
		this.databaseSessionManager = databaseSessionManager;
		this.identityManager = identityManager;
		this.uiBridgeService = uiBridgeService;
		this.chatRoomService = chatRoomService;
		this.chatBacklogService = chatBacklogService;
		this.rsServiceRegistry = rsServiceRegistry;
		this.unHtmlService = unHtmlService;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return CHAT;
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.HIGH;
	}

	@Override
	public int onGxsTunnelInitialization(GxsTunnelRsService gxsTunnelRsService)
	{
		this.gxsTunnelRsService = gxsTunnelRsService;
		return DISTANT_CHAT_GXS_TUNNEL_SERVICE_ID;
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		if (item instanceof ChatRoomListRequestItem)
		{
			handleChatRoomListRequestItem(sender);
		}
		else if (item instanceof ChatRoomListItem chatRoomListItem)
		{
			handleChatRoomListItem(sender, chatRoomListItem);
		}
		else if (item instanceof ChatMessageItem chatMessageItem)
		{
			handleChatMessageItem(sender, chatMessageItem);
		}
		else if (item instanceof ChatRoomMessageItem chatRoomMessageItem)
		{
			handleChatRoomMessageItem(sender, chatRoomMessageItem);
		}
		else if (item instanceof ChatStatusItem chatStatusItem)
		{
			handleChatStatusItem(sender, chatStatusItem);
		}
		else if (item instanceof ChatRoomInviteItem chatRoomInviteItem)
		{
			handleChatRoomInviteItem(sender, chatRoomInviteItem);
		}
		else if (item instanceof ChatRoomEventItem chatRoomEventItem)
		{
			handleChatRoomEventItem(sender, chatRoomEventItem);
		}
		else if (item instanceof ChatRoomConnectChallengeItem chatRoomConnectChallengeItem)
		{
			handleChatRoomConnectChallengeItem(sender, chatRoomConnectChallengeItem);
		}
		else if (item instanceof ChatRoomUnsubscribeItem chatRoomUnsubscribeItem)
		{
			handleChatRoomUnsubscribeItem(sender, chatRoomUnsubscribeItem);
		}
		else if (item instanceof ChatAvatarItem chatAvatarItem)
		{
			handleChatAvatarItem(sender, chatAvatarItem);
		}
		else if (item instanceof ChatRoomInviteOldItem chatRoomInviteOldItem)
		{
			handleChatRoomInviteOldItem(sender, chatRoomInviteOldItem);
		}
	}

	@Override
	public void onGxsTunnelDataReceived(Location tunnelId, byte[] data)
	{
		var destination = gxsTunnelRsService.getGxsFromTunnel(tunnelId);
		if (destination == null)
		{
			log.error("Cannot get tunnel info from {}", tunnelId);
			return;
		}

		var distantLocation = distantChatContacts.computeIfAbsent(destination, gxsId -> new DistantLocation(tunnelId, destination));

		var item = ItemUtils.deserializeItem(data, rsServiceRegistry);
		switch (item)
		{
			case ChatMessageItem chatMessageItem -> handleChatMessageItem(distantLocation, chatMessageItem);
			case ChatAvatarItem chatAvatarItem -> handleChatAvatarItem(distantLocation, chatAvatarItem);
			case ChatStatusItem chatStatusItem -> handleChatStatusItem(distantLocation, chatStatusItem);
			default -> log.error("Unknown item {}", item);
		}
	}

	@Override
	public boolean onGxsTunnelDataAuthorization(GxsId sender, Location tunnelId, boolean clientSide)
	{
		if (clientSide)
		{
			return true;
		}

		// XXX: add code for refusing distant chats
		return true;
	}

	@Override
	public void onGxsTunnelStatusChanged(Location tunnelId, GxsId destination, GxsTunnelStatus status)
	{
		switch (status)
		{
			case UNKNOWN -> log.warn("Don't know how to handle {}", status);
			case CAN_TALK -> messageService.sendToConsumers(chatDistantDestination(), CHAT_AVAILABILITY, destination, AVAILABLE);
			case TUNNEL_DOWN, REMOTELY_CLOSED -> messageService.sendToConsumers(chatDistantDestination(), CHAT_AVAILABILITY, destination, OFFLINE);
		}
	}

	@Override
	@SuppressWarnings("java:S1905")
	public void initialize()
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			chatRoomService.markAllChatRoomsAsLeft();
			subscribeToAllSavedRooms();
		}

		executorService = ExecutorUtils.createFixedRateExecutor(this::manageChatRooms,
				getInitPriority().getMaxTime() + HOUSEKEEPING_DELAY.toSeconds() / 2,
				HOUSEKEEPING_DELAY.toSeconds());
	}

	@Override
	public void shutdown()
	{
		chatRooms.forEach((id, chatRoom) -> {
			chatRoomService.syncParticipatingLocations(chatRoom);
			sendChatRoomEvent(chatRoom, ChatRoomEvent.PEER_LEFT);
		});
		chatBacklogService.cleanup();
	}

	@Override
	public void cleanup()
	{
		ExecutorUtils.cleanupExecutor(executorService);
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		peerConnection.scheduleAtFixedRate(
				() -> askForNearbyChatRooms(peerConnection),
				ThreadLocalRandom.current().nextLong(CHATROOM_NEARBY_REFRESH_INITIAL_MIN.toSeconds(), CHATROOM_NEARBY_REFRESH_INITIAL_MAX.toSeconds() + 1),
				CHATROOM_NEARBY_REFRESH.toSeconds(),
				TimeUnit.SECONDS
		);
	}

	private void manageChatRooms()
	{
		chatRooms.forEach((roomId, chatRoom) -> {
			log.debug("Cleanup of room {}", chatRoom);

			// Remove old messages
			chatRoom.getMessageCache().purge();

			// Remove inactive gxsIds
			chatRoom.getExpiredUsers().forEach(user -> {
				chatRoom.removeUser(user);
				sendChatRoomTimeoutToConsumers(chatRoom.getId(), user, isEmpty(chatRoom.getParticipatingLocations()));
			});

			sendKeepAliveIfNeeded(chatRoom);

			sendConnectionChallengeIfNeeded(chatRoom);

			sendJoinEventIfNeeded(chatRoom);
		});

		removeUnseenRooms();
	}

	/**
	 * Removes rooms that haven't been seen for a while.
	 */
	private void removeUnseenRooms()
	{
		var now = Instant.now();
		if (availableChatRooms.entrySet().removeIf(entry -> entry.getValue().getLastSeen().plus(CHATROOM_NEARBY_TIMEOUT).isBefore(now)))
		{
			refreshChatRoomsInClients();
		}
	}

	/**
	 * Asks a peer for the list of chat rooms he's subscribed to.
	 *
	 * @param peerConnection the peer
	 */
	private void askForNearbyChatRooms(PeerConnection peerConnection)
	{
		log.debug("Asking for nearby chat rooms...");
		peerConnectionManager.writeItem(peerConnection, new ChatRoomListRequestItem(), this);
	}

	/**
	 * Sends a keep alive event to the room. Allows other users to know we're in it.
	 *
	 * @param chatRoom the chat room
	 */
	private void sendKeepAliveIfNeeded(ChatRoom chatRoom)
	{
		var now = Instant.now();

		if (Duration.between(chatRoom.getLastKeepAlivePacket(), now).compareTo(KEEPALIVE_DELAY) > 0)
		{
			log.debug("Sending keepalive event to chatroom {}", chatRoom);
			sendChatRoomEvent(chatRoom, ChatRoomEvent.KEEP_ALIVE);
			chatRoom.setLastKeepAlivePacket(now);
		}
	}

	/**
	 * Sends a connection challenge. Can be used to know if the peer is relaying a private room that we're also subscribed to.
	 *
	 * @param chatRoom the chat room
	 */
	private void sendConnectionChallengeIfNeeded(ChatRoom chatRoom)
	{
		if (chatRoom.getConnectionChallengeCountAndIncrease() > CONNECTION_CHALLENGE_COUNT_MIN &&
				Duration.between(chatRoom.getLastConnectionChallenge(), Instant.now()).compareTo(CONNECTION_CHALLENGE_MIN_DELAY) > 0)
		{
			chatRoom.resetConnectionChallengeCount();

			var recentMessage = chatRoom.getMessageCache().getRecentMessage();
			if (recentMessage == 0)
			{
				log.debug("No message in cache to send connection challenge to room {}. Not enough activity?", chatRoom);
				return;
			}

			// Send connection challenge to all connected friends
			log.debug("Sending connection challenge for room {}", chatRoom);
			peerConnectionManager.doForAllPeers(peerConnection -> peerConnectionManager.writeItem(peerConnection, new ChatRoomConnectChallengeItem(peerConnection.getLocation().getLocationIdentifier(), chatRoom.getId(), recentMessage), this),
					this);
		}
	}

	/**
	 * Sends a join event so others can know we joined the chat room.
	 *
	 * @param chatRoom the chat room
	 */
	private void sendJoinEventIfNeeded(ChatRoom chatRoom)
	{
		if (!chatRoom.isJoinedRoomPacketSent() && isNotEmpty(chatRoom.getParticipatingLocations()))
		{
			sendChatRoomEvent(chatRoom, ChatRoomEvent.PEER_JOINED);
			chatRoom.setJoinedRoomPacketSent(true);
		}
	}

	/**
	 * Subscribes to all rooms that are saved in the database.
	 */
	private void subscribeToAllSavedRooms()
	{
		log.debug("Subscribing to all saved rooms...");

		chatRoomService.getAllChatRoomsPendingToSubscribe().forEach(savedRoom -> {
			var chatRoom = new ChatRoom(
					savedRoom.getRoomId(),
					savedRoom.getName(),
					savedRoom.getTopic(),
					savedRoom.getFlags().contains(RoomFlags.PUBLIC) ? RoomType.PUBLIC : RoomType.PRIVATE,
					1,
					savedRoom.getFlags().contains(RoomFlags.PGP_SIGNED)
			);
			savedRoom.getLocations().forEach(chatRoom::recordPreviouslyKnownLocation);
			availableChatRooms.put(chatRoom.getId(), chatRoom);
			joinChatRoom(chatRoom.getId());
		});
		refreshChatRoomsInClients();
	}

	private ChatRoomLists buildChatRoomLists()
	{
		var chatRoomLists = new ChatRoomLists();

		chatRooms.forEach((aLong, chatRoom) -> chatRoomLists.addSubscribed(chatRoom.getAsRoomInfo()));
		availableChatRooms.forEach((aLong, chatRoom) -> chatRoomLists.addAvailable(chatRoom.getAsRoomInfo()));
		invitedChatRooms.forEach((aLong, chatRoom) -> {
			if (chatRoom.isPrivate()) // Public rooms can be invited to too, so skip them here
			{
				chatRoomLists.addAvailable(chatRoom.getAsRoomInfo());
			}
		});

		return chatRoomLists;
	}

	public ChatRoomContext getChatRoomContext()
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			var ownIdentity = identityService.getOwnIdentity();
			return new ChatRoomContext(buildChatRoomLists(), new ChatRoomUser(ownIdentity.getName(), ownIdentity.getGxsId(), ownIdentity.getId()));
		}
	}

	/**
	 * Handles the reception of the list of chat room the peer is subscribed to.
	 *
	 * @param peerConnection the peer
	 * @param item           the ChatRoomListItem
	 */
	private void handleChatRoomListItem(PeerConnection peerConnection, ChatRoomListItem item)
	{
		log.debug("Received chat room list from {}: {}", peerConnection, item);
		if (item.getChatRooms().size() > CHATROOM_LIST_MAX)
		{
			log.warn("Location {} is sending a chat room list of {} items, which is bigger than the allowed {}", peerConnection, item.getChatRooms().size(), CHATROOM_LIST_MAX);
		}
		item.getChatRooms().stream()
				.limit(CHATROOM_LIST_MAX)
				.forEach(itemRoom -> {
					var chatRoom = availableChatRooms.getOrDefault(itemRoom.getId(), new ChatRoom(
							itemRoom.getId(),
							itemRoom.getName(),
							itemRoom.getTopic(),
							itemRoom.getFlags().contains(RoomFlags.PUBLIC) ? RoomType.PUBLIC : RoomType.PRIVATE,
							itemRoom.getCount(), // XXX: we should update current chatroom with max(current_count, remote_count)
							itemRoom.getFlags().contains(RoomFlags.PGP_SIGNED)));

					// If we're subscribed to the chat room but the friend is not participating, invite him
					if (chatRoom.addParticipatingLocation(peerConnection.getLocation()) && chatRooms.containsKey(chatRoom.getId()))
					{
						inviteLocationToChatRoom(peerConnection.getLocation(), chatRoom, Invitation.PLAIN);
					}
					updateRooms(chatRoom);
					chatRoomService.getAllChatRoomsPendingToSubscribe().stream()
							.filter(pendingChatRoom -> pendingChatRoom.getRoomId() == chatRoom.getId())
							.findFirst()
							.ifPresent(pendingChatRoom -> joinChatRoom(pendingChatRoom.getRoomId()));
				});

		refreshChatRoomsInClients();
	}

	private void updateRooms(ChatRoom chatRoom)
	{
		chatRoom.updateLastSeen();
		availableChatRooms.put(chatRoom.getId(), chatRoom);
		chatRooms.replace(chatRoom.getId(), chatRoom);
	}

	private void refreshChatRoomsInClients()
	{
		messageService.sendToConsumers(chatRoomDestination(), CHAT_ROOM_LIST, buildChatRoomLists());
	}

	private void handleChatRoomListRequestItem(PeerConnection peerConnection)
	{
		var chatRoomListItem = new ChatRoomListItem(chatRooms.values().stream()
				.filter(chatRoom -> chatRoom.isPublic()
						|| chatRoom.isPreviouslyKnownLocation(peerConnection.getLocation())
						|| chatRoom.getParticipatingLocations().contains(peerConnection.getLocation()))
				.map(ChatRoom::getAsVisibleChatRoomInfo)
				.toList());

		log.debug("Received chat room list request from {}, sending back {}", peerConnection, chatRoomListItem);

		peerConnectionManager.writeItem(peerConnection, chatRoomListItem, this);
	}

	private void handleChatRoomMessageItem(PeerConnection peerConnection, ChatRoomMessageItem item)
	{
		log.debug("Received chat room message from peer {}: {}", peerConnection, item);

		if (!validateExpiration(item.getSendTime()))
		{
			log.warn("Received chat room message from peer {} failed time validation, dropping", peerConnection);
		}

		if (!validateAndBounceItem(peerConnection, item))
		{
			return;
		}

		var chatRoom = chatRooms.get(item.getRoomId());

		// And display the message for us
		var user = item.getSignature().getGxsId();
		chatRoom.userActivity(user);
		var message = parseIncomingText(item.getMessage());
		chatBacklogService.storeIncomingChatRoomMessage(item.getRoomId(), user, item.getSenderNickname(), message);
		sendChatRoomMessageToConsumers(item.getRoomId(), user, item.getSenderNickname(), message);

		chatRoom.incrementConnectionChallengeCount();
	}

	private void handleChatRoomEventItem(PeerConnection peerConnection, ChatRoomEventItem item)
	{
		log.debug("Received chat room event item from peer {}: {}", peerConnection, item);

		if (!validateExpiration(item.getSendTime()))
		{
			log.warn("Received chat room event from peer {} failed time validation, dropping", peerConnection);
		}

		if (!validateAndBounceItem(peerConnection, item))
		{
			return;
		}

		// XXX: add routing clue
		var chatRoom = chatRooms.get(item.getRoomId());
		var user = item.getSignature().getGxsId();

		if (item.getEventType() == ChatRoomEvent.PEER_LEFT.getCode())
		{
			chatRoom.removeUser(user);
			sendChatRoomEventToConsumers(item.getRoomId(), CHAT_ROOM_USER_LEAVE, user, item.getSenderNickname());
		}
		else if (item.getEventType() == ChatRoomEvent.PEER_JOINED.getCode())
		{
			chatRoom.addUser(user);
			sendChatRoomEventToConsumers(item.getRoomId(), CHAT_ROOM_USER_JOIN, user, item.getSenderNickname(), identityManager.getGxsGroup(peerConnection, user));
			chatRoom.setLastKeepAlivePacket(Instant.EPOCH); // send a keep alive event to the participant so that he knows we are in the room
		}
		else if (item.getEventType() == ChatRoomEvent.KEEP_ALIVE.getCode())
		{
			chatRoom.addUser(user); // KEEP_ALIVE is also used to add users
			sendChatRoomEventToConsumers(item.getRoomId(), CHAT_ROOM_USER_KEEP_ALIVE, user, item.getSenderNickname(), identityManager.getGxsGroup(peerConnection, user));
		}
		else if (item.getEventType() == ChatRoomEvent.PEER_STATUS.getCode())
		{
			chatRoom.userActivity(user);
			sendChatRoomTypingNotificationToConsumers(item.getRoomId(), user, item.getSenderNickname());
		}
	}

	private void sendChatRoomEventToConsumers(long roomId, MessageType messageType, GxsId gxsId, String nickname, IdentityGroupItem identityGroupItem)
	{
		var chatRoomUserEvent = new ChatRoomUserEvent(gxsId, nickname, identityGroupItem != null ? identityGroupItem.getId() : 0L);
		messageService.sendToConsumers(chatRoomDestination(), messageType, roomId, chatRoomUserEvent);
	}

	private void sendChatRoomEventToConsumers(long roomId, MessageType messageType, GxsId gxsId, String nickname)
	{
		sendChatRoomEventToConsumers(roomId, messageType, gxsId, nickname, null);
	}

	private void sendChatRoomTypingNotificationToConsumers(long roomId, GxsId gxsId, String nickname)
	{
		var chatRoomMessage = new ChatRoomMessage(nickname, gxsId, null);
		messageService.sendToConsumers(chatRoomDestination(), CHAT_ROOM_TYPING_NOTIFICATION, roomId, chatRoomMessage);
	}

	private void sendChatRoomTimeoutToConsumers(long roomId, GxsId gxsId, boolean split)
	{
		var chatRoomTimeoutEvent = new ChatRoomTimeoutEvent(gxsId, split);
		messageService.sendToConsumers(chatRoomDestination(), CHAT_ROOM_USER_TIMEOUT, roomId, chatRoomTimeoutEvent);
	}

	private void sendChatRoomMessageToConsumers(long roomId, GxsId gxsId, String nickname, String content)
	{
		var chatRoomMessage = new ChatRoomMessage(nickname, gxsId, content);
		messageService.sendToConsumers(chatRoomDestination(), CHAT_ROOM_MESSAGE, roomId, chatRoomMessage);
	}

	private void sendInviteToClient(LocationIdentifier locationIdentifier, long roomId, String roomName, String roomTopic)
	{
		if (invitedChatRooms.containsKey(roomId))
		{
			return; // Don't show multiple requesters
		}
		var chatRoomInvite = new ChatRoomInviteEvent(locationIdentifier.toString(), roomName, roomTopic);
		messageService.sendToConsumers(chatRoomDestination(), CHAT_ROOM_INVITE, roomId, chatRoomInvite);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean validateAndBounceItem(PeerConnection peerConnection, ChatRoomBounce item)
	{
		if (!chatRooms.containsKey(item.getRoomId()))
		{
			log.error("We're not subscribed to chat room id {}, dropping item {}", log.isErrorEnabled() ? Id.toStringLowerCase(item.getRoomId()) : null, item);
			return false;
		}

		if (isBanned(item.getSignature().getGxsId()))
		{
			log.debug("Dropping item from banned entity {}", item.getSignature().getGxsId());
			return false;
		}

		if (!validateBounceSignature(peerConnection, item))
		{
			log.error("Invalid signature for item {} from peer {}, gxsId: {}, dropping", item, peerConnection, item.getSignature().getGxsId());
			return false;
		}

		// XXX: add routing clue (ie. best peer for channel)

		return bounce(peerConnection, item);
	}

	private void handleChatRoomUnsubscribeItem(PeerConnection peerConnection, ChatRoomUnsubscribeItem item)
	{
		log.debug("Received unsubscribe item from {}: {}", peerConnection, item);
		var chatRoom = chatRooms.get(item.getRoomId());
		if (chatRoom == null)
		{
			log.error("Cannot unsubscribe peer from chat room {} as we're not in it", log.isErrorEnabled() ? Id.toStringLowerCase(item.getRoomId()) : null);
			return;
		}

		chatRoom.removeParticipatingLocation(peerConnection.getLocation());
		chatRoom.recordPreviouslyKnownLocation(peerConnection.getLocation());
	}

	private void handleChatRoomInviteOldItem(PeerConnection peerConnection, ChatRoomInviteOldItem item)
	{
		log.debug("Received deprecated invite from {}: {}", peerConnection, item);
		// We do nothing because current RS sends that event for compatibility
	}

	private void handleChatRoomInviteItem(PeerConnection peerConnection, ChatRoomInviteItem item)
	{
		log.debug("Received invite from {}: {}", peerConnection, item);

		var chatRoom = chatRooms.get(item.getRoomId());
		if (chatRoom != null)
		{
			if (!item.isConnectionChallenge() && (chatRoom.isPublic() != item.isPublic() || chatRoom.isSigned() != item.isSigned()))
			{
				log.debug("Not a matching item");
				return;
			}
			log.debug("Adding peer {} to chat room {}", peerConnection, chatRoom);

			chatRoom.addParticipatingLocation(peerConnection.getLocation());
		}
		else
		{
			if (!item.isConnectionChallenge())
			{
				log.debug("Chat room invite, prompting user...");

				var invitedChatRoom = new ChatRoom(
						item.getRoomId(),
						item.getRoomName(),
						item.getRoomTopic(),
						item.isPublic() ? RoomType.PUBLIC : RoomType.PRIVATE,
						1,
						item.isSigned());

				invitedChatRoom.addParticipatingLocation(peerConnection.getLocation());
				sendInviteToClient(peerConnection.getLocation().getLocationIdentifier(), item.getRoomId(), item.getRoomName(), item.getRoomTopic());

				invitedChatRooms.put(invitedChatRoom.getId(), invitedChatRoom);

				refreshChatRoomsInClients();
			}
		}
	}

	private void handleChatStatusItem(PeerConnection peerConnection, ChatStatusItem item)
	{
		// There's a whole protocol with the flags (REQUEST_CUSTOM_STATE, CUSTOM_STATE and CUSTOM_STATE_AVAILABLE)
		// to send and request states; but it seems all RS does is send the typing state every
		// 5 seconds while the user is typing.
		log.debug("Got status item from peer {}: {}", peerConnection, item);
		if (MESSAGE_TYPING_CONTENT.equals(item.getStatus()))
		{
			messageService.sendToConsumers(chatPrivateDestination(), CHAT_TYPING_NOTIFICATION, peerConnection.getLocation().getLocationIdentifier(), new ChatMessage());
		}
		else
		{
			log.warn("Unknown status item from peer {}, status: {}, flags: {}", peerConnection, item.getStatus(), item.getFlags());
		}
	}

	private void handleChatStatusItem(DistantLocation distantLocation, ChatStatusItem item)
	{
		log.debug("Got status item from distant peer {}: {}", distantLocation, item);
		if (MESSAGE_TYPING_CONTENT.equals(item.getStatus()))
		{
			messageService.sendToConsumers(chatDistantDestination(), CHAT_TYPING_NOTIFICATION, distantLocation.getGxsId(), new ChatMessage());
		}
		else
		{
			log.warn("Unknown status item from distant peer {}, status: {}, flags: {}", distantLocation, item.getStatus(), item.getFlags());
		}
	}

	private void handleChatMessageItem(PeerConnection peerConnection, ChatMessageItem item)
	{
		log.debug("Received chat message item from {}: {}", peerConnection, item);
		if (item.isPrivate())
		{
			if (item.isAvatarRequest())
			{
				handleAvatarRequest(peerConnection);
			}
			else
			{
				if (item.isPartial())
				{
					handlePartialMessage(peerConnection, item);
				}
				else
				{
					handleMessage(peerConnection, item);
				}
			}
		}
		else if (item.isBroadcast())
		{
			uiBridgeService.showTrayNotification(BROADCAST, "Broadcast from " + peerConnection.getLocation().getProfile().getName() + "@" + peerConnection.getLocation().getSafeName() + ": " + parseIncomingText(item.getMessage()));
		}
	}

	private void handleChatMessageItem(DistantLocation distantLocation, ChatMessageItem item)
	{
		log.debug("Received distant chat message item from {}: {}", distantLocation, item);
		if (!item.isPrivate())
		{
			log.debug("Item type {} not supported", item);
			return;
		}

		if (item.isAvatarRequest())
		{
			handleAvatarRequest(distantLocation);
		}
		else
		{
			if (item.isPartial())
			{
				handlePartialMessage(distantLocation, item);
			}
			else
			{
				handleMessage(distantLocation, item);
			}
		}
	}

	private void handleAvatarRequest(PeerConnection peerConnection)
	{
		var ownImage = getOwnImage();
		if (ownImage != null)
		{
			peerConnectionManager.writeItem(peerConnection, new ChatAvatarItem(ownImage), this);
		}
	}

	private void handleAvatarRequest(DistantLocation distantLocation)
	{
		var ownImage = getOwnImage();
		if (ownImage != null)
		{
			gxsTunnelRsService.sendData(distantLocation.getTunnelId(), DISTANT_CHAT_GXS_TUNNEL_SERVICE_ID, ItemUtils.serializeItem(new ChatAvatarItem(ownImage), this));
		}
	}

	private byte[] getOwnImage()
	{
		var ownImage = identityService.getOwnIdentity().getImage();
		if (ownImage != null && ownImage.length <= AVATAR_SIZE_MAX)
		{
			return ownImage;
		}
		return null;
	}

	private void handleMessage(PeerConnection peerConnection, ChatMessageItem item)
	{
		var message = item.getMessage();
		var messageList = peerConnection.getServiceData(this, KEY_PARTIAL_MESSAGE_LIST);
		if (messageList.isPresent())
		{
			@SuppressWarnings("unchecked")
			var existingList = (List<String>) messageList.get();
			existingList.add(message);
			message = String.join("", existingList);
			peerConnection.removeServiceData(this, KEY_PARTIAL_MESSAGE_LIST);
		}
		var from = peerConnection.getLocation().getLocationIdentifier();
		var chatMessage = new ChatMessage(parseIncomingText(message));
		chatBacklogService.storeIncomingMessage(from, chatMessage.getContent());
		messageService.sendToConsumers(chatPrivateDestination(), CHAT_PRIVATE_MESSAGE, from, chatMessage);
	}

	private void handleMessage(DistantLocation distantLocation, ChatMessageItem item)
	{
		var message = item.getMessage();
		if (distantLocation.hasMessages())
		{
			distantLocation.addMessage(message);
			message = distantLocation.getAllMessages();
			distantLocation.clearMessages();
		}
		var from = distantLocation.getGxsId();
		var chatMessage = new ChatMessage(parseIncomingText(message));
		chatBacklogService.storeIncomingDistantMessage(from, chatMessage.getContent());
		messageService.sendToConsumers(chatDistantDestination(), CHAT_PRIVATE_MESSAGE, from, chatMessage);
	}

	private void handlePartialMessage(PeerConnection peerConnection, ChatMessageItem item)
	{
		var messageList = peerConnection.getServiceData(this, KEY_PARTIAL_MESSAGE_LIST);
		if (messageList.isEmpty())
		{
			List<String> newMessageList = new ArrayList<>();
			newMessageList.add(item.getMessage());
			peerConnection.putServiceData(this, KEY_PARTIAL_MESSAGE_LIST, newMessageList);
		}
		else
		{
			//noinspection unchecked
			((List<String>) messageList.get()).add(item.getMessage());
		}
	}

	private void handlePartialMessage(DistantLocation distantLocation, ChatMessageItem item)
	{
		distantLocation.addMessage(item.getMessage());
	}

	private void handleChatAvatarItem(PeerConnection peerConnection, ChatAvatarItem item)
	{
		if (!isAvatarValid(item))
		{
			log.debug("Avatar from {} is null or too big", peerConnection);
			return;
		}

		var chatAvatar = new ChatAvatar(item.getImageData());
		messageService.sendToConsumers(chatPrivateDestination(), CHAT_AVATAR, peerConnection.getLocation().getLocationIdentifier(), chatAvatar);
	}

	private void handleChatAvatarItem(DistantLocation distantLocation, ChatAvatarItem item)
	{
		if (!isAvatarValid(item))
		{
			log.debug("Distant avatar from {} is null or too big", distantLocation);
			return;
		}

		var chatAvatar = new ChatAvatar(item.getImageData());
		messageService.sendToConsumers(chatDistantDestination(), CHAT_AVATAR, distantLocation.getGxsId(), chatAvatar);
	}

	private boolean isAvatarValid(ChatAvatarItem item)
	{
		return item.getImageData() != null && item.getImageData().length <= AVATAR_SIZE_MAX;
	}

	/**
	 * Allows to know if a peer is participating in a private chat room and if it is, add it as participating in the room.
	 * For example A, B and C are connected together. If B sends a challenge to A, and it matches (because B is connected through C), A will know that B is on that private
	 * channel and can forward directly to it.
	 *
	 * @param peerConnection the peer connection
	 * @param item           the challenge item
	 */
	private void handleChatRoomConnectChallengeItem(PeerConnection peerConnection, ChatRoomConnectChallengeItem item)
	{
		log.debug("Received chat room connect challenge from {}: {}", peerConnection, item);
		var locationIdentifier = peerConnection.getLocation().getLocationIdentifier();

		chatRooms.values().stream()
				.filter(chatRoom -> chatRoom.getMessageCache().hasConnectionChallenge(locationIdentifier, chatRoom.getId(), item.getChallengeCode()))
				.findAny()
				.ifPresent(chatRoom -> {
					log.debug("Challenge accepted for chatroom {}, sending connection request to peer {}", chatRoom, peerConnection);
					chatRoom.addParticipatingLocation(peerConnection.getLocation());
					inviteLocationToChatRoom(peerConnection.getLocation(), chatRoom, Invitation.FROM_CHALLENGE);
				});
	}

	private void inviteLocationToChatRoom(Location location, ChatRoom chatRoom, Invitation invitation)
	{
		log.debug("Invite location {} to chatRoom {} with invitation {}", location, chatRoom, invitation);
		var item = new ChatRoomInviteItem(
				chatRoom.getId(),
				chatRoom.getName(),
				chatRoom.getTopic(),
				invitation == Invitation.FROM_CHALLENGE ? EnumSet.of(RoomFlags.CHALLENGE) : chatRoom.getRoomFlags());
		peerConnectionManager.writeItem(location, item, this);
	}

	private void signalChatRoomLeave(Location location, ChatRoom chatRoom)
	{
		var item = new ChatRoomUnsubscribeItem(chatRoom.getId());
		peerConnectionManager.writeItem(location, item, this);
	}

	private void initializeBounce(ChatRoom chatRoom, ChatRoomBounce bounce)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			var ownIdentity = identityService.getOwnIdentity();

			bounce.setRoomId(chatRoom.getId());
			bounce.setMessageId(chatRoom.getNewMessageId());
			bounce.setSenderNickname(ownIdentity.getName()); // XXX: we should use the identity in chatRoom.getGxsId() once we have multiple identities support done properly

			var signature = identityService.signData(ownIdentity, getBounceData(bounce));

			bounce.setSignature(new Signature(ownIdentity.getGxsId(), signature));
		}
	}

	private boolean bounce(ChatRoomBounce bounce)
	{
		return bounce(null, bounce);
	}

	private boolean bounce(PeerConnection peerConnection, ChatRoomBounce bounce)
	{
		var chatRoom = chatRooms.get(bounce.getRoomId());
		if (chatRoom == null)
		{
			log.error("Can't send to chat room {}, we're not subscribed to it", log.isErrorEnabled() ? Id.toStringLowerCase(bounce.getRoomId()) : null);
			return false;
		}

		if (peerConnection != null)
		{
			chatRoom.addParticipatingLocation(peerConnection.getLocation()); // If we didn't receive the list yet, it means he's participating still
		}

		if (chatRoom.getMessageCache().exists(bounce.getMessageId()))
		{
			log.debug("Message id {} already received, dropping", bounce.getMessageId());
			return false;
		}

		chatRoom.getMessageCache().add(bounce.getMessageId());
		chatRoom.updateActivity();

		// XXX: check for antiflood

		// Send to everyone except the originating peer
		var iterator = chatRoom.getParticipatingLocations().iterator();
		while (iterator.hasNext())
		{
			var location = iterator.next();
			if (peerConnection == null || !Objects.equals(location, peerConnection.getLocation()))
			{
				var status = peerConnectionManager.writeItem(location, bounce.clone(), this); // Netty frees sent items so we need to clone
				if (status.isDone() && !status.isSuccess())
				{
					iterator.remove(); // Failed to write, it means the location disconnected, so we need to remove it from our participating locations
				}
			}
		}

		chatRoom.incrementConnectionChallengeCount();

		return true;
	}

	private boolean validateBounceSignature(PeerConnection peerConnection, ChatRoomBounce bounce)
	{
		var gxsGroup = identityManager.getGxsGroup(peerConnection, bounce.getSignature().getGxsId());
		if (gxsGroup != null)
		{
			if (!gxsGroup.hasAdminPublicKey())
			{
				log.debug("{} has no public admin key, not validating", bounce.getSenderNickname());
				return false;
			}
			return RSA.verify(gxsGroup.getAdminPublicKey(), bounce.getSignature().getData(), getBounceData(bounce));
		}
		log.debug("No key yet for verification, passing through");
		return true; // if we don't have the identity yet, we let the item pass because it could be valid, and it's impossible to impersonate an identity this way
	}

	private static boolean isBanned(GxsId gxsId)
	{
		// XXX: implement by using the reputation level
		return false;
	}

	private byte[] getBounceData(ChatRoomBounce chatRoomBounce)
	{
		return ItemUtils.serializeItemForSignature(chatRoomBounce, this);
	}

	/**
	 * Checks if a message is well within our own time.
	 *
	 * @param sendTime the time the message was sent at, in seconds from 1970-01-01 UTC
	 * @return true if within bounds
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private static boolean validateExpiration(int sendTime)
	{
		var now = Instant.now();
		if (sendTime < now.getEpochSecond() + TIME_DRIFT_PAST_MAX.toSeconds() - KEEP_MESSAGE_RECORD_MAX.toSeconds())
		{
			return false;
		}

		//noinspection RedundantIfStatement
		if (sendTime > now.getEpochSecond() + TIME_DRIFT_FUTURE_MAX.toSeconds())
		{
			return false;
		}
		return true;
	}

	/**
	 * Sends a broadcast message to all connected peers.
	 *
	 * @param message the message
	 */
	public void sendBroadcastMessage(String message)
	{
		var chatMessageItem = new ChatMessageItem(message, EnumSet.of(ChatFlags.PUBLIC));
		peerConnectionManager.doForAllPeers(peerConnection -> peerConnectionManager.writeItem(peerConnection, chatMessageItem, this),
				this);
	}

	/**
	 * Sends a private message to a peer.
	 *
	 * @param identifier the identifier (LocationIdentifier or GxsId)
	 * @param message    the message
	 */
	public void sendPrivateMessage(Identifier identifier, String message)
	{
		switch (identifier)
		{
			case LocationIdentifier locationIdentifier -> sendPrivateMessageToLocation(locationIdentifier, message);
			case GxsId gxsId -> sendPrivateMessageToGxsId(gxsId, message);
			default -> throw new IllegalStateException("Unexpected value: " + identifier);
		}
	}

	private void sendPrivateMessageToLocation(LocationIdentifier locationIdentifier, String message)
	{
		var location = locationService.findLocationByLocationIdentifier(locationIdentifier).orElseThrow();
		chatBacklogService.storeOutgoingMessage(location.getLocationIdentifier(), message);
		peerConnectionManager.writeItem(location, new ChatMessageItem(message, EnumSet.of(ChatFlags.PRIVATE)), this);
	}

	private void sendPrivateMessageToGxsId(GxsId gxsId, String message)
	{
		var distantLocation = distantChatContacts.get(gxsId);
		if (distantLocation == null)
		{
			log.error("Cannot find distantLocation for gxsId {} when sending private message", gxsId);
			return;
		}

		var identity = identityService.findByGxsId(gxsId).orElseThrow();
		chatBacklogService.storeOutgoingDistantMessage(identity.getGxsId(), message);
		var data = ItemUtils.serializeItem(new ChatMessageItem(message, EnumSet.of(ChatFlags.PRIVATE)), this);
		gxsTunnelRsService.sendData(distantLocation.getTunnelId(), DISTANT_CHAT_GXS_TUNNEL_SERVICE_ID, data);
	}

	/**
	 * Sends a typing notification for private messages to a peer.
	 *
	 * @param identifier the identifier
	 */
	public void sendPrivateTypingNotification(Identifier identifier)
	{
		switch (identifier)
		{
			case LocationIdentifier locationIdentifier -> sendPrivateTypingNotificationToLocation(locationIdentifier);
			case GxsId gxsId -> sendPrivateTypingNotificationToGxsId(gxsId);
			default -> throw new IllegalStateException("Unexpected value: " + identifier);
		}
	}

	private void sendPrivateTypingNotificationToLocation(LocationIdentifier locationIdentifier)
	{
		var location = locationService.findLocationByLocationIdentifier(locationIdentifier).orElseThrow();
		peerConnectionManager.writeItem(location, new ChatStatusItem(MESSAGE_TYPING_CONTENT, EnumSet.of(ChatFlags.PRIVATE)), this);
	}

	private void sendPrivateTypingNotificationToGxsId(GxsId gxsId)
	{
		var distantLocation = distantChatContacts.get(gxsId);
		if (distantLocation == null)
		{
			log.error("Cannot find distantLocation for gxsId {} when sending typing notification", gxsId);
			return;
		}
		var data = ItemUtils.serializeItem(new ChatStatusItem(MESSAGE_TYPING_CONTENT, EnumSet.of(ChatFlags.PRIVATE)), this);
		gxsTunnelRsService.sendData(distantLocation.getTunnelId(), DISTANT_CHAT_GXS_TUNNEL_SERVICE_ID, data);
	}

	public void sendAvatarRequest(Identifier identifier)
	{
		switch (identifier)
		{
			case LocationIdentifier locationIdentifier -> sendAvatarRequestToLocation(locationIdentifier);
			case GxsId gxsId -> sendAvatarRequestToGxsId(gxsId);
			default -> throw new IllegalStateException("Unexpected value: " + identifier);
		}
	}

	private void sendAvatarRequestToLocation(LocationIdentifier locationIdentifier)
	{
		var location = locationService.findLocationByLocationIdentifier(locationIdentifier).orElseThrow();
		peerConnectionManager.writeItem(location, new ChatMessageItem("", EnumSet.of(ChatFlags.PRIVATE, ChatFlags.REQUEST_AVATAR)), this);
	}

	private void sendAvatarRequestToGxsId(GxsId gxsId)
	{
		var distantLocation = distantChatContacts.get(gxsId);
		if (distantLocation == null)
		{
			log.error("Cannot find distantLocation for gxsId: {} when sending avatar request", gxsId);
			return;
		}
		var data = ItemUtils.serializeItem(new ChatMessageItem("", EnumSet.of(ChatFlags.PRIVATE, ChatFlags.REQUEST_AVATAR)), this);
		gxsTunnelRsService.sendData(distantLocation.getTunnelId(), DISTANT_CHAT_GXS_TUNNEL_SERVICE_ID, data);
	}

	public Location createDistantChat(IdentityGroupItem identityGroupItem)
	{
		var ownIdentity = identityService.getOwnIdentity();
		var tunnelId = gxsTunnelRsService.requestSecuredTunnel(ownIdentity.getGxsId(), identityGroupItem.getGxsId(), DISTANT_CHAT_GXS_TUNNEL_SERVICE_ID);
		if (tunnelId != null)
		{
			log.debug("Creating distant chat tunnel for identity {}, resulting tunnelId: {}", identityGroupItem.getGxsId(), tunnelId.getLocationIdentifier());
			distantChatContacts.put(identityGroupItem.getGxsId(), new DistantLocation(tunnelId, identityGroupItem.getGxsId()));
		}
		return tunnelId;
	}

	public boolean closeDistantChat(IdentityGroupItem identityGroupItem)
	{
		var location = distantChatContacts.remove(identityGroupItem.getGxsId());
		if (location == null)
		{
			log.debug("Failed to close distant chat for identityGroupItem {}", identityGroupItem);
			return false;
		}
		gxsTunnelRsService.closeExistingTunnel(location.getTunnelId(), DISTANT_CHAT_GXS_TUNNEL_SERVICE_ID);
		return true;
	}

	/**
	 * Sets the status message (the one appearing at the top of the profile peer; for example, "I'm eating", "Gone for a walk", etc...).
	 *
	 * @param message the status message
	 */
	public void setStatusMessage(String message)
	{
		peerConnectionManager.doForAllPeers(peerConnection -> peerConnectionManager.writeItem(peerConnection, new ChatStatusItem(message, EnumSet.of(ChatFlags.CUSTOM_STATE)), this),
				this);
	}

	/**
	 * Sends a message to a chat room.
	 *
	 * @param chatRoomId the id of the chat room
	 * @param message    the message
	 */
	public void sendChatRoomMessage(long chatRoomId, String message)
	{
		var chatRoomMessageItem = new ChatRoomMessageItem(message);

		var chatRoom = chatRooms.get(chatRoomId);
		if (chatRoom == null)
		{
			log.warn("Chatroom {} doesn't exist. Not sending the message.", log.isWarnEnabled() ? Id.toStringLowerCase(chatRoomId) : null);
			return;
		}

		initializeBounce(chatRoom, chatRoomMessageItem);
		chatBacklogService.storeOutgoingChatRoomMessage(chatRoomId, chatRoomMessageItem.getSenderNickname(), message);
		bounce(chatRoomMessageItem);
	}

	public void sendChatRoomTypingNotification(long chatRoomId)
	{
		var chatRoom = chatRooms.get(chatRoomId);
		if (chatRoom == null)
		{
			log.warn("Chatroom {} doesn't exist. Not sending the typing notification.", log.isWarnEnabled() ? Id.toStringLowerCase(chatRoomId) : null);
			return;
		}
		sendChatRoomEvent(chatRoom, ChatRoomEvent.PEER_STATUS, MESSAGE_TYPING_CONTENT);
	}

	/**
	 * Joins a chat room.
	 *
	 * @param chatRoomId the id of the chat room
	 */
	public void joinChatRoom(long chatRoomId)
	{
		log.debug("Joining chat room {}", log.isDebugEnabled() ? Id.toStringLowerCase(chatRoomId) : null);
		if (chatRooms.containsKey(chatRoomId))
		{
			log.debug("Already in the chatroom");
			return;
		}

		var chatRoom = getAvailableChatRoom(chatRoomId);
		if (chatRoom == null)
		{
			log.warn("Chatroom {} doesn't exist, can't join.", log.isWarnEnabled() ? Id.toStringLowerCase(chatRoomId) : null);
			return;
		}
		chatRooms.put(chatRoomId, chatRoom);

		try (var ignored = new DatabaseSession(databaseSessionManager)) // XXX: ugly, it's because we can be called from a lambda.. make it take the arguments later (needed for multi identity support)
		{
			var ownIdentity = identityService.getOwnIdentity();
			chatRoom.setOwnGxsId(ownIdentity.getGxsId());
			chatRoomService.subscribeToChatRoomAndJoin(chatRoom, ownIdentity);

			chatRoom.getParticipatingLocations().forEach(location -> inviteLocationToChatRoom(location, chatRoom, Invitation.PLAIN));

			messageService.sendToConsumers(chatRoomDestination(), CHAT_ROOM_JOIN, chatRoom.getId(), new ChatRoomMessage());
			chatRoom.addUser(ownIdentity.getGxsId());

			sendJoinEventIfNeeded(chatRoom);

			// Add ourselves in the UI so that we're shown as joining
			sendChatRoomEventToConsumers(chatRoom.getId(), CHAT_ROOM_USER_JOIN, ownIdentity.getGxsId(), ownIdentity.getName(), ownIdentity);
		}
	}

	private ChatRoom getAvailableChatRoom(long chatRoomId)
	{
		var chatRoom = availableChatRooms.get(chatRoomId);
		if (chatRoom == null)
		{
			chatRoom = invitedChatRooms.remove(chatRoomId);
		}
		return chatRoom;
	}

	/**
	 * Leaves a chat room.
	 *
	 * @param chatRoomId the id of the chat room
	 */
	public void leaveChatRoom(long chatRoomId)
	{
		log.debug("Leaving chat room {}", log.isDebugEnabled() ? Id.toStringLowerCase(chatRoomId) : null);
		var chatRoomToRemove = chatRooms.get(chatRoomId);
		if (chatRoomToRemove == null)
		{
			log.debug("Can't leave a chatroom we aren't into");
			return;
		}
		chatRoomToRemove.clearUsers();
		sendChatRoomEvent(chatRoomToRemove, ChatRoomEvent.PEER_LEFT);
		chatRooms.remove(chatRoomId);
		chatRoomToRemove.setJoinedRoomPacketSent(false); // in the case we rejoin immediately
		chatRoomService.unsubscribeFromChatRoomAndLeave(chatRoomId, identityService.getOwnIdentity()); // XXX: allow multiple identities

		chatRoomToRemove.getParticipatingLocations().forEach(peer -> signalChatRoomLeave(peer, chatRoomToRemove));
		messageService.sendToConsumers(chatRoomDestination(), CHAT_ROOM_LEAVE, chatRoomToRemove.getId(), new ChatRoomMessage());
	}

	public long createChatRoom(String roomName, String topic, Set<RoomFlags> flags, boolean signedIdentities)
	{
		var newChatRoom = new ChatRoom(
				createUniqueRoomId(),
				roomName,
				topic,
				flags.contains(RoomFlags.PUBLIC) ? RoomType.PUBLIC : RoomType.PRIVATE,
				1,
				signedIdentities);

		availableChatRooms.put(newChatRoom.getId(), newChatRoom);

		refreshChatRoomsInClients();

		joinChatRoom(newChatRoom.getId());

		// XXX: we could invite friends in there... supply a list of friends as parameter

		return newChatRoom.getId();
	}

	public void inviteLocationsToChatRoom(long chatRoomId, Set<LocationIdentifier> ids)
	{
		var chatRoom = chatRooms.get(chatRoomId);
		if (chatRoom == null)
		{
			log.error("Cannot invite to unsubscribed chatroom {}", chatRoomId);
			return;
		}

		peerConnectionManager.doForAllPeers(peerConnection -> {
			if (ids.contains(peerConnection.getLocation().getLocationIdentifier()))
			{
				inviteLocationToChatRoom(peerConnection.getLocation(), chatRoom, Invitation.PLAIN);
			}
		}, this);
	}

	@EventListener
	public void onPeerConnectedEvent(PeerConnectedEvent event)
	{
		messageService.sendToConsumers(chatPrivateDestination(), CHAT_AVAILABILITY, event.locationIdentifier(), AVAILABLE);
	}

	@EventListener
	public void onPeerDisconnectedEvent(PeerDisconnectedEvent event)
	{
		messageService.sendToConsumers(chatPrivateDestination(), CHAT_AVAILABILITY, event.locationIdentifier(), OFFLINE);
	}

	private long createUniqueRoomId()
	{
		long newId;

		do
		{
			newId = SecureRandomUtils.nextLong();
		}
		while (availableChatRooms.containsKey(newId) || chatRooms.containsKey(newId) || invitedChatRooms.containsKey(newId));

		return newId;
	}

	/**
	 * Send a chat room event to the participating peers.
	 *
	 * @param chatRoom the chat room
	 * @param event    the event
	 */
	private void sendChatRoomEvent(ChatRoom chatRoom, ChatRoomEvent event)
	{
		sendChatRoomEvent(chatRoom, event, "");
	}

	/**
	 * Send a chat room event to the participating peers.
	 *
	 * @param chatRoom the chat room
	 * @param event    the event
	 * @param status   the status, if empty prefer {@linkplain #sendChatRoomEvent(ChatRoom, ChatRoomEvent) the overloaded alternative}
	 */
	private void sendChatRoomEvent(ChatRoom chatRoom, ChatRoomEvent event, String status)
	{
		var chatRoomEvent = new ChatRoomEventItem(event, status);

		initializeBounce(chatRoom, chatRoomEvent);
		log.debug("Sending chat room event {}", chatRoomEvent);
		bounce(chatRoomEvent);
	}

	private String parseIncomingText(String text)
	{
		return unHtmlService.cleanupMessage(text);
	}
}
