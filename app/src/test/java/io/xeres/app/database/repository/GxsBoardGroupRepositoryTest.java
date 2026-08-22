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

import io.xeres.app.database.model.gxs.BoardGroupItemFakes;
import io.xeres.testutils.IdFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GxsBoardGroupRepositoryTest
{
	@Autowired
	private GxsBoardGroupRepository gxsBoardGroupRepository;

	@Test
	void CRUD_Success()
	{
		var boardGroupItem1 = BoardGroupItemFakes.createBoardGroupItem();
		var boardGroupItem2 = BoardGroupItemFakes.createBoardGroupItem();
		var boardGroupItem3 = BoardGroupItemFakes.createBoardGroupItem();

		var savedBoardGroupItem1 = gxsBoardGroupRepository.save(boardGroupItem1);
		gxsBoardGroupRepository.save(boardGroupItem2);
		gxsBoardGroupRepository.save(boardGroupItem3);

		var boardGroupItems = gxsBoardGroupRepository.findAll();
		assertNotNull(boardGroupItems);
		assertEquals(3, boardGroupItems.size());

		var first = gxsBoardGroupRepository.findById(boardGroupItems.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedBoardGroupItem1.getId(), first.getId());
		assertEquals(savedBoardGroupItem1.getName(), first.getName());

		first.setDescription("updated");

		var updatedBoardGroupItem = gxsBoardGroupRepository.save(first);

		assertNotNull(updatedBoardGroupItem);
		assertEquals(first.getId(), updatedBoardGroupItem.getId());
		assertEquals("updated", updatedBoardGroupItem.getDescription());

		gxsBoardGroupRepository.deleteById(first.getId());

		var deleted = gxsBoardGroupRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindByGxsId_Success()
	{
		var boardGroupItem = BoardGroupItemFakes.createBoardGroupItem();
		gxsBoardGroupRepository.save(boardGroupItem);

		var found = gxsBoardGroupRepository.findByGxsId(boardGroupItem.getGxsId()).orElse(null);

		assertNotNull(found);
		assertEquals(boardGroupItem.getGxsId(), found.getGxsId());
		assertEquals(boardGroupItem.getName(), found.getName());

		assertTrue(gxsBoardGroupRepository.findByGxsId(IdFakes.createGxsId()).isEmpty());
	}

	@Test
	void FindAllByGxsIdIn_Success()
	{
		var boardGroupItem1 = BoardGroupItemFakes.createBoardGroupItem();
		var boardGroupItem2 = BoardGroupItemFakes.createBoardGroupItem();
		var boardGroupItem3 = BoardGroupItemFakes.createBoardGroupItem();

		gxsBoardGroupRepository.save(boardGroupItem1);
		gxsBoardGroupRepository.save(boardGroupItem2);
		gxsBoardGroupRepository.save(boardGroupItem3);

		var found = gxsBoardGroupRepository.findAllByGxsIdIn(Set.of(boardGroupItem1.getGxsId(), boardGroupItem3.getGxsId()));

		assertNotNull(found);
		assertEquals(2, found.size());
		assertTrue(found.contains(boardGroupItem1));
		assertTrue(found.contains(boardGroupItem3));
	}

	@Test
	void FindAllBySubscribedIsTrue_Success()
	{
		var subscribedBoardGroupItem1 = BoardGroupItemFakes.createBoardGroupItem();
		subscribedBoardGroupItem1.setSubscribed(true);
		var subscribedBoardGroupItem2 = BoardGroupItemFakes.createBoardGroupItem();
		subscribedBoardGroupItem2.setSubscribed(true);
		var unsubscribedBoardGroupItem = BoardGroupItemFakes.createBoardGroupItem();

		gxsBoardGroupRepository.save(subscribedBoardGroupItem1);
		gxsBoardGroupRepository.save(subscribedBoardGroupItem2);
		gxsBoardGroupRepository.save(unsubscribedBoardGroupItem);

		var found = gxsBoardGroupRepository.findAllBySubscribedIsTrue();

		assertNotNull(found);
		assertEquals(2, found.size());
	}
}
