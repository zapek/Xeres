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

import io.xeres.app.database.model.gxs.BoardMessageItemFakes;
import io.xeres.app.xrs.service.board.item.BoardMessageItem;
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
class GxsBoardMessageRepositoryTest
{
	@Autowired
	private GxsBoardMessageRepository gxsBoardMessageRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void CRUD_Success()
	{
		var boardMessageItem1 = BoardMessageItemFakes.createBoardMessageItem();
		var boardMessageItem2 = BoardMessageItemFakes.createBoardMessageItem();
		var boardMessageItem3 = BoardMessageItemFakes.createBoardMessageItem();

		var savedBoardMessageItem1 = gxsBoardMessageRepository.save(boardMessageItem1);
		gxsBoardMessageRepository.save(boardMessageItem2);
		gxsBoardMessageRepository.save(boardMessageItem3);

		var boardMessageItems = gxsBoardMessageRepository.findAll();
		assertNotNull(boardMessageItems);
		assertEquals(3, boardMessageItems.size());

		var first = gxsBoardMessageRepository.findById(boardMessageItems.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedBoardMessageItem1.getId(), first.getId());
		assertEquals(savedBoardMessageItem1.getName(), first.getName());

		first.setRead(true);

		var updatedBoardMessageItem = gxsBoardMessageRepository.save(first);

		assertNotNull(updatedBoardMessageItem);
		assertEquals(first.getId(), updatedBoardMessageItem.getId());
		assertTrue(updatedBoardMessageItem.isRead());

		gxsBoardMessageRepository.deleteById(first.getId());

		var deleted = gxsBoardMessageRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindByGxsIdAndMsgId_Success()
	{
		var boardMessageItem = BoardMessageItemFakes.createBoardMessageItem();
		gxsBoardMessageRepository.save(boardMessageItem);

		var found = gxsBoardMessageRepository.findByGxsIdAndMsgId(boardMessageItem.getGxsId(), boardMessageItem.getMsgId()).orElse(null);

		assertNotNull(found);
		assertEquals(boardMessageItem.getGxsId(), found.getGxsId());
		assertEquals(boardMessageItem.getMsgId(), found.getMsgId());
	}

	@Test
	void FindAllByGxsIdAndHiddenFalse_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var visible = createBoardMessageItem(gxsId, IdFakes.createMsgId(), false);
		var hidden = createBoardMessageItem(gxsId, IdFakes.createMsgId(), true);

		gxsBoardMessageRepository.save(visible);
		gxsBoardMessageRepository.save(hidden);

		var page = gxsBoardMessageRepository.findAllByGxsIdAndHiddenFalse(gxsId, PageRequest.of(0, 10));

		assertNotNull(page);
		assertEquals(1, page.getTotalElements());
		assertEquals(visible.getMsgId(), page.getContent().getFirst().getMsgId());
	}

	@Test
	void FindAllByGxsIdAndPublishedAfterAndHiddenFalse_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var visible = createBoardMessageItem(gxsId, IdFakes.createMsgId(), false);
		var hidden = createBoardMessageItem(gxsId, IdFakes.createMsgId(), true);

		gxsBoardMessageRepository.save(visible);
		gxsBoardMessageRepository.save(hidden);

		var found = gxsBoardMessageRepository.findAllByGxsIdAndPublishedAfterAndHiddenFalse(gxsId, Instant.EPOCH);

		assertNotNull(found);
		assertEquals(1, found.size());
		assertEquals(visible.getMsgId(), found.getFirst().getMsgId());

		found = gxsBoardMessageRepository.findAllByGxsIdAndPublishedAfterAndHiddenFalse(gxsId, Instant.now().plusSeconds(60));

		assertNotNull(found);
		assertTrue(found.isEmpty());
	}

	@Test
	void FindAllByGxsIdAndMsgIdIn_Success()
	{
		var gxsId = IdFakes.createGxsId();
		var otherGxsId = IdFakes.createGxsId();

		var boardMessageItem1 = createBoardMessageItem(gxsId, IdFakes.createMsgId(), false);
		var boardMessageItem2 = createBoardMessageItem(gxsId, IdFakes.createMsgId(), true);
		var boardMessageItem3 = createBoardMessageItem(otherGxsId, IdFakes.createMsgId(), false);

		gxsBoardMessageRepository.save(boardMessageItem1);
		gxsBoardMessageRepository.save(boardMessageItem2);
		gxsBoardMessageRepository.save(boardMessageItem3);

		var msgIds = Set.of(boardMessageItem1.getMsgId(), boardMessageItem2.getMsgId(), boardMessageItem3.getMsgId());

		var all = gxsBoardMessageRepository.findAllByGxsIdAndMsgIdIn(gxsId, msgIds);
		assertNotNull(all);
		assertEquals(2, all.size());

		var visibleOnly = gxsBoardMessageRepository.findAllByGxsIdAndMsgIdInAndHiddenFalse(gxsId, msgIds);
		assertNotNull(visibleOnly);
		assertEquals(1, visibleOnly.size());
		assertEquals(boardMessageItem1.getMsgId(), visibleOnly.getFirst().getMsgId());
	}

	@Test
	void FindAllByMsgIdInAndHiddenFalse_Success()
	{
		var boardMessageItem1 = BoardMessageItemFakes.createBoardMessageItem();
		var boardMessageItem2 = BoardMessageItemFakes.createBoardMessageItem();
		boardMessageItem2.setHidden(true);
		var boardMessageItem3 = BoardMessageItemFakes.createBoardMessageItem();

		gxsBoardMessageRepository.save(boardMessageItem1);
		gxsBoardMessageRepository.save(boardMessageItem2);
		gxsBoardMessageRepository.save(boardMessageItem3);

		var found = gxsBoardMessageRepository.findAllByMsgIdInAndHiddenFalse(Set.of(boardMessageItem1.getMsgId(), boardMessageItem2.getMsgId(), boardMessageItem3.getMsgId()));

		assertNotNull(found);
		assertEquals(2, found.size());
	}

	@Test
	void CountUnreadMessages_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var unreadVisible1 = createBoardMessageItem(gxsId, IdFakes.createMsgId(), false);
		var unreadVisible2 = createBoardMessageItem(gxsId, IdFakes.createMsgId(), false);
		var readVisible = createBoardMessageItem(gxsId, IdFakes.createMsgId(), false);
		readVisible.setRead(true);
		var unreadHidden = createBoardMessageItem(gxsId, IdFakes.createMsgId(), true);

		gxsBoardMessageRepository.save(unreadVisible1);
		gxsBoardMessageRepository.save(unreadVisible2);
		gxsBoardMessageRepository.save(readVisible);
		gxsBoardMessageRepository.save(unreadHidden);

		assertEquals(2, gxsBoardMessageRepository.countUnreadMessages(gxsId));
	}

	@Test
	void SetAllGroupMessagesReadState_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var unread1 = createBoardMessageItem(gxsId, IdFakes.createMsgId(), false);
		var unread2 = createBoardMessageItem(gxsId, IdFakes.createMsgId(), true);
		var read = createBoardMessageItem(gxsId, IdFakes.createMsgId(), false);
		read.setRead(true);

		gxsBoardMessageRepository.save(unread1);
		gxsBoardMessageRepository.save(unread2);
		gxsBoardMessageRepository.save(read);

		gxsBoardMessageRepository.setAllGroupMessagesReadState(gxsId, true);

		entityManager.flush();
		entityManager.clear();

		assertTrue(gxsBoardMessageRepository.findByGxsIdAndMsgId(gxsId, unread1.getMsgId()).orElseThrow().isRead());
		assertTrue(gxsBoardMessageRepository.findByGxsIdAndMsgId(gxsId, unread2.getMsgId()).orElseThrow().isRead());
		assertTrue(gxsBoardMessageRepository.findByGxsIdAndMsgId(gxsId, read.getMsgId()).orElseThrow().isRead());
	}

	private static BoardMessageItem createBoardMessageItem(GxsId gxsId, MsgId msgId, boolean hidden)
	{
		var boardMessageItem = new BoardMessageItem(gxsId, msgId, "message");
		boardMessageItem.setHidden(hidden);
		return boardMessageItem;
	}
}
