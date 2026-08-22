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

import io.xeres.app.xrs.common.CommentMessageItem;
import io.xeres.common.id.GxsId;
import io.xeres.testutils.IdFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GxsCommentMessageRepositoryTest
{
	@Autowired
	private GxsCommentMessageRepository gxsCommentMessageRepository;

	@Test
	void CRUD_Success()
	{
		var gxsId = IdFakes.createGxsId();
		var commentMessageItem1 = createCommentMessageItem(gxsId);
		var commentMessageItem2 = createCommentMessageItem(gxsId);
		var commentMessageItem3 = createCommentMessageItem(gxsId);

		commentMessageItem1.setComment("first comment");

		var savedCommentMessageItem1 = gxsCommentMessageRepository.save(commentMessageItem1);
		gxsCommentMessageRepository.save(commentMessageItem2);
		gxsCommentMessageRepository.save(commentMessageItem3);

		var commentMessageItems = gxsCommentMessageRepository.findAll();
		assertNotNull(commentMessageItems);
		assertEquals(3, commentMessageItems.size());

		var first = gxsCommentMessageRepository.findById(commentMessageItems.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedCommentMessageItem1.getId(), first.getId());
		assertEquals("first comment", first.getComment());

		first.setComment("updated comment");

		var updatedCommentMessageItem = gxsCommentMessageRepository.save(first);

		assertNotNull(updatedCommentMessageItem);
		assertEquals(first.getId(), updatedCommentMessageItem.getId());
		assertEquals("updated comment", updatedCommentMessageItem.getComment());

		gxsCommentMessageRepository.deleteById(first.getId());

		var deleted = gxsCommentMessageRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindAllByGxsIdAndMsgIdIn_Success()
	{
		var gxsId1 = IdFakes.createGxsId();
		var gxsId2 = IdFakes.createGxsId();

		var commentMessageItem1 = createCommentMessageItem(gxsId1);
		var commentMessageItem2 = createCommentMessageItem(gxsId1);
		var commentMessageItem3 = createCommentMessageItem(gxsId2);

		gxsCommentMessageRepository.save(commentMessageItem1);
		gxsCommentMessageRepository.save(commentMessageItem2);
		gxsCommentMessageRepository.save(commentMessageItem3);

		var found = gxsCommentMessageRepository.findAllByGxsIdAndMsgIdIn(gxsId1, Set.of(commentMessageItem1.getMsgId(), commentMessageItem2.getMsgId(), commentMessageItem3.getMsgId()));

		assertNotNull(found);
		assertEquals(2, found.size());
		assertTrue(found.contains(commentMessageItem1));
		assertTrue(found.contains(commentMessageItem2));
	}

	private static CommentMessageItem createCommentMessageItem(GxsId gxsId)
	{
		var commentMessageItem = new CommentMessageItem(gxsId, "comment");
		commentMessageItem.setMsgId(IdFakes.createMsgId());
		return commentMessageItem;
	}
}
