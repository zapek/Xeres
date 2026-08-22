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

import io.xeres.app.xrs.common.VoteMessageItem;
import io.xeres.common.id.GxsId;
import io.xeres.testutils.IdFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GxsVoteMessageRepositoryTest
{
	@Autowired
	private GxsVoteMessageRepository gxsVoteMessageRepository;

	@Test
	void CRUD_Success()
	{
		var gxsId = IdFakes.createGxsId();
		var voteMessageItem1 = createVoteMessageItem(gxsId);
		var voteMessageItem2 = createVoteMessageItem(gxsId);
		var voteMessageItem3 = createVoteMessageItem(gxsId);

		voteMessageItem1.setType(VoteMessageItem.Type.UP);
		voteMessageItem2.setType(VoteMessageItem.Type.DOWN);

		var savedVoteMessageItem1 = gxsVoteMessageRepository.save(voteMessageItem1);
		gxsVoteMessageRepository.save(voteMessageItem2);
		gxsVoteMessageRepository.save(voteMessageItem3);

		var voteMessageItems = gxsVoteMessageRepository.findAll();
		assertNotNull(voteMessageItems);
		assertEquals(3, voteMessageItems.size());

		var first = gxsVoteMessageRepository.findById(voteMessageItems.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedVoteMessageItem1.getId(), first.getId());
		assertEquals(VoteMessageItem.Type.UP, first.getType());

		first.setType(VoteMessageItem.Type.NONE);

		var updatedVoteMessageItem = gxsVoteMessageRepository.save(first);

		assertNotNull(updatedVoteMessageItem);
		assertEquals(first.getId(), updatedVoteMessageItem.getId());
		assertEquals(VoteMessageItem.Type.NONE, updatedVoteMessageItem.getType());

		gxsVoteMessageRepository.deleteById(first.getId());

		var deleted = gxsVoteMessageRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindAllByGxsIdAndMsgIdIn_Success()
	{
		var gxsId1 = IdFakes.createGxsId();
		var gxsId2 = IdFakes.createGxsId();

		var voteMessageItem1 = createVoteMessageItem(gxsId1);
		var voteMessageItem2 = createVoteMessageItem(gxsId1);
		var voteMessageItem3 = createVoteMessageItem(gxsId2);

		gxsVoteMessageRepository.save(voteMessageItem1);
		gxsVoteMessageRepository.save(voteMessageItem2);
		gxsVoteMessageRepository.save(voteMessageItem3);

		var found = gxsVoteMessageRepository.findAllByGxsIdAndMsgIdIn(gxsId1, Set.of(voteMessageItem1.getMsgId(), voteMessageItem2.getMsgId(), voteMessageItem3.getMsgId()));

		assertNotNull(found);
		assertEquals(2, found.size());
		assertTrue(found.contains(voteMessageItem1));
		assertTrue(found.contains(voteMessageItem2));
	}

	private static VoteMessageItem createVoteMessageItem(GxsId gxsId)
	{
		var voteMessageItem = new VoteMessageItem(gxsId, "vote");
		voteMessageItem.setMsgId(IdFakes.createMsgId());
		return voteMessageItem;
	}
}
