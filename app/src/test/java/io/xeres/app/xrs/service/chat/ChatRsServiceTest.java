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
import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.MessageService;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.chat.item.ChatMessageItem;
import io.xeres.app.xrs.service.chat.item.ChatRoomListItem;
import io.xeres.app.xrs.service.chat.item.ChatRoomListRequestItem;
import io.xeres.common.message.MessageType;
import io.xeres.common.message.chat.ChatMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.EnumSet;

import static io.xeres.common.message.MessagePath.chatPrivateDestination;
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
	private IdentityService identityService;

	@Mock
	private ChatRoomService chatRoomService;

	@Mock
	private ChatBacklogService chatBacklogService;

	@Mock
	private MessageService messageService;

	@InjectMocks
	private ChatRsService chatRsService;

	@Test
	void HandleChatMessageItem_Success()
	{
		var message = "hello";
		var peerConnection = new PeerConnection(LocationFakes.createLocation(), null);

		var item = new ChatMessageItem(message, EnumSet.of(ChatFlags.PRIVATE));

		chatRsService.handleItem(peerConnection, item);

		verify(messageService).sendToConsumers(eq(chatPrivateDestination()), eq(MessageType.CHAT_PRIVATE_MESSAGE), eq(peerConnection.getLocation().getLocationId()), argThat(chatMessage -> {
			assertNotNull(chatMessage);
			assertEquals(message, ((ChatMessage) (chatMessage)).getContent());
			return true;
		}));
	}

	@Test
	void HandleChatMessageItem_Partial_Success()
	{
		var message1 = "hello, ";
		var message2 = "world";
		var peerConnection = new PeerConnection(LocationFakes.createLocation(), null);

		var item1 = new ChatMessageItem(message1, EnumSet.of(ChatFlags.PRIVATE, ChatFlags.PARTIAL_MESSAGE));
		var item2 = new ChatMessageItem(message2, EnumSet.of(ChatFlags.PRIVATE));

		chatRsService.handleItem(peerConnection, item1);
		chatRsService.handleItem(peerConnection, item2);

		verify(messageService).sendToConsumers(eq(chatPrivateDestination()), eq(MessageType.CHAT_PRIVATE_MESSAGE), eq(peerConnection.getLocation().getLocationId()), argThat(chatMessage -> {
			assertNotNull(chatMessage);
			assertEquals(message1 + message2, ((ChatMessage) (chatMessage)).getContent());
			return true;
		}));
	}

	@Test
	void HandleChatRoomListRequestItem_Empty_Success()
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
	void HandleChatRoomListRequestItem_Success()
	{
		var roomName = "test";
		var roomTopic = "test topic";
		var roomFlags = EnumSet.of(RoomFlags.PUBLIC);

		var ownIdentity = IdentityFakes.createOwn();

		var peerConnection = new PeerConnection(LocationFakes.createLocation(), null);

		var item = new ChatRoomListRequestItem();

		when(identityService.getOwnIdentity()).thenReturn(ownIdentity);

		var roomId = chatRsService.createChatRoom(roomName, roomTopic, roomFlags, false);
		chatRsService.handleItem(peerConnection, item);

		verify(peerConnectionManager).writeItem(eq(peerConnection), argThat(chatRoomListItem -> {
			assertNotNull(chatRoomListItem);
			assertFalse(((ChatRoomListItem) chatRoomListItem).getChatRooms().isEmpty());
			assertEquals(roomId, ((ChatRoomListItem) chatRoomListItem).getChatRooms().getFirst().getId());
			return true;
		}), any(RsService.class));
	}
}
