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

package io.xeres.app.xrs.service.chat;

import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.chat.ChatRoom;
import io.xeres.app.database.model.chat.ChatRoomFakes;
import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.repository.ChatRoomRepository;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.chat.item.ChatMessageItem;
import io.xeres.app.xrs.service.chat.item.ChatRoomListItem;
import io.xeres.app.xrs.service.chat.item.ChatRoomListRequestItem;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.common.message.MessageType;
import io.xeres.common.message.chat.PrivateChatMessage;
import io.xeres.common.message.chat.RoomType;
import org.apache.commons.lang3.EnumUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.EnumSet;
import java.util.Optional;

import static io.xeres.common.rest.PathConfig.CHAT_PATH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ChatRsServiceTest
{
	@Mock
	private PeerConnectionManager peerConnectionManager;

	@Mock
	private DatabaseSessionManager databaseSessionManager;

	@Mock
	private IdentityRsService identityRsService;

	@Mock
	private ChatRoomRepository chatRoomRepository;

	@InjectMocks
	private ChatRsService chatRsService;

	@Test
	void ChatService_HandleChatMessageItem_OK()
	{
		var MESSAGE = "hello";
		var peerConnection = new PeerConnection(LocationFakes.createLocation(), null);

		var item = new ChatMessageItem(MESSAGE, EnumSet.of(ChatFlags.PRIVATE));

		chatRsService.handleItem(peerConnection, item);

		verify(peerConnectionManager).sendToClientSubscriptions(eq(CHAT_PATH), eq(MessageType.CHAT_PRIVATE_MESSAGE), eq(peerConnection.getLocation().getLocationId()), argThat(privateChatMessage -> {
			assertNotNull(privateChatMessage);
			assertEquals(MESSAGE, ((PrivateChatMessage) (privateChatMessage)).getContent());
			return true;
		}));
	}

	@Test
	void ChatService_HandleChatMessageItem_Partial_OK()
	{
		var MESSAGE1 = "hello, ";
		var MESSAGE2 = "world";
		var peerConnection = new PeerConnection(LocationFakes.createLocation(), null);

		var item1 = new ChatMessageItem(MESSAGE1, EnumSet.of(ChatFlags.PRIVATE, ChatFlags.PARTIAL_MESSAGE));
		var item2 = new ChatMessageItem(MESSAGE2, EnumSet.of(ChatFlags.PRIVATE));

		chatRsService.handleItem(peerConnection, item1);
		chatRsService.handleItem(peerConnection, item2);

		verify(peerConnectionManager).sendToClientSubscriptions(eq(CHAT_PATH), eq(MessageType.CHAT_PRIVATE_MESSAGE), eq(peerConnection.getLocation().getLocationId()), argThat(privateChatMessage -> {
			assertNotNull(privateChatMessage);
			assertEquals(MESSAGE1 + MESSAGE2, ((PrivateChatMessage) (privateChatMessage)).getContent());
			return true;
		}));
	}

	@Test
	void ChatService_HandleChatRoomListRequestItem_Empty_OK()
	{
		var peerConnection = new PeerConnection(LocationFakes.createLocation(), null);

		var item = new ChatRoomListRequestItem();

		chatRsService.handleItem(peerConnection, item);

		verify(peerConnectionManager).writeItem(eq(peerConnection), argThat(chatRoomListItem -> {
			assertNotNull(chatRoomListItem);
			assertTrue(((ChatRoomListItem) chatRoomListItem).getChatRooms().isEmpty());
			return true;
		}), any(RsService.class));
	}

	@Test
	void ChatService_HandleChatRoomListRequestItem_OK()
	{
		var roomName = "test";
		var roomTopic = "test topic";
		var roomFlags = EnumSet.of(RoomFlags.PUBLIC);

		var ownIdentity = IdentityFakes.createOwn();

		var peerConnection = new PeerConnection(LocationFakes.createLocation(), null);

		var item = new ChatRoomListRequestItem();

		when(identityRsService.getOwnIdentity()).thenReturn(ownIdentity);
		when(chatRoomRepository.save(any())).thenReturn(ChatRoomFakes.createChatRoomEntity(1L, ownIdentity, roomName, roomTopic, (int) EnumUtils.generateBitVector(RoomFlags.class, roomFlags)));

		var roomId = chatRsService.createChatRoom(roomName, roomTopic, roomFlags, false);
		chatRsService.handleItem(peerConnection, item);

		verify(peerConnectionManager).writeItem(eq(peerConnection), argThat(chatRoomListItem -> {
			assertNotNull(chatRoomListItem);
			assertFalse(((ChatRoomListItem) chatRoomListItem).getChatRooms().isEmpty());
			assertEquals(roomId, ((ChatRoomListItem) chatRoomListItem).getChatRooms().get(0).getId());
			return true;
		}), any(RsService.class));
	}

	@Test
	void ChatRoomService_CreateChatRoom_OK()
	{
		chatRsService.createChatRoom(createSignedChatRoom(), IdentityFakes.createOwn());
		verify(chatRoomRepository).save(any(ChatRoom.class));
	}

	@Test
	void ChatRoomService_SubscribeToChatRoomAndJoin_OK()
	{
		var serviceChatRoom = createSignedChatRoom();
		var identity = IdentityFakes.createOwn();
		var chatRoom = ChatRoomFakes.createChatRoomEntity(serviceChatRoom.getId(), identity, serviceChatRoom.getName(), serviceChatRoom.getTopic(), 0);

		when(chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoom.getRoomId(), identity)).thenReturn(Optional.of(chatRoom));
		when(chatRoomRepository.save(chatRoom)).thenReturn(chatRoom);

		var subscribedChatRoom = chatRsService.subscribeToChatRoomAndJoin(serviceChatRoom, identity);

		assertTrue(subscribedChatRoom.isSubscribed());
		assertTrue(subscribedChatRoom.isJoined());

		verify(chatRoomRepository).findByRoomIdAndIdentityGroupItem(chatRoom.getRoomId(), identity);
		verify(chatRoomRepository).save(subscribedChatRoom);
	}

	@Test
	void ChatRoomService_UnsubscribeFromChatRoomAndLeave_OK()
	{
		var serviceChatRoom = createSignedChatRoom();
		var identity = IdentityFakes.createOwn();
		var chatRoom = ChatRoomFakes.createChatRoomEntity(serviceChatRoom.getId(), identity, serviceChatRoom.getName(), serviceChatRoom.getTopic(), 0);

		when(chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoom.getRoomId(), identity)).thenReturn(Optional.of(chatRoom));
		when(chatRoomRepository.save(chatRoom)).thenReturn(chatRoom);

		var unsubscribedChatRoom = chatRsService.unsubscribeFromChatRoomAndLeave(serviceChatRoom.getId(), identity);

		assertFalse(unsubscribedChatRoom.isSubscribed());
		assertFalse(unsubscribedChatRoom.isJoined());

		verify(chatRoomRepository).findByRoomIdAndIdentityGroupItem(chatRoom.getRoomId(), identity);
		verify(chatRoomRepository).save(unsubscribedChatRoom);
	}

	private io.xeres.app.xrs.service.chat.ChatRoom createSignedChatRoom()
	{
		return new io.xeres.app.xrs.service.chat.ChatRoom(1L, "test", "something", RoomType.PUBLIC, 1, true);
	}
}
