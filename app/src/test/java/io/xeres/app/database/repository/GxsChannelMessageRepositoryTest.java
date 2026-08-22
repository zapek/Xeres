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

import io.xeres.app.database.model.gxs.ChannelMessageItemFakes;
import io.xeres.app.xrs.service.channel.item.ChannelMessageItem;
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
class GxsChannelMessageRepositoryTest
{
	@Autowired
	private GxsChannelMessageRepository gxsChannelMessageRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void CRUD_Success()
	{
		var channelMessageItem1 = ChannelMessageItemFakes.createChannelMessageItem();
		var channelMessageItem2 = ChannelMessageItemFakes.createChannelMessageItem();
		var channelMessageItem3 = ChannelMessageItemFakes.createChannelMessageItem();

		var savedChannelMessageItem1 = gxsChannelMessageRepository.save(channelMessageItem1);
		gxsChannelMessageRepository.save(channelMessageItem2);
		gxsChannelMessageRepository.save(channelMessageItem3);

		var channelMessageItems = gxsChannelMessageRepository.findAll();
		assertNotNull(channelMessageItems);
		assertEquals(3, channelMessageItems.size());

		var first = gxsChannelMessageRepository.findById(channelMessageItems.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedChannelMessageItem1.getId(), first.getId());
		assertEquals(savedChannelMessageItem1.getName(), first.getName());

		first.setRead(true);

		var updatedChannelMessageItem = gxsChannelMessageRepository.save(first);

		assertNotNull(updatedChannelMessageItem);
		assertEquals(first.getId(), updatedChannelMessageItem.getId());
		assertTrue(updatedChannelMessageItem.isRead());

		gxsChannelMessageRepository.deleteById(first.getId());

		var deleted = gxsChannelMessageRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindByGxsIdAndMsgId_Success()
	{
		var channelMessageItem = ChannelMessageItemFakes.createChannelMessageItem();
		gxsChannelMessageRepository.save(channelMessageItem);

		var found = gxsChannelMessageRepository.findByGxsIdAndMsgId(channelMessageItem.getGxsId(), channelMessageItem.getMsgId()).orElse(null);

		assertNotNull(found);
		assertEquals(channelMessageItem.getGxsId(), found.getGxsId());
		assertEquals(channelMessageItem.getMsgId(), found.getMsgId());
	}

	@Test
	void FindAllByGxsIdAndHiddenFalse_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var visible = createChannelMessageItem(gxsId, IdFakes.createMsgId(), false);
		var hidden = createChannelMessageItem(gxsId, IdFakes.createMsgId(), true);

		gxsChannelMessageRepository.save(visible);
		gxsChannelMessageRepository.save(hidden);

		var page = gxsChannelMessageRepository.findAllByGxsIdAndHiddenFalse(gxsId, PageRequest.of(0, 10));

		assertNotNull(page);
		assertEquals(1, page.getTotalElements());
		assertEquals(visible.getMsgId(), page.getContent().getFirst().getMsgId());
	}

	@Test
	void FindAllByGxsIdAndPublishedAfterAndHiddenFalse_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var visible = createChannelMessageItem(gxsId, IdFakes.createMsgId(), false);
		var hidden = createChannelMessageItem(gxsId, IdFakes.createMsgId(), true);

		gxsChannelMessageRepository.save(visible);
		gxsChannelMessageRepository.save(hidden);

		var found = gxsChannelMessageRepository.findAllByGxsIdAndPublishedAfterAndHiddenFalse(gxsId, Instant.EPOCH);

		assertNotNull(found);
		assertEquals(1, found.size());
		assertEquals(visible.getMsgId(), found.getFirst().getMsgId());

		found = gxsChannelMessageRepository.findAllByGxsIdAndPublishedAfterAndHiddenFalse(gxsId, Instant.now().plusSeconds(60));

		assertNotNull(found);
		assertTrue(found.isEmpty());
	}

	@Test
	void FindAllByGxsIdAndMsgIdIn_Success()
	{
		var gxsId = IdFakes.createGxsId();
		var otherGxsId = IdFakes.createGxsId();

		var channelMessageItem1 = createChannelMessageItem(gxsId, IdFakes.createMsgId(), false);
		var channelMessageItem2 = createChannelMessageItem(gxsId, IdFakes.createMsgId(), true);
		var channelMessageItem3 = createChannelMessageItem(otherGxsId, IdFakes.createMsgId(), false);

		gxsChannelMessageRepository.save(channelMessageItem1);
		gxsChannelMessageRepository.save(channelMessageItem2);
		gxsChannelMessageRepository.save(channelMessageItem3);

		var msgIds = Set.of(channelMessageItem1.getMsgId(), channelMessageItem2.getMsgId(), channelMessageItem3.getMsgId());

		var all = gxsChannelMessageRepository.findAllByGxsIdAndMsgIdIn(gxsId, msgIds);
		assertNotNull(all);
		assertEquals(2, all.size());

		var visibleOnly = gxsChannelMessageRepository.findAllByGxsIdAndMsgIdInAndHiddenFalse(gxsId, msgIds);
		assertNotNull(visibleOnly);
		assertEquals(1, visibleOnly.size());
		assertEquals(channelMessageItem1.getMsgId(), visibleOnly.getFirst().getMsgId());
	}

	@Test
	void FindAllByMsgIdInAndHiddenFalse_Success()
	{
		var channelMessageItem1 = ChannelMessageItemFakes.createChannelMessageItem();
		var channelMessageItem2 = ChannelMessageItemFakes.createChannelMessageItem();
		channelMessageItem2.setHidden(true);
		var channelMessageItem3 = ChannelMessageItemFakes.createChannelMessageItem();

		gxsChannelMessageRepository.save(channelMessageItem1);
		gxsChannelMessageRepository.save(channelMessageItem2);
		gxsChannelMessageRepository.save(channelMessageItem3);

		var found = gxsChannelMessageRepository.findAllByMsgIdInAndHiddenFalse(Set.of(channelMessageItem1.getMsgId(), channelMessageItem2.getMsgId(), channelMessageItem3.getMsgId()));

		assertNotNull(found);
		assertEquals(2, found.size());
	}

	@Test
	void CountUnreadMessages_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var unreadVisible1 = createChannelMessageItem(gxsId, IdFakes.createMsgId(), false);
		var unreadVisible2 = createChannelMessageItem(gxsId, IdFakes.createMsgId(), false);
		var readVisible = createChannelMessageItem(gxsId, IdFakes.createMsgId(), false);
		readVisible.setRead(true);
		var unreadHidden = createChannelMessageItem(gxsId, IdFakes.createMsgId(), true);

		gxsChannelMessageRepository.save(unreadVisible1);
		gxsChannelMessageRepository.save(unreadVisible2);
		gxsChannelMessageRepository.save(readVisible);
		gxsChannelMessageRepository.save(unreadHidden);

		assertEquals(2, gxsChannelMessageRepository.countUnreadMessages(gxsId));
	}

	@Test
	void SetAllGroupMessagesReadState_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var unread1 = createChannelMessageItem(gxsId, IdFakes.createMsgId(), false);
		var unread2 = createChannelMessageItem(gxsId, IdFakes.createMsgId(), true);
		var read = createChannelMessageItem(gxsId, IdFakes.createMsgId(), false);
		read.setRead(true);

		gxsChannelMessageRepository.save(unread1);
		gxsChannelMessageRepository.save(unread2);
		gxsChannelMessageRepository.save(read);

		gxsChannelMessageRepository.setAllGroupMessagesReadState(gxsId, true);

		entityManager.flush();
		entityManager.clear();

		assertTrue(gxsChannelMessageRepository.findByGxsIdAndMsgId(gxsId, unread1.getMsgId()).orElseThrow().isRead());
		assertTrue(gxsChannelMessageRepository.findByGxsIdAndMsgId(gxsId, unread2.getMsgId()).orElseThrow().isRead());
		assertTrue(gxsChannelMessageRepository.findByGxsIdAndMsgId(gxsId, read.getMsgId()).orElseThrow().isRead());
	}

	private static ChannelMessageItem createChannelMessageItem(GxsId gxsId, MsgId msgId, boolean hidden)
	{
		var channelMessageItem = new ChannelMessageItem(gxsId, msgId, "message");
		channelMessageItem.setContent("content");
		channelMessageItem.setHidden(hidden);
		return channelMessageItem;
	}
}
