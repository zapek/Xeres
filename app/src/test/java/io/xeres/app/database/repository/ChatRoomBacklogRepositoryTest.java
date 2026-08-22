/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.chat.ChatRoom;
import io.xeres.app.database.model.chat.ChatRoomBacklog;
import io.xeres.app.database.model.chat.ChatRoomFakes;
import io.xeres.app.database.model.gxs.IdentityGroupItemFakes;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Limit;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ChatRoomBacklogRepositoryTest
{
	@Autowired
	private GxsIdentityRepository gxsIdentityRepository;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private ChatRoomBacklogRepository chatRoomBacklogRepository;

	@Autowired
	private EntityManager entityManager;

	private ChatRoom saveChatRoom()
	{
		var identity = gxsIdentityRepository.save(IdentityGroupItemFakes.createIdentityGroupItem());
		return chatRoomRepository.save(ChatRoomFakes.createChatRoomEntity(identity));
	}

	private void setCreated(String message, Instant created)
	{
		// Bulk update because @CreationTimestamp overrides the value on persist
		entityManager.createQuery("UPDATE ChatRoomBacklog b SET b.created = :created WHERE b.message = :message")
				.setParameter("created", created)
				.setParameter("message", message)
				.executeUpdate();
		entityManager.clear();
	}

	@Test
	void CRUD_Success()
	{
		var chatRoom = saveChatRoom();

		var backlog1 = new ChatRoomBacklog(chatRoom, "alice", "hello");
		var backlog2 = new ChatRoomBacklog(chatRoom, "bob", "hi");

		chatRoomBacklogRepository.save(backlog1);
		chatRoomBacklogRepository.save(backlog2);

		entityManager.flush();
		entityManager.clear();

		var backlogs = chatRoomBacklogRepository.findAll();
		assertNotNull(backlogs);
		assertEquals(2, backlogs.size());

		var first = chatRoomBacklogRepository.findById(backlogs.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals("alice", first.getNickname());
		assertEquals("hello", first.getMessage());

		first.setMessage("updated");

		var updatedBacklog = chatRoomBacklogRepository.save(first);

		assertNotNull(updatedBacklog);
		assertEquals(first.getId(), updatedBacklog.getId());
		assertEquals("updated", updatedBacklog.getMessage());

		chatRoomBacklogRepository.deleteById(first.getId());

		var deleted = chatRoomBacklogRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindAllByRoomAndCreatedAfterOrderByCreatedDesc_Success()
	{
		var room1 = saveChatRoom();
		var room2 = saveChatRoom();

		var now = Instant.now();

		chatRoomBacklogRepository.save(new ChatRoomBacklog(room1, "alice", "old message"));
		chatRoomBacklogRepository.save(new ChatRoomBacklog(room1, "bob", "recent message"));
		chatRoomBacklogRepository.save(new ChatRoomBacklog(room2, "carol", "other room message"));

		setCreated("old message", now.minusSeconds(3600));
		setCreated("recent message", now.minusSeconds(60));

		// Only recent messages of room1 are returned, newest first
		var found = chatRoomBacklogRepository.findAllByRoomAndCreatedAfterOrderByCreatedDesc(room1, now.minusSeconds(120), Limit.unlimited());

		assertNotNull(found);
		assertEquals(1, found.size());
		assertEquals("recent message", found.getFirst().getMessage());

		// All messages of room1 are returned when the range covers everything
		var all = chatRoomBacklogRepository.findAllByRoomAndCreatedAfterOrderByCreatedDesc(room1, Instant.EPOCH, Limit.unlimited());

		assertNotNull(all);
		assertEquals(2, all.size());
		assertEquals("recent message", all.getFirst().getMessage()); // newest first
		assertEquals("old message", all.getLast().getMessage());

		// Messages from other rooms are not included
		var otherRoom = chatRoomBacklogRepository.findAllByRoomAndCreatedAfterOrderByCreatedDesc(room2, Instant.EPOCH, Limit.unlimited());

		assertNotNull(otherRoom);
		assertEquals(1, otherRoom.size());
		assertEquals("other room message", otherRoom.getFirst().getMessage());
	}

	@Test
	void FindAllByRoomAndCreatedAfterOrderByCreatedDesc_WithLimit_Success()
	{
		var room = saveChatRoom();

		var now = Instant.now();

		chatRoomBacklogRepository.save(new ChatRoomBacklog(room, "alice", "first"));
		chatRoomBacklogRepository.save(new ChatRoomBacklog(room, "bob", "second"));

		setCreated("first", now.minusSeconds(3600));
		setCreated("second", now.minusSeconds(60));

		var limited = chatRoomBacklogRepository.findAllByRoomAndCreatedAfterOrderByCreatedDesc(room, Instant.EPOCH, Limit.of(1));

		assertNotNull(limited);
		assertEquals(1, limited.size());
		assertEquals("second", limited.getFirst().getMessage()); // newest first
	}

	@Test
	void DeleteAllByCreatedBefore_Success()
	{
		var room = saveChatRoom();

		var now = Instant.now();

		chatRoomBacklogRepository.save(new ChatRoomBacklog(room, "alice", "old"));
		chatRoomBacklogRepository.save(new ChatRoomBacklog(room, "bob", "recent"));

		setCreated("old", now.minusSeconds(3600));
		setCreated("recent", now.minusSeconds(60));

		chatRoomBacklogRepository.deleteAllByCreatedBefore(now.minusSeconds(120));

		var remaining = chatRoomBacklogRepository.findAll();

		assertEquals(1, remaining.size());
		assertEquals("recent", remaining.getFirst().getMessage());
	}

	@Test
	void DeleteAllByRoom_Success()
	{
		var room1 = saveChatRoom();
		var room2 = saveChatRoom();

		chatRoomBacklogRepository.save(new ChatRoomBacklog(room1, "alice", "hello"));
		chatRoomBacklogRepository.save(new ChatRoomBacklog(room2, "bob", "hi"));

		chatRoomBacklogRepository.deleteAllByRoom(room1);

		var remaining = chatRoomBacklogRepository.findAll();

		assertEquals(1, remaining.size());
		assertEquals("hi", remaining.getFirst().getMessage());
	}
}
