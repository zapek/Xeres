/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.service;

import io.xeres.app.database.model.gxs.BoardMessageItemFakes;
import io.xeres.app.xrs.service.board.BoardRsService;
import io.xeres.app.xrs.service.board.item.BoardMessageItem;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;
import io.xeres.testutils.IdFakes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardMessageServiceTest
{
	@Mock
	private BoardRsService boardRsService;

	@Mock
	private IdentityService identityService;

	@InjectMocks
	private BoardMessageService boardMessageService;

	@Test
	void getAuthorsMapFromMessages_ShouldReturnCorrectMap()
	{
		var gxsId = IdFakes.createGxsId();
		var message = BoardMessageItemFakes.createBoardMessageItem();
		message.setAuthorGxsId(gxsId);
		var identityGroupItem = new IdentityGroupItem();
		identityGroupItem.setGxsId(gxsId);

		when(identityService.findAll(Set.of(gxsId)))
				.thenReturn(List.of(identityGroupItem));

		Map<GxsId, IdentityGroupItem> result = boardMessageService.getAuthorsMapFromMessages(new PageImpl<>(List.of(message)));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.containsKey(gxsId));
		assertEquals(identityGroupItem, result.get(gxsId));
	}

	@Test
	void getAuthorsMapFromMessages_WithSameAuthor_ShouldQueryUniqueAuthorsOnly()
	{
		var gxsId = IdFakes.createGxsId();
		var message1 = new BoardMessageItem(gxsId, IdFakes.createMsgId(), "First");
		message1.setAuthorGxsId(gxsId);
		var message2 = new BoardMessageItem(gxsId, IdFakes.createMsgId(), "Second");
		message2.setAuthorGxsId(gxsId);
		var identityGroupItem = new IdentityGroupItem();
		identityGroupItem.setGxsId(gxsId);

		when(identityService.findAll(Set.of(gxsId)))
				.thenReturn(List.of(identityGroupItem));

		Map<GxsId, IdentityGroupItem> result = boardMessageService.getAuthorsMapFromMessages(new PageImpl<>(List.of(message1, message2)));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.containsKey(gxsId));
	}

	@Test
	void getMessagesMapFromSummaries_ShouldReturnCorrectMap()
	{
		var msgId = IdFakes.createMsgId();
		var parentMsgId = IdFakes.createMsgId();
		var groupId = 1L;

		var summary = new BoardMessageItem(IdFakes.createGxsId(), msgId, "Summary");
		summary.setParentMsgId(parentMsgId);

		var message = new BoardMessageItem();
		message.setMsgId(msgId);

		when(boardRsService.findAllMessages(groupId, Set.of(msgId, parentMsgId)))
				.thenReturn(List.of(message));

		Map<MsgId, BoardMessageItem> result = boardMessageService.getMessagesMapFromSummaries(groupId, new PageImpl<>(List.of(summary)));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.containsKey(msgId));
		assertEquals(message, result.get(msgId));
	}

	@Test
	void getMessagesMapFromSummaries_WithoutParent_ShouldFilterNullIds()
	{
		var msgId = IdFakes.createMsgId();
		var groupId = 1L;

		var summary = new BoardMessageItem(IdFakes.createGxsId(), msgId, "Summary");

		var message = new BoardMessageItem();
		message.setMsgId(msgId);

		when(boardRsService.findAllMessages(groupId, Set.of(msgId)))
				.thenReturn(List.of(message));

		Map<MsgId, BoardMessageItem> result = boardMessageService.getMessagesMapFromSummaries(groupId, new PageImpl<>(List.of(summary)));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.containsKey(msgId));
	}

	@Test
	void getMessagesMapFromMessages_ShouldReturnCorrectMap()
	{
		var msgId = IdFakes.createMsgId();
		var parentMsgId = IdFakes.createMsgId();

		var message1 = new BoardMessageItem();
		message1.setMsgId(msgId);
		message1.setParentMsgId(parentMsgId);

		var message2 = new BoardMessageItem();
		message2.setMsgId(parentMsgId);

		when(boardRsService.findAllMessages(Set.of(msgId, parentMsgId)))
				.thenReturn(List.of(message1, message2));

		Map<MsgId, BoardMessageItem> result = boardMessageService.getMessagesMapFromMessages(List.of(message1));

		assertNotNull(result);
		assertEquals(2, result.size());
		assertTrue(result.containsKey(msgId));
		assertTrue(result.containsKey(parentMsgId));
		assertEquals(message1, result.get(msgId));
		assertEquals(message2, result.get(parentMsgId));
	}
}
