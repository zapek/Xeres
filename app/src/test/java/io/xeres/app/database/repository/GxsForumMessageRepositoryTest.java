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
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;
import io.xeres.testutils.IdFakes;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GxsForumMessageRepositoryTest
{
	@Autowired
	private GxsForumMessageRepository gxsForumMessageRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void CRUD_Success()
	{
		var forumMessageItem1 = ForumMessageItemFakes.createForumMessageItem();
		var forumMessageItem2 = ForumMessageItemFakes.createForumMessageItem();
		var forumMessageItem3 = ForumMessageItemFakes.createForumMessageItem();

		forumMessageItem1.setContent("first content");

		var savedForumMessageItem1 = gxsForumMessageRepository.save(forumMessageItem1);
		gxsForumMessageRepository.save(forumMessageItem2);
		gxsForumMessageRepository.save(forumMessageItem3);

		var forumMessageItems = gxsForumMessageRepository.findAll();
		assertNotNull(forumMessageItems);
		assertEquals(3, forumMessageItems.size());

		var first = gxsForumMessageRepository.findById(forumMessageItems.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedForumMessageItem1.getId(), first.getId());
		assertEquals("first content", first.getContent());

		first.setRead(true);

		var updatedForumMessageItem = gxsForumMessageRepository.save(first);

		assertNotNull(updatedForumMessageItem);
		assertEquals(first.getId(), updatedForumMessageItem.getId());
		assertTrue(updatedForumMessageItem.isRead());

		gxsForumMessageRepository.deleteById(first.getId());

		var deleted = gxsForumMessageRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindByGxsIdAndMsgId_Success()
	{
		var forumMessageItem = ForumMessageItemFakes.createForumMessageItem();
		gxsForumMessageRepository.save(forumMessageItem);

		var found = gxsForumMessageRepository.findByGxsIdAndMsgId(forumMessageItem.getGxsId(), forumMessageItem.getMsgId()).orElse(null);

		assertNotNull(found);
		assertEquals(forumMessageItem.getGxsId(), found.getGxsId());
		assertEquals(forumMessageItem.getMsgId(), found.getMsgId());
	}

	@Test
	void FindAllByGxsIdAndPublishedAfterAndHiddenFalse_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var visible = createForumMessageItem(gxsId, IdFakes.createMsgId(), false);
		var hidden = createForumMessageItem(gxsId, IdFakes.createMsgId(), true);

		gxsForumMessageRepository.save(visible);
		gxsForumMessageRepository.save(hidden);

		var found = gxsForumMessageRepository.findAllByGxsIdAndPublishedAfterAndHiddenFalse(gxsId, Instant.EPOCH);

		assertNotNull(found);
		assertEquals(1, found.size());
		assertEquals(visible.getMsgId(), found.getFirst().getMsgId());

		found = gxsForumMessageRepository.findAllByGxsIdAndPublishedAfterAndHiddenFalse(gxsId, Instant.now().plusSeconds(60));

		assertNotNull(found);
		assertTrue(found.isEmpty());
	}

	@Test
	void FindAllByGxsIdAndMsgIdIn_Success()
	{
		var gxsId = IdFakes.createGxsId();
		var otherGxsId = IdFakes.createGxsId();

		var forumMessageItem1 = createForumMessageItem(gxsId, IdFakes.createMsgId(), false);
		var forumMessageItem2 = createForumMessageItem(gxsId, IdFakes.createMsgId(), true);
		var forumMessageItem3 = createForumMessageItem(otherGxsId, IdFakes.createMsgId(), false);

		gxsForumMessageRepository.save(forumMessageItem1);
		gxsForumMessageRepository.save(forumMessageItem2);
		gxsForumMessageRepository.save(forumMessageItem3);

		var msgIds = Set.of(forumMessageItem1.getMsgId(), forumMessageItem2.getMsgId(), forumMessageItem3.getMsgId());

		var all = gxsForumMessageRepository.findAllByGxsIdAndMsgIdIn(gxsId, msgIds);
		assertNotNull(all);
		assertEquals(2, all.size());

		var visibleOnly = gxsForumMessageRepository.findAllByGxsIdAndMsgIdInAndHiddenFalse(gxsId, msgIds);
		assertNotNull(visibleOnly);
		assertEquals(1, visibleOnly.size());
		assertEquals(forumMessageItem1.getMsgId(), visibleOnly.getFirst().getMsgId());
	}

	@Test
	void FindAllByMsgIdIn_Success()
	{
		var forumMessageItem1 = ForumMessageItemFakes.createForumMessageItem();
		var forumMessageItem2 = ForumMessageItemFakes.createForumMessageItem();
		forumMessageItem2.setHidden(true);
		var forumMessageItem3 = ForumMessageItemFakes.createForumMessageItem();

		gxsForumMessageRepository.save(forumMessageItem1);
		gxsForumMessageRepository.save(forumMessageItem2);
		gxsForumMessageRepository.save(forumMessageItem3);

		var msgIds = Set.of(forumMessageItem1.getMsgId(), forumMessageItem2.getMsgId(), forumMessageItem3.getMsgId());

		var visible = gxsForumMessageRepository.findAllByMsgIdInAndHiddenFalse(msgIds);
		assertNotNull(visible);
		assertEquals(2, visible.size());

		var hidden = gxsForumMessageRepository.findAllByMsgIdInAndHiddenTrue(msgIds);
		assertNotNull(hidden);
		assertEquals(1, hidden.size());
		assertEquals(forumMessageItem2.getMsgId(), hidden.getFirst().getMsgId());
	}

	@Test
	void FindSummaryAllByGxsIdAndHiddenFalse_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var visible1 = createForumMessageItem(gxsId, IdFakes.createMsgId(), false);
		var visible2 = createForumMessageItem(gxsId, IdFakes.createMsgId(), false);
		var hidden = createForumMessageItem(gxsId, IdFakes.createMsgId(), true);

		gxsForumMessageRepository.save(visible1);
		gxsForumMessageRepository.save(visible2);
		gxsForumMessageRepository.save(hidden);

		var page = gxsForumMessageRepository.findSummaryAllByGxsIdAndHiddenFalse(gxsId, PageRequest.of(0, 10));

		assertNotNull(page);
		assertEquals(2, page.getTotalElements());
		assertEquals(1, page.getTotalPages());
		assertTrue(page.getContent().stream().noneMatch(summary -> summary.getMsgId().equals(hidden.getMsgId())));

		page = gxsForumMessageRepository.findSummaryAllByGxsIdAndHiddenFalse(gxsId, PageRequest.of(0, 1));

		assertEquals(2, page.getTotalElements());
		assertEquals(2, page.getTotalPages());
		assertEquals(1, page.getContent().size());
	}

	@Test
	void CountUnreadMessages_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var unreadVisible1 = createForumMessageItem(gxsId, IdFakes.createMsgId(), false);
		var unreadVisible2 = createForumMessageItem(gxsId, IdFakes.createMsgId(), false);
		var readVisible = createForumMessageItem(gxsId, IdFakes.createMsgId(), false);
		readVisible.setRead(true);
		var unreadHidden = createForumMessageItem(gxsId, IdFakes.createMsgId(), true);

		gxsForumMessageRepository.save(unreadVisible1);
		gxsForumMessageRepository.save(unreadVisible2);
		gxsForumMessageRepository.save(readVisible);
		gxsForumMessageRepository.save(unreadHidden);

		assertEquals(2, gxsForumMessageRepository.countUnreadMessages(gxsId));
	}

	@Test
	void SetAllGroupMessagesReadState_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var unread1 = createForumMessageItem(gxsId, IdFakes.createMsgId(), false);
		var unread2 = createForumMessageItem(gxsId, IdFakes.createMsgId(), true);
		var read = createForumMessageItem(gxsId, IdFakes.createMsgId(), false);
		read.setRead(true);

		gxsForumMessageRepository.save(unread1);
		gxsForumMessageRepository.save(unread2);
		gxsForumMessageRepository.save(read);

		gxsForumMessageRepository.setAllGroupMessagesReadState(gxsId, true);

		entityManager.flush();
		entityManager.clear();

		assertTrue(gxsForumMessageRepository.findByGxsIdAndMsgId(gxsId, unread1.getMsgId()).orElseThrow().isRead());
		assertTrue(gxsForumMessageRepository.findByGxsIdAndMsgId(gxsId, unread2.getMsgId()).orElseThrow().isRead());
		assertTrue(gxsForumMessageRepository.findByGxsIdAndMsgId(gxsId, read.getMsgId()).orElseThrow().isRead());
	}

	private static ForumMessageItem createForumMessageItem(GxsId gxsId, MsgId msgId, boolean hidden)
	{
		var forumMessageItem = new ForumMessageItem(gxsId, msgId, "message");
		forumMessageItem.setHidden(hidden);
		return forumMessageItem;
	}
}
