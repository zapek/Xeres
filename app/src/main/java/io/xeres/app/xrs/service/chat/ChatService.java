/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.ChatRoomService;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.LocationService;
import io.xeres.app.xrs.common.Signature;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.chat.item.*;
import io.xeres.app.xrs.service.gxsid.GxsIdService;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Id;
import io.xeres.common.id.LocationId;
import io.xeres.common.message.chat.*;
import io.xeres.common.util.NoSuppressedRunnable;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

import static io.xeres.app.xrs.service.RsServiceType.CHAT;
import static io.xeres.common.message.MessageType.*;
import static io.xeres.common.rest.PathConfig.CHAT_PATH;
import static java.util.Map.entry;

// XXX: check if everything is thread safe (the lists are but are the operations ok?)
@Component
public class ChatService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(ChatService.class);

	private final Map<Long, ChatRoom> chatRooms = new ConcurrentHashMap<>();
	private final Map<Long, ChatRoom> availableChatRooms = new ConcurrentHashMap<>();

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
	private static final Duration CHATROOM_NEARBY_REFRESH = Duration.ofMinutes(2);

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

	private enum Invitation
	{
		PLAIN,
		FROM_CHALLENGE
	}

	private final LocationService locationService;
	private final PeerConnectionManager peerConnectionManager;
	private final IdentityService identityService;
	private final ChatRoomService chatRoomService;
	private final DatabaseSessionManager databaseSessionManager;
	private final GxsIdService gxsIdService;

	private ScheduledExecutorService executorService;

	public ChatService(Environment environment, PeerConnectionManager peerConnectionManager, LocationService locationService, PeerConnectionManager peerConnectionManager1, IdentityService identityService, ChatRoomService chatRoomService, DatabaseSessionManager databaseSessionManager, GxsIdService gxsIdService)
	{
		super(environment, peerConnectionManager);
		this.locationService = locationService;
		this.peerConnectionManager = peerConnectionManager1;
		this.identityService = identityService;
		this.chatRoomService = chatRoomService;
		this.databaseSessionManager = databaseSessionManager;
		this.gxsIdService = gxsIdService;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return CHAT;
	}

	@Override
	public Map<Class<? extends Item>, Integer> getSupportedItems()
	{
		return Map.ofEntries(
				entry(ChatMessageItem.class, 1),
				entry(ChatAvatarItem.class, 3),
				entry(ChatStatusItem.class, 4),
				entry(PrivateChatMessageConfigItem.class, 5),
				entry(ChatRoomConnectChallengeItem.class, 9),
				entry(ChatRoomUnsubscribeItem.class, 10),
				entry(ChatRoomListRequestItem.class, 13),
				entry(ChatRoomConfigItem.class, 21),
				entry(ChatRoomMessageItem.class, 23),
				entry(ChatRoomEventItem.class, 24),
				entry(ChatRoomListItem.class, 25),
				entry(ChatRoomInviteItem.class, 27),
				entry(PrivateOutgoingMapItem.class, 28),
				entry(SubscribedChatRoomConfigItem.class, 29)
		);
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.LOW;
	}

	@Override
	@SuppressWarnings("java:S1905")
	public void initialize()
	{
		executorService = Executors.newSingleThreadScheduledExecutor();

		executorService.scheduleAtFixedRate((NoSuppressedRunnable) this::manageChatRooms,
				HOUSEKEEPING_DELAY.toSeconds(),
				HOUSEKEEPING_DELAY.toSeconds(),
				TimeUnit.SECONDS
		);
	}

	@Override
	public void shutdown()
	{
		chatRooms.forEach((id, chatRoom) -> sendChatRoomEvent(chatRoom, ChatRoomEvent.PEER_LEFT));
	}

	@Override
	public void cleanup()
	{
		executorService.shutdownNow();
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		peerConnection.scheduleAtFixedRate(
				() -> askForNearbyChatRooms(peerConnection)
				, 0,
				CHATROOM_NEARBY_REFRESH.toSeconds(),
				TimeUnit.SECONDS
		);

		// XXX: we should do that when the network/UI is ready, not upon the first connection. this is a workaround for now (also Priority.LOW makes it happen pretty late...)
		// XXX: it also gives problems with join events
//		try (var ignored = new DatabaseSession(databaseSessionManager))
//		{
//			subscribeToAllSavedRooms();
//		}
	}

	private void manageChatRooms()
	{
		chatRooms.forEach((roomId, chatRoom) -> {

			// Remove old messages
			chatRoom.getMessageCache().purge();

			// Remove inactive gxsIds
			// XXX: implement...

			// XXX: send joined_lobby if participating friend connected... maybe do it when he actually connects!

			sendKeepAliveIfNeeded(chatRoom);

			sendConnectionChallengeIfNeeded(chatRoom);
		});

		// XXX: remove outdated rooms visible lobbies

		// XXX: add the rest of the handling...
		// XXX: note that some events can be handled better by being sent directly do the peer upon action (eg. join is already on auto subscribe)
		// XXX: also. maybe it's better to have several scheduledAtFixedRate() instead of doing each 10 seconds + checks and storage...  with a schelder, while it does do "pulses", the only case where it's more
		// efficient to do it the RS way is when we join a channel in the middle of a session, which happens rarely
	}

	private void askForNearbyChatRooms(PeerConnection peerConnection)
	{
		writeItem(peerConnection, new ChatRoomListRequestItem());
	}

	private void sendKeepAliveIfNeeded(ChatRoom chatRoom)
	{
		var now = Instant.now();

		if (Duration.between(chatRoom.getLastKeepAlivePacket(), now).compareTo(KEEPALIVE_DELAY) > 0)
		{
			sendChatRoomEvent(chatRoom, ChatRoomEvent.KEEP_ALIVE);
			chatRoom.setLastKeepAlivePacket(now);
		}
	}

	private void sendConnectionChallengeIfNeeded(ChatRoom chatRoom)
	{
		if (chatRoom.getConnectionChallengeCountAndIncrease() > CONNECTION_CHALLENGE_COUNT_MIN &&
				Duration.between(chatRoom.getLastConnectionChallenge(), Instant.now()).compareTo(CONNECTION_CHALLENGE_MIN_DELAY) > 0)
		{
			chatRoom.resetConnectionChallengeCount();

			long recentMessage = chatRoom.getMessageCache().getRecentMessage();
			if (recentMessage == 0)
			{
				log.debug("No message in cache to send connection challenge. Not enough activity?");
				return;
			}

			// Send connection challenge to all connected friends
			peerConnectionManager.doForAllPeers(peerConnection -> writeItem(peerConnection, new ChatRoomConnectChallengeItem(peerConnection.getLocation().getLocationId(), chatRoom.getId(), recentMessage)), this);
		}
	}

	// XXX: not sure that thing works well enough... it has to be called early before packets start coming
	private void subscribeToAllSavedRooms()
	{
		log.debug("doing the subscribe thing");
		var roomListMessage = new ChatRoomListMessage();

		chatRoomService.getAllChatRoomsPendingToSubscribe().forEach(savedRoom -> {
			var chatRoom = new ChatRoom(
					savedRoom.getRoomId(),
					savedRoom.getName(),
					savedRoom.getTopic(),
					savedRoom.getFlags().contains(RoomFlags.PUBLIC) ? RoomType.PUBLIC : RoomType.PRIVATE,
					1,
					savedRoom.getFlags().contains(RoomFlags.PGP_SIGNED)
			); // XXX: add copy constructor?
			log.debug("adding room {}", chatRoom);
			roomListMessage.add(chatRoom.getAsRoomInfo());
			availableChatRooms.put(chatRoom.getId(), chatRoom);
		});

		peerConnectionManager.sendToSubscriptions(CHAT_PATH, CHAT_ROOM_LIST, roomListMessage);

		availableChatRooms.forEach((chatRoomId, chatRoom) -> joinChatRoom(chatRoomId));
	}

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
	}

	private void handleChatRoomListItem(PeerConnection peerConnection, ChatRoomListItem item)
	{
		log.debug("Received chat room list from {}", peerConnection);
		if (item.getChatRooms().size() > CHATROOM_LIST_MAX)
		{
			log.warn("Location {} is sending a chat room list of {} items, which is bigger than the allowed {}", peerConnection, item.getChatRooms().size(), CHATROOM_LIST_MAX);
		}
		var roomListMessage = new ChatRoomListMessage();
		item.getChatRooms().stream()
				.limit(CHATROOM_LIST_MAX)
				.forEach(visibleRoom -> {
					var chatRoom = new ChatRoom(
							visibleRoom.getId(),
							visibleRoom.getName(),
							visibleRoom.getTopic(),
							visibleRoom.getFlags().contains(RoomFlags.PUBLIC) ? RoomType.PUBLIC : RoomType.PRIVATE,
							visibleRoom.getCount(),
							visibleRoom.getFlags().contains(RoomFlags.PGP_SIGNED)
					);
					chatRoom.addParticipatingPeer(peerConnection);
					roomListMessage.add(chatRoom.getAsRoomInfo());
					availableChatRooms.put(chatRoom.getId(), chatRoom);
				});

		peerConnectionManager.sendToSubscriptions(CHAT_PATH, CHAT_ROOM_LIST, roomListMessage);

		chatRoomService.getAllChatRoomsPendingToSubscribe().forEach(chatRoom -> joinChatRoom(chatRoom.getRoomId()));
	}

	private void handleChatRoomListRequestItem(PeerConnection peerConnection)
	{
		var chatRoomListItem = new ChatRoomListItem(chatRooms.values().stream()
				.filter(chatRoom -> chatRoom.isPublic()
						|| chatRoom.getPreviouslyKnownLocations().contains(peerConnection.getLocation().getLocationId())
						|| chatRoom.getParticipatingPeers().contains(peerConnection))
				.map(ChatRoom::getAsVisibleChatRoomInfo)
				.toList());

		writeItem(peerConnection, chatRoomListItem);
	}

	private void handleChatRoomMessageItem(PeerConnection peerConnection, ChatRoomMessageItem item)
	{
		if (!validateExpiration(item.getSendTime()))
		{
			log.warn("Received message from peer {} failed time validation, dropping", peerConnection);
		}

		if (!validateChatRoomBounce(peerConnection, item))
		{
			return;
		}

		var chatRoom = chatRooms.get(item.getRoomId());

		// And display the message for us
		var chatRoomMessage = new ChatRoomMessage(item.getSenderNickname(), parseIncomingText(item.getMessage()));
		peerConnectionManager.sendToSubscriptions(CHAT_PATH, CHAT_ROOM_MESSAGE, item.getRoomId(), chatRoomMessage);

		chatRoom.incrementConnectionChallengeCount(); // XXX: this allows to find out when to send challenges. do that (where?)
	}

	private void handleChatRoomEventItem(PeerConnection peerConnection, ChatRoomEventItem item)
	{
		if (!validateExpiration(item.getSendTime()))
		{
			log.warn("Received message from peer {} failed time validation, dropping", peerConnection);
		}

		if (!validateChatRoomBounce(peerConnection, item))
		{
			return;
		}

		// XXX: addTimeShiftStatistics()... why isn't this done for messages as well? it just displays a warning anyway (and it's disabled in RS so it does nothing)

		// XXX: add routing clue

		if (item.getEventType() == ChatRoomEvent.PEER_LEFT.getCode())
		{
			var chatRoomUserEvent = new ChatRoomUserEvent(item.getSignature().getGxsId(), item.getSenderNickname());
			peerConnectionManager.sendToSubscriptions(CHAT_PATH, CHAT_ROOM_USER_LEAVE, item.getRoomId(), chatRoomUserEvent);
		}
		else if (item.getEventType() == ChatRoomEvent.PEER_JOINED.getCode())
		{
			// XXX: send a keep alive event to the participant so that he knows we are in the room (RS sends to everyone but that's lame)
			var chatRoomUserEvent = new ChatRoomUserEvent(item.getSignature().getGxsId(), item.getSenderNickname());
			peerConnectionManager.sendToSubscriptions(CHAT_PATH, CHAT_ROOM_USER_JOIN, item.getRoomId(), chatRoomUserEvent);
			//sendKeepAliveIfNeeded(chatRooms.get(item.getRoomId()));
		}
		else if (item.getEventType() == ChatRoomEvent.KEEP_ALIVE.getCode())
		{
			var chatRoomUserEvent = new ChatRoomUserEvent(item.getSignature().getGxsId(), item.getSenderNickname());
			peerConnectionManager.sendToSubscriptions(CHAT_PATH, CHAT_ROOM_USER_KEEP_ALIVE, item.getRoomId(), chatRoomUserEvent);
		}
		else if (item.getEventType() == ChatRoomEvent.PEER_STATUS.getCode())
		{
			var chatRoomMessage = new ChatRoomMessage(item.getSenderNickname(), null);
			peerConnectionManager.sendToSubscriptions(CHAT_PATH, CHAT_ROOM_TYPING_NOTIFICATION, item.getRoomId(), chatRoomMessage);
		}
	}

	private boolean validateChatRoomBounce(PeerConnection peerConnection, ChatRoomBounce item)
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
			log.error("Invalid signature for item from peer {}, dropping", peerConnection);
			return false;
		}

		// XXX: add routing clue (ie. best peer for channel)

		// XXX: check if it's for signed lobby. need to get the key and check it

		if (!bounce(peerConnection, item))
		{
			return false;
		}
		return true;
	}

	private void handleChatRoomUnsubscribeItem(PeerConnection peerConnection, ChatRoomUnsubscribeItem item)
	{
		var chatRoom = chatRooms.get(item.getRoomId());
		if (chatRoom == null)
		{
			log.error("Cannot unsubscribe peer from chat room {} as we're not in it", log.isErrorEnabled() ? Id.toStringLowerCase(item.getRoomId()) : null);
			return;
		}

		chatRoom.removeParticipatingPeer(peerConnection);

		// XXX: RS has some "previously_known_peers"... see if it's useful
	}

	private void handleChatRoomInviteItem(PeerConnection peerConnection, ChatRoomInviteItem item)
	{
		log.debug("Received invite from {} to room {}", peerConnection, item.getRoomName());

		var chatRoom = chatRooms.get(item.getRoomId());
		if (chatRoom != null)
		{
			if (!item.isConnectionChallenge() && (chatRoom.isPublic() != item.isPublic() || chatRoom.isSigned() != item.isSigned()))
			{
				log.debug("Not a matching item");
				return;
			}

			log.debug("Adding peer {} to chat room {}", peerConnection, chatRoom);

			chatRoom.addParticipatingPeer(peerConnection);
		}
		else
		{
			if (!item.isConnectionChallenge())
			{
				// XXX: prompt the user for a lobby invite, store it somewhere if it's accepted to be able to join
				log.debug("Is a lobby invite to join a new room");
			}
		}
	}

	private void handleChatStatusItem(PeerConnection peerConnection, ChatStatusItem item)
	{
		// There's a whole protocol with the flags (REQUEST_CUSTOM_STATE, CUSTOM_STATE and CUSTOM_STATE_AVAILABLE)
		// to send and request states; but it seems all RS does is send the typing state every
		// 5 seconds while the user is typing.
		log.debug("Got status item from peer {} with status string: {}", peerConnection, item.getStatus());
		if (MESSAGE_TYPING_CONTENT.equals(item.getStatus()))
		{
			var privateChatMessage = new PrivateChatMessage();
			peerConnectionManager.sendToSubscriptions(CHAT_PATH, CHAT_TYPING_NOTIFICATION, peerConnection.getLocation().getLocationId(), privateChatMessage);
		}
		else
		{
			log.warn("Unknown status item from peer {}, status: {}, flags: {}", peerConnection, item.getStatus(), item.getFlags());
		}
	}

	private void handleChatMessageItem(PeerConnection peerConnection, ChatMessageItem item)
	{
		if (item.isPrivate() && !item.isAvatarRequest()) // XXX: handle avatars later
		{
			var privateChatMessage = new PrivateChatMessage(parseIncomingText(item.getMessage()));
			peerConnectionManager.sendToSubscriptions(CHAT_PATH, CHAT_PRIVATE_MESSAGE, peerConnection.getLocation().getLocationId(), privateChatMessage);
		}
	}

	private void handleChatRoomConnectChallengeItem(PeerConnection peerConnection, ChatRoomConnectChallengeItem item)
	{
		var locationId = peerConnection.getLocation().getLocationId();

		for (ChatRoom chatRoom : chatRooms.values())
		{
			if (chatRoom.getMessageCache().hasConnectionChallenge(locationId, chatRoom.getId(), item.getChallengeCode()))
			{
				log.debug("Challenge accepted for chatroom {}, sending connection request to peer {}", chatRoom, peerConnection);
				chatRoom.addParticipatingPeer(peerConnection);
				invitePeerToChatRoom(peerConnection, chatRoom, Invitation.FROM_CHALLENGE);
				return;
			}
		}
	}

	private void invitePeerToChatRoom(PeerConnection peerConnection, ChatRoom chatRoom, Invitation invitation)
	{
		var item = new ChatRoomInviteItem(
				chatRoom.getId(),
				chatRoom.getName(),
				chatRoom.getTopic(),
				invitation == Invitation.FROM_CHALLENGE ? EnumSet.of(RoomFlags.CHALLENGE) : chatRoom.getRoomFlags());
		peerConnectionManager.writeItem(peerConnection, item, this);
	}

	private void signalChatRoomLeave(PeerConnection peerConnection, ChatRoom chatRoom)
	{
		var item = new ChatRoomUnsubscribeItem(chatRoom.getId());
		peerConnectionManager.writeItem(peerConnection, item, this);
	}

	private void initializeBounce(ChatRoom chatRoom, ChatRoomBounce bounce)
	{
		try (var session = new DatabaseSession(databaseSessionManager))
		{
			var ownIdentity = identityService.getOwnIdentity();

			bounce.setRoomId(chatRoom.getId());
			bounce.setMessageId(chatRoom.getNewMessageId());
			bounce.setSenderNickname(ownIdentity.getGxsIdGroupItem().getName()); // XXX: we should use the identity in chatRoom.getGxsId() once we have multiple identities support done properly

			byte[] signature = identityService.signData(ownIdentity, getBounceData(bounce));

			bounce.setSignature(new Signature(ownIdentity.getGxsIdGroupItem().getGxsId(), signature));
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
			chatRoom.addParticipatingPeer(peerConnection); // If we didn't receive the list yet, it means he's participating still
		}

		if (chatRoom.getMessageCache().exists(bounce.getMessageId()))
		{
			log.warn("Message id {} already received, dropping", bounce.getMessageId());
			chatRoom.getMessageCache().update(bounce.getMessageId()); // prevent echoes
			return false;
		}

		chatRoom.getMessageCache().add(bounce.getMessageId());
		chatRoom.updateActivity();

		// XXX: check for antiflood

		// Send to everyone except the originating peer
		chatRoom.getParticipatingPeers().forEach(peer -> {
			if (!Objects.equals(peer, peerConnection))
			{
				writeItem(peer, bounce);
			}
		});

		chatRoom.incrementConnectionChallengeCount();

		return true;
	}

	private boolean validateBounceSignature(PeerConnection peerConnection, ChatRoomBounce bounce)
	{
		var gxsGroup = gxsIdService.getGxsGroup(peerConnection, bounce.getSignature().getGxsId());
		// XXX: getBounceData() won't work for an incoming buffer! because serializeItemForSignature() sets it as outgoing... it needs to be copied or so
//		if (gxsGroup != null)
//		{
//			return RSA.verify(gxsGroup.getPublishingPublicKey(), bounce.getSignature().getData(), getBounceData(bounce));
//		}
		return true; // if we don't have the identity yet, we let the item pass because it could be valid, and it's impossible to impersonate an identity this way
	}

	private boolean isBanned(GxsId gxsId)
	{
		// XXX: implement by using the reputation level
		return false;
	}

	private byte[] getBounceData(ChatRoomBounce chatRoomBounce)
	{
		var buffer = peerConnectionManager.serializeItemForSignature(chatRoomBounce, this);
		var data = new byte[buffer.writerIndex()];
		buffer.getBytes(0, data);
		buffer.release();
		return data;
	}

	/**
	 * Checks if a message is well within our own time.
	 *
	 * @param sendTime the time the message was sent at, in seconds from 1970-01-01 UTC
	 * @return true if within bounds
	 */
	private boolean validateExpiration(int sendTime)
	{
		var now = Instant.now();
		if (sendTime < now.getEpochSecond() + TIME_DRIFT_PAST_MAX.toSeconds() - KEEP_MESSAGE_RECORD_MAX.toSeconds())
		{
			return false;
		}

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
		peerConnectionManager.doForAllPeers(peerConnection -> writeItem(peerConnection, chatMessageItem), this);
	}

	/**
	 * Sends a private message to a peer.
	 *
	 * @param locationId the location id
	 * @param message    the message
	 */
	@Transactional(readOnly = true)
	public void sendPrivateMessage(LocationId locationId, String message)
	{
		var location = locationService.findLocationById(locationId).orElseThrow();
		writeItem(location, new ChatMessageItem(message, EnumSet.of(ChatFlags.PRIVATE)));
	}

	/**
	 * Sends a typing notification for private messages to a peer.
	 *
	 * @param locationId the location id
	 */
	@Transactional(readOnly = true)
	public void sendPrivateTypingNotification(LocationId locationId)
	{
		var location = locationService.findLocationById(locationId).orElseThrow();
		writeItem(location, new ChatStatusItem(MESSAGE_TYPING_CONTENT, EnumSet.of(ChatFlags.PRIVATE)));
	}

	/**
	 * Sets the status message (the one appearing at the top of the profile peer; for example, "I'm eating", "Gone for a walk", etc...).
	 *
	 * @param message the status message
	 */
	public void setStatusMessage(String message)
	{
		peerConnectionManager.doForAllPeers(peerConnection -> writeItem(peerConnection, new ChatStatusItem(message, EnumSet.of(ChatFlags.CUSTOM_STATE))), this);
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

		var chatRoom = availableChatRooms.get(chatRoomId);
		if (chatRoom == null)
		{
			log.warn("Chatroom {} doesn't exist, can't join.", log.isWarnEnabled() ? Id.toStringLowerCase(chatRoomId) : null);
			return;
		}
		chatRooms.put(chatRoomId, chatRoom);

		try (var ignored = new DatabaseSession(databaseSessionManager)) // XXX: ugly, it's because we can be called from a lambda.. make it take the arguments later (needed for multi identity support)
		{
			var ownIdentity = identityService.getOwnIdentity(); // XXX: allow multiple identities later on
			chatRoomService.subscribeToChatRoomAndJoin(chatRoom, ownIdentity);

			chatRoom.getParticipatingPeers().forEach(peer -> invitePeerToChatRoom(peer, chatRoom, Invitation.PLAIN));

			peerConnectionManager.sendToSubscriptions(CHAT_PATH, CHAT_ROOM_JOIN, chatRoom.getId(), new ChatRoomMessage());

			sendChatRoomEvent(chatRoom, ChatRoomEvent.PEER_JOINED);

			// Send a keep alive event from ourselves so that we are added to the user list in the UI
			var chatRoomUserEvent = new ChatRoomUserEvent(ownIdentity.getGxsIdGroupItem().getGxsId(), ownIdentity.getGxsIdGroupItem().getName());
			peerConnectionManager.sendToSubscriptions(CHAT_PATH, CHAT_ROOM_USER_KEEP_ALIVE, chatRoom.getId(), chatRoomUserEvent);
		}
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
		sendChatRoomEvent(chatRoomToRemove, ChatRoomEvent.PEER_LEFT); // XXX: produces an exception!
		chatRooms.remove(chatRoomId);
		chatRoomService.unsubscribeFromChatRoomAndLeave(chatRoomId, identityService.getOwnIdentity()); // XXX: allow multiple identities

		chatRoomToRemove.getParticipatingPeers().forEach(peer -> signalChatRoomLeave(peer, chatRoomToRemove));
		peerConnectionManager.sendToSubscriptions(CHAT_PATH, CHAT_ROOM_LEAVE, chatRoomToRemove.getId(), new ChatRoomMessage());

		// XXX: find a way to remove ourselves from the UI...
	}

	public long createChatRoom(String roomName, String topic, GxsId identity, Set<RoomFlags> flags)
	{
		var newChatRoom = new ChatRoom(
				createUniqueRoomId(),
				roomName,
				topic,
				RoomType.PUBLIC, // XXX: for now
				1,
				false); // XXX: for now

		availableChatRooms.put(newChatRoom.getId(), newChatRoom);

		joinChatRoom(newChatRoom.getId());

		// XXX: we could invite friends in there... supply a list of friends as parameter

		return newChatRoom.getId();
	}

	private long createUniqueRoomId()
	{
		long newId;

		do
		{
			newId = ThreadLocalRandom.current().nextLong();
		}
		while (availableChatRooms.containsKey(newId) || chatRooms.containsKey(newId));

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
		bounce(chatRoomEvent);
	}

	private String parseIncomingText(String text)
	{
		return Jsoup.clean(text, Safelist.none()
				.addAttributes("img", "src")
				.addProtocols("img", "src", "data")
				.preserveRelativeLinks(true));
	}
}
