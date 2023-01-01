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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.chat.ChatRoomFakes;
import io.xeres.app.database.model.identity.GxsIdFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ChatRoomRepositoryTest
{
	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Test
	void ChatRoomRepository_CRUD_OK()
	{
		var identity = GxsIdFakes.createOwnIdentity();

		var chatRoom1 = ChatRoomFakes.createChatRoomEntity(identity);
		var chatRoom2 = ChatRoomFakes.createChatRoomEntity(identity);
		var chatRoom3 = ChatRoomFakes.createChatRoomEntity(identity);

		chatRoom1.setSubscribed(true);
		chatRoom2.setSubscribed(true);
		chatRoom3.setSubscribed(false);

		var savedChatRoom1 = chatRoomRepository.save(chatRoom1);
		chatRoomRepository.save(chatRoom2);
		chatRoomRepository.save(chatRoom3);

		var chatRooms = chatRoomRepository.findAllBySubscribedTrueAndJoinedFalse();
		assertNotNull(chatRooms);
		assertEquals(2, chatRooms.size());

		var first = chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoom1.getRoomId(), identity).orElse(null);

		assertNotNull(first);
		assertEquals(savedChatRoom1.getId(), first.getId());
		assertEquals(savedChatRoom1.getName(), first.getName());

		first.setJoined(true);

		var updatedChatRoom = chatRoomRepository.save(first);

		assertNotNull(updatedChatRoom);
		assertEquals(first.getId(), updatedChatRoom.getId());
		assertTrue(updatedChatRoom.isJoined());

		chatRoomRepository.deleteById(first.getId());

		var deleted = chatRoomRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}
}
