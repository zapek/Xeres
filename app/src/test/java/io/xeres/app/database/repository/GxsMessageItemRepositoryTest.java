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

import io.xeres.app.database.model.gxs.ForumMessageItemFakes;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.common.util.DebugUtils;
import io.xeres.testutils.IdFakes;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GxsMessageItemRepositoryTest
{
	@Autowired
	private GxsMessageItemRepository gxsMessageItemRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void CRUD_Success()
	{
		var forumMessageItem1 = ForumMessageItemFakes.createForumMessageItem();
		var forumMessageItem2 = ForumMessageItemFakes.createForumMessageItem();
		var forumMessageItem3 = ForumMessageItemFakes.createForumMessageItem();

		var savedForumMessageItem1 = gxsMessageItemRepository.save(forumMessageItem1);
		gxsMessageItemRepository.save(forumMessageItem2);
		gxsMessageItemRepository.save(forumMessageItem3);

		var forumMessageItems = gxsMessageItemRepository.findAll();
		assertNotNull(forumMessageItems);
		assertEquals(3, forumMessageItems.size());

		var first = gxsMessageItemRepository.findById(forumMessageItems.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedForumMessageItem1.getId(), first.getId());
		assertEquals(savedForumMessageItem1.getName(), first.getName());

		first.setName("updated");

		var updatedForumMessageItem = gxsMessageItemRepository.save(first);

		assertNotNull(updatedForumMessageItem);
		assertEquals(first.getId(), updatedForumMessageItem.getId());
		assertEquals("updated", updatedForumMessageItem.getName());

		gxsMessageItemRepository.deleteById(first.getId());

		var deleted = gxsMessageItemRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindByGxsIdAndMsgId_Success()
	{
		var forumMessageItem = ForumMessageItemFakes.createForumMessageItem();
		gxsMessageItemRepository.save(forumMessageItem);

		var found = gxsMessageItemRepository.findByGxsIdAndMsgId(forumMessageItem.getGxsId(), forumMessageItem.getMsgId()).orElse(null);

		assertNotNull(found);
		assertEquals(forumMessageItem.getGxsId(), found.getGxsId());
		assertEquals(forumMessageItem.getMsgId(), found.getMsgId());

		assertTrue(gxsMessageItemRepository.findByGxsIdAndMsgId(IdFakes.createGxsId(), forumMessageItem.getMsgId()).isEmpty());
	}

	@Test
	void CountByGxsId_Success()
	{
		var gxsId = IdFakes.createGxsId();
		var forumMessageItem1 = ForumMessageItemFakes.createForumMessageItem();
		forumMessageItem1.setGxsId(gxsId);
		var forumMessageItem2 = ForumMessageItemFakes.createForumMessageItem();
		forumMessageItem2.setGxsId(gxsId);
		var forumMessageItem3 = ForumMessageItemFakes.createForumMessageItem();

		gxsMessageItemRepository.save(forumMessageItem1);
		gxsMessageItemRepository.save(forumMessageItem2);
		gxsMessageItemRepository.save(forumMessageItem3);

		assertEquals(2, gxsMessageItemRepository.countByGxsId(gxsId));
	}

	@Test
	void FixIntervalDuplicates_Success()
	{
		var gxsId = IdFakes.createGxsId();

		// msgA is replaced by msgB (msgB.originalMsgId == msgA.msgId)
		var msgA = new ForumMessageItem(gxsId, IdFakes.createMsgId(), "original");
		msgA.updatePublished();
		var msgB = new ForumMessageItem(gxsId, IdFakes.createMsgId(), "replacement");
		msgB.setOriginalMsgId(msgA.getMsgId());
		msgB.updatePublished();
		var msgC = new ForumMessageItem(gxsId, IdFakes.createMsgId(), "unrelated");
		msgC.updatePublished();

		gxsMessageItemRepository.save(msgA);
		gxsMessageItemRepository.save(msgB);
		gxsMessageItemRepository.save(msgC);

		gxsMessageItemRepository.fixIntervalDuplicates(gxsId, Instant.EPOCH);

		entityManager.flush();
		entityManager.clear();

		assertTrue(gxsMessageItemRepository.findByGxsIdAndMsgId(gxsId, msgA.getMsgId()).orElseThrow().isHidden());
		assertFalse(gxsMessageItemRepository.findByGxsIdAndMsgId(gxsId, msgB.getMsgId()).orElseThrow().isHidden());
		assertFalse(gxsMessageItemRepository.findByGxsIdAndMsgId(gxsId, msgC.getMsgId()).orElseThrow().isHidden());
	}

	@Test
	void FixIntervalDuplicates_WithSince_DoesNothing()
	{
		var gxsId = IdFakes.createGxsId();

		var msgA = new ForumMessageItem(gxsId, IdFakes.createMsgId(), "original");
		msgA.updatePublished();
		var msgB = new ForumMessageItem(gxsId, IdFakes.createMsgId(), "replacement");
		msgB.setOriginalMsgId(msgA.getMsgId());
		msgB.updatePublished();

		gxsMessageItemRepository.save(msgA);
		gxsMessageItemRepository.save(msgB);

		gxsMessageItemRepository.fixIntervalDuplicates(gxsId, Instant.now().plusSeconds(60));

		entityManager.flush();
		entityManager.clear();

		assertFalse(gxsMessageItemRepository.findByGxsIdAndMsgId(gxsId, msgA.getMsgId()).orElseThrow().isHidden());
	}

	@Test
	void HideOldDuplicates_Success()
	{
		var gxsId = IdFakes.createGxsId();
		var originalMsgId = IdFakes.createMsgId();

		// Two messages branched from the same original message, the oldest one must be hidden
		var oldDuplicate = new ForumMessageItem(gxsId, IdFakes.createMsgId(), "old");
		oldDuplicate.setOriginalMsgId(originalMsgId);
		oldDuplicate.updatePublished();

		// The duplicate must be created later than the original. We need to add a short delay.
		DebugUtils.wait(1);

		var newDuplicate = new ForumMessageItem(gxsId, IdFakes.createMsgId(), "new");
		newDuplicate.setOriginalMsgId(originalMsgId);
		newDuplicate.updatePublished();
		var standalone = new ForumMessageItem(gxsId, IdFakes.createMsgId(), "standalone");
		standalone.updatePublished();

		gxsMessageItemRepository.save(oldDuplicate);
		gxsMessageItemRepository.save(newDuplicate);
		gxsMessageItemRepository.save(standalone);

		gxsMessageItemRepository.hideOldDuplicates(gxsId, Instant.EPOCH);

		entityManager.flush();
		entityManager.clear();

		assertTrue(gxsMessageItemRepository.findByGxsIdAndMsgId(gxsId, oldDuplicate.getMsgId()).orElseThrow().isHidden());
		assertFalse(gxsMessageItemRepository.findByGxsIdAndMsgId(gxsId, newDuplicate.getMsgId()).orElseThrow().isHidden());
		assertFalse(gxsMessageItemRepository.findByGxsIdAndMsgId(gxsId, standalone.getMsgId()).orElseThrow().isHidden());
	}

	@Test
	void FindAllAuthors_Success()
	{
		var gxsId = IdFakes.createGxsId();
		var author1 = IdFakes.createGxsId();
		var author2 = IdFakes.createGxsId();

		var forumMessageItem1 = ForumMessageItemFakes.createForumMessageItem();
		forumMessageItem1.setGxsId(gxsId);
		forumMessageItem1.setAuthorGxsId(author1);
		var forumMessageItem2 = ForumMessageItemFakes.createForumMessageItem();
		forumMessageItem2.setGxsId(gxsId);
		forumMessageItem2.setAuthorGxsId(author2);
		var forumMessageItem3 = ForumMessageItemFakes.createForumMessageItem();
		forumMessageItem3.setGxsId(gxsId);
		forumMessageItem3.setAuthorGxsId(author1); // duplicate author

		gxsMessageItemRepository.save(forumMessageItem1);
		gxsMessageItemRepository.save(forumMessageItem2);
		gxsMessageItemRepository.save(forumMessageItem3);

		var authors = gxsMessageItemRepository.findAllAuthors(gxsId);

		assertEquals(2, authors.size());
		assertTrue(authors.contains(author1));
		assertTrue(authors.contains(author2));
	}
}
