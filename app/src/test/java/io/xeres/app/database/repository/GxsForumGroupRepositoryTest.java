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
import io.xeres.testutils.IdFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GxsForumGroupRepositoryTest
{
	@Autowired
	private GxsForumGroupRepository gxsForumGroupRepository;

	@Test
	void CRUD_Success()
	{
		var forumGroupItem1 = ForumGroupItemFakes.createForumGroupItem();
		var forumGroupItem2 = ForumGroupItemFakes.createForumGroupItem();
		var forumGroupItem3 = ForumGroupItemFakes.createForumGroupItem();

		var savedForumGroupItem1 = gxsForumGroupRepository.save(forumGroupItem1);
		gxsForumGroupRepository.save(forumGroupItem2);
		gxsForumGroupRepository.save(forumGroupItem3);

		var forumGroupItems = gxsForumGroupRepository.findAll();
		assertNotNull(forumGroupItems);
		assertEquals(3, forumGroupItems.size());

		var first = gxsForumGroupRepository.findById(forumGroupItems.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedForumGroupItem1.getId(), first.getId());
		assertEquals(savedForumGroupItem1.getName(), first.getName());

		first.setDescription("updated");

		var updatedForumGroupItem = gxsForumGroupRepository.save(first);

		assertNotNull(updatedForumGroupItem);
		assertEquals(first.getId(), updatedForumGroupItem.getId());
		assertEquals("updated", updatedForumGroupItem.getDescription());

		gxsForumGroupRepository.deleteById(first.getId());

		var deleted = gxsForumGroupRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindByGxsId_Success()
	{
		var forumGroupItem = ForumGroupItemFakes.createForumGroupItem();
		gxsForumGroupRepository.save(forumGroupItem);

		var found = gxsForumGroupRepository.findByGxsId(forumGroupItem.getGxsId()).orElse(null);

		assertNotNull(found);
		assertEquals(forumGroupItem.getGxsId(), found.getGxsId());
		assertEquals(forumGroupItem.getName(), found.getName());

		assertTrue(gxsForumGroupRepository.findByGxsId(IdFakes.createGxsId()).isEmpty());
	}

	@Test
	void FindAllByGxsIdIn_Success()
	{
		var forumGroupItem1 = ForumGroupItemFakes.createForumGroupItem();
		var forumGroupItem2 = ForumGroupItemFakes.createForumGroupItem();
		var forumGroupItem3 = ForumGroupItemFakes.createForumGroupItem();

		gxsForumGroupRepository.save(forumGroupItem1);
		gxsForumGroupRepository.save(forumGroupItem2);
		gxsForumGroupRepository.save(forumGroupItem3);

		var found = gxsForumGroupRepository.findAllByGxsIdIn(Set.of(forumGroupItem1.getGxsId(), forumGroupItem3.getGxsId()));

		assertNotNull(found);
		assertEquals(2, found.size());
		assertTrue(found.contains(forumGroupItem1));
		assertTrue(found.contains(forumGroupItem3));
	}

	@Test
	void FindAllBySubscribedIsTrue_Success()
	{
		var subscribedForumGroupItem1 = ForumGroupItemFakes.createForumGroupItem();
		subscribedForumGroupItem1.setSubscribed(true);
		var subscribedForumGroupItem2 = ForumGroupItemFakes.createForumGroupItem();
		subscribedForumGroupItem2.setSubscribed(true);
		var unsubscribedForumGroupItem = ForumGroupItemFakes.createForumGroupItem();

		gxsForumGroupRepository.save(subscribedForumGroupItem1);
		gxsForumGroupRepository.save(subscribedForumGroupItem2);
		gxsForumGroupRepository.save(unsubscribedForumGroupItem);

		var found = gxsForumGroupRepository.findAllBySubscribedIsTrue();

		assertNotNull(found);
		assertEquals(2, found.size());
	}
}
