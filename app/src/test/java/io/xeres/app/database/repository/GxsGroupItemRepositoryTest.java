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

import io.xeres.app.database.model.gxs.ForumGroupItemFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Limit;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GxsGroupItemRepositoryTest
{
	@Autowired
	private GxsGroupItemRepository gxsGroupItemRepository;

	@Test
	void CRUD_Success()
	{
		var forumGroupItem1 = ForumGroupItemFakes.createForumGroupItem();
		var forumGroupItem2 = ForumGroupItemFakes.createForumGroupItem();
		var forumGroupItem3 = ForumGroupItemFakes.createForumGroupItem();

		var savedForumGroupItem1 = gxsGroupItemRepository.save(forumGroupItem1);
		gxsGroupItemRepository.save(forumGroupItem2);
		gxsGroupItemRepository.save(forumGroupItem3);

		var forumGroupItems = gxsGroupItemRepository.findAll();
		assertNotNull(forumGroupItems);
		assertEquals(3, forumGroupItems.size());

		var first = gxsGroupItemRepository.findById(forumGroupItems.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedForumGroupItem1.getId(), first.getId());
		assertEquals(savedForumGroupItem1.getName(), first.getName());

		first.setSubscribed(true);

		var updatedForumGroupItem = gxsGroupItemRepository.save(first);

		assertNotNull(updatedForumGroupItem);
		assertEquals(first.getId(), updatedForumGroupItem.getId());
		assertTrue(updatedForumGroupItem.isSubscribed());

		gxsGroupItemRepository.deleteById(first.getId());

		var deleted = gxsGroupItemRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindByGxsId_Success()
	{
		var forumGroupItem = ForumGroupItemFakes.createForumGroupItem();
		gxsGroupItemRepository.save(forumGroupItem);

		var found = gxsGroupItemRepository.findByGxsId(forumGroupItem.getGxsId()).orElse(null);

		assertNotNull(found);
		assertEquals(forumGroupItem.getGxsId(), found.getGxsId());
		assertEquals(forumGroupItem.getName(), found.getName());
	}

	@Test
	void FindByGxsIdAndSubscribedIsTrue_Success()
	{
		var subscribedGroup = ForumGroupItemFakes.createForumGroupItem();
		subscribedGroup.setSubscribed(true);
		var unsubscribedGroup = ForumGroupItemFakes.createForumGroupItem();
		unsubscribedGroup.setSubscribed(false);

		gxsGroupItemRepository.save(subscribedGroup);
		gxsGroupItemRepository.save(unsubscribedGroup);

		var found = gxsGroupItemRepository.findByGxsIdAndSubscribedIsTrue(subscribedGroup.getGxsId()).orElse(null);
		assertNotNull(found);
		assertEquals(subscribedGroup.getGxsId(), found.getGxsId());

		assertTrue(gxsGroupItemRepository.findByGxsIdAndSubscribedIsTrue(unsubscribedGroup.getGxsId()).isEmpty());
	}

	@Test
	void FindByOrderByLastStatistics_Success()
	{
		var group1 = ForumGroupItemFakes.createForumGroupItem();
		group1.setLastStatistics(Instant.EPOCH.plusSeconds(300));
		var group2 = ForumGroupItemFakes.createForumGroupItem();
		group2.setLastStatistics(Instant.EPOCH.plusSeconds(200));
		var group3 = ForumGroupItemFakes.createForumGroupItem();
		group3.setLastStatistics(Instant.EPOCH.plusSeconds(100));

		gxsGroupItemRepository.save(group1);
		gxsGroupItemRepository.save(group2);
		gxsGroupItemRepository.save(group3);

		var groups = gxsGroupItemRepository.findByOrderByLastStatistics(Limit.of(2));

		assertNotNull(groups);
		assertEquals(2, groups.size());
		assertEquals(group3.getGxsId(), groups.getFirst().getGxsId());
		assertEquals(group2.getGxsId(), groups.getLast().getGxsId());
	}
}
