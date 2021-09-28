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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.chatroom.ChatRoom;
import io.xeres.app.database.model.chatroom.ChatRoomFakes;
import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.common.identity.Type;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ChatRoomRepositoryTest
{
	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Test
	void ChatRoomRepository_CRUD_OK()
	{
		var identity = IdentityFakes.createOwnIdentity("test", Type.SIGNED);

		var chatRoom1 = ChatRoomFakes.createChatRoom(identity);
		var chatRoom2 = ChatRoomFakes.createChatRoom(identity);
		var chatRoom3 = ChatRoomFakes.createChatRoom(identity);

		chatRoom1.setSubscribed(true);
		chatRoom2.setSubscribed(true);
		chatRoom3.setSubscribed(false);

		ChatRoom savedChatRoom1 = chatRoomRepository.save(chatRoom1);
		chatRoomRepository.save(chatRoom2);
		chatRoomRepository.save(chatRoom3);

		List<ChatRoom> chatRooms = chatRoomRepository.findAllBySubscribedTrueAndJoinedFalse();
		assertNotNull(chatRooms);
		assertEquals(2, chatRooms.size());

		ChatRoom first = chatRoomRepository.findByRoomIdAndIdentity(chatRoom1.getRoomId(), identity).orElse(null);

		assertNotNull(first);
		assertEquals(savedChatRoom1.getId(), first.getId());
		assertEquals(savedChatRoom1.getName(), first.getName());

		first.setJoined(true);

		ChatRoom updatedChatRoom = chatRoomRepository.save(first);

		assertNotNull(updatedChatRoom);
		assertEquals(first.getId(), updatedChatRoom.getId());
		assertTrue(updatedChatRoom.isJoined());

		chatRoomRepository.deleteById(first.getId());

		Optional<ChatRoom> deleted = chatRoomRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}
}
