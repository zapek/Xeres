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

import io.xeres.app.database.model.chat.DistantChatBacklog;
import io.xeres.app.database.model.gxs.IdentityGroupItemFakes;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Limit;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class DistantChatBacklogRepositoryTest
{
	@Autowired
	private GxsIdentityRepository gxsIdentityRepository;

	@Autowired
	private DistantChatBacklogRepository distantChatBacklogRepository;

	@Autowired
	private EntityManager entityManager;

	private void setCreated(String message, Instant created)
	{
		// Bulk update because @CreationTimestamp overrides the value on persist
		entityManager.createQuery("UPDATE DistantChatBacklog b SET b.created = :created WHERE b.message = :message")
				.setParameter("created", created)
				.setParameter("message", message)
				.executeUpdate();
		entityManager.clear();
	}

	@Test
	void CRUD_Success()
	{
		var identity = gxsIdentityRepository.save(IdentityGroupItemFakes.createIdentityGroupItem());

		var backlog1 = new DistantChatBacklog(identity, true, "hello");
		var backlog2 = new DistantChatBacklog(identity, false, "hi");

		distantChatBacklogRepository.save(backlog1);
		distantChatBacklogRepository.save(backlog2);

		entityManager.flush();
		entityManager.clear();

		var backlogs = distantChatBacklogRepository.findAll();
		assertNotNull(backlogs);
		assertEquals(2, backlogs.size());

		var first = distantChatBacklogRepository.findById(backlogs.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertTrue(first.isOwn());
		assertEquals("hello", first.getMessage());

		first.setMessage("updated");

		var updatedBacklog = distantChatBacklogRepository.save(first);

		assertNotNull(updatedBacklog);
		assertEquals(first.getId(), updatedBacklog.getId());
		assertEquals("updated", updatedBacklog.getMessage());

		distantChatBacklogRepository.deleteById(first.getId());

		var deleted = distantChatBacklogRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindAllByIdentityGroupItemAndCreatedAfterOrderByCreatedDesc_Success()
	{
		var identity1 = gxsIdentityRepository.save(IdentityGroupItemFakes.createIdentityGroupItem());
		var identity2 = gxsIdentityRepository.save(IdentityGroupItemFakes.createIdentityGroupItem());

		var now = Instant.now();

		distantChatBacklogRepository.save(new DistantChatBacklog(identity1, true, "old message"));
		distantChatBacklogRepository.save(new DistantChatBacklog(identity1, false, "recent message"));
		distantChatBacklogRepository.save(new DistantChatBacklog(identity2, false, "other identity message"));

		setCreated("old message", now.minusSeconds(3600));
		setCreated("recent message", now.minusSeconds(60));

		// Only recent messages of identity1 are returned, newest first
		var found = distantChatBacklogRepository.findAllByIdentityGroupItemAndCreatedAfterOrderByCreatedDesc(identity1, now.minusSeconds(120), Limit.unlimited());

		assertNotNull(found);
		assertEquals(1, found.size());
		assertEquals("recent message", found.getFirst().getMessage());

		// All messages of identity1 are returned when the range covers everything
		var all = distantChatBacklogRepository.findAllByIdentityGroupItemAndCreatedAfterOrderByCreatedDesc(identity1, Instant.EPOCH, Limit.unlimited());

		assertNotNull(all);
		assertEquals(2, all.size());
		assertEquals("recent message", all.getFirst().getMessage()); // newest first
		assertEquals("old message", all.getLast().getMessage());

		// Messages from other identities are not included
		var otherIdentity = distantChatBacklogRepository.findAllByIdentityGroupItemAndCreatedAfterOrderByCreatedDesc(identity2, Instant.EPOCH, Limit.unlimited());

		assertNotNull(otherIdentity);
		assertEquals(1, otherIdentity.size());
		assertEquals("other identity message", otherIdentity.getFirst().getMessage());
	}

	@Test
	void FindAllByIdentityGroupItemAndCreatedAfterOrderByCreatedDesc_WithLimit_Success()
	{
		var identity = gxsIdentityRepository.save(IdentityGroupItemFakes.createIdentityGroupItem());

		var now = Instant.now();

		distantChatBacklogRepository.save(new DistantChatBacklog(identity, true, "first"));
		distantChatBacklogRepository.save(new DistantChatBacklog(identity, false, "second"));

		setCreated("first", now.minusSeconds(3600));
		setCreated("second", now.minusSeconds(60));

		var limited = distantChatBacklogRepository.findAllByIdentityGroupItemAndCreatedAfterOrderByCreatedDesc(identity, Instant.EPOCH, Limit.of(1));

		assertNotNull(limited);
		assertEquals(1, limited.size());
		assertEquals("second", limited.getFirst().getMessage()); // newest first
	}

	@Test
	void DeleteAllByCreatedBefore_Success()
	{
		var identity = gxsIdentityRepository.save(IdentityGroupItemFakes.createIdentityGroupItem());

		var now = Instant.now();

		distantChatBacklogRepository.save(new DistantChatBacklog(identity, true, "old"));
		distantChatBacklogRepository.save(new DistantChatBacklog(identity, false, "recent"));

		setCreated("old", now.minusSeconds(3600));
		setCreated("recent", now.minusSeconds(60));

		distantChatBacklogRepository.deleteAllByCreatedBefore(now.minusSeconds(120));

		var remaining = distantChatBacklogRepository.findAll();

		assertEquals(1, remaining.size());
		assertEquals("recent", remaining.getFirst().getMessage());
	}

	@Test
	void DeleteAllByIdentityGroupItem_Success()
	{
		var identity1 = gxsIdentityRepository.save(IdentityGroupItemFakes.createIdentityGroupItem());
		var identity2 = gxsIdentityRepository.save(IdentityGroupItemFakes.createIdentityGroupItem());

		distantChatBacklogRepository.save(new DistantChatBacklog(identity1, true, "hello"));
		distantChatBacklogRepository.save(new DistantChatBacklog(identity2, false, "hi"));

		distantChatBacklogRepository.deleteAllByIdentityGroupItem(identity1);

		var remaining = distantChatBacklogRepository.findAll();

		assertEquals(1, remaining.size());
		assertEquals("hi", remaining.getFirst().getMessage());
	}
}
