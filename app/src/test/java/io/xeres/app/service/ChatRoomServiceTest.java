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

package io.xeres.app.service;

import io.xeres.app.database.model.chat.ChatRoom;
import io.xeres.app.database.model.chat.ChatRoomFakes;
import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.app.database.repository.ChatRoomRepository;
import io.xeres.common.message.chat.RoomType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ChatRoomServiceTest
{
	@Mock
	private ChatRoomRepository chatRoomRepository;

	@InjectMocks
	private ChatRoomService chatRoomService;

	@Test
	void ChatRoomService_CreateChatRoom_OK()
	{
		chatRoomService.createChatRoom(createSignedChatRoom(), IdentityFakes.createOwnIdentity());
		verify(chatRoomRepository).save(any(ChatRoom.class));
	}

	@Test
	void ChatRoomService_SubscribeToChatRoomAndJoin_OK()
	{
		var serviceChatRoom = createSignedChatRoom();
		var identity = IdentityFakes.createOwnIdentity();
		var chatRoom = ChatRoomFakes.createChatRoomEntity(serviceChatRoom.getId(), identity, serviceChatRoom.getName(), serviceChatRoom.getTopic(), 0);

		when(chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoom.getRoomId(), identity)).thenReturn(Optional.of(chatRoom));
		when(chatRoomRepository.save(chatRoom)).thenReturn(chatRoom);

		var subscribedChatRoom = chatRoomService.subscribeToChatRoomAndJoin(serviceChatRoom, identity);

		assertTrue(subscribedChatRoom.isSubscribed());
		assertTrue(subscribedChatRoom.isJoined());

		verify(chatRoomRepository).findByRoomIdAndIdentityGroupItem(chatRoom.getRoomId(), identity);
		verify(chatRoomRepository).save(subscribedChatRoom);
	}

	@Test
	void ChatRoomService_UnsubscribeFromChatRoomAndLeave_OK()
	{
		var serviceChatRoom = createSignedChatRoom();
		var identity = IdentityFakes.createOwnIdentity();
		var chatRoom = ChatRoomFakes.createChatRoomEntity(serviceChatRoom.getId(), identity, serviceChatRoom.getName(), serviceChatRoom.getTopic(), 0);

		when(chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoom.getRoomId(), identity)).thenReturn(Optional.of(chatRoom));
		when(chatRoomRepository.save(chatRoom)).thenReturn(chatRoom);

		var unsubscribedChatRoom = chatRoomService.unsubscribeFromChatRoomAndLeave(serviceChatRoom.getId(), identity);

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
