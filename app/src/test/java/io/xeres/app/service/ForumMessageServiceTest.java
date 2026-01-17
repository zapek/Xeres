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

import io.xeres.app.database.model.gxs.ForumMessageItemFakes;
import io.xeres.app.xrs.service.forum.ForumRsService;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
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
class ForumMessageServiceTest
{
	@Mock
	private ForumRsService forumRsService;

	@Mock
	private IdentityService identityService;

	@InjectMocks
	private ForumMessageService forumMessageService;

	@Test
	void getAuthorsMapFromSummaries_ShouldReturnCorrectMap()
	{
		var gxsId = IdFakes.createGxsId();
		var summary = ForumMessageItemFakes.createForumMessageItemSummary(IdFakes.createMessageId(), gxsId, null);
		var identityGroupItem = new IdentityGroupItem();
		identityGroupItem.setGxsId(gxsId);

		when(identityService.findAll(Set.of(gxsId)))
				.thenReturn(List.of(identityGroupItem));

		Map<GxsId, IdentityGroupItem> result = forumMessageService.getAuthorsMapFromSummaries(new PageImpl<>(List.of(summary)));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.containsKey(gxsId));
		assertEquals(identityGroupItem, result.get(gxsId));
	}

	@Test
	void getAuthorsMapFromMessages_ShouldReturnCorrectMap()
	{
		var gxsId = IdFakes.createGxsId();
		var message = ForumMessageItemFakes.createForumMessageItem();
		message.setAuthorId(gxsId);
		var identityGroupItem = new IdentityGroupItem();
		identityGroupItem.setGxsId(gxsId);

		when(identityService.findAll(Set.of(gxsId)))
				.thenReturn(List.of(identityGroupItem));

		Map<GxsId, IdentityGroupItem> result = forumMessageService.getAuthorsMapFromMessages(List.of(message));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.containsKey(gxsId));
		assertEquals(identityGroupItem, result.get(gxsId));
	}

	@Test
	void getMessagesMapFromSummaries_ShouldReturnCorrectMap()
	{
		var messageId = IdFakes.createMessageId();
		var parentId = IdFakes.createMessageId();
		var groupId = 1L;

		var summary = ForumMessageItemFakes.createForumMessageItemSummary(messageId, null, parentId);

		var message = new ForumMessageItem();
		message.setMessageId(messageId);

		when(forumRsService.findAllMessages(groupId, Set.of(messageId, parentId)))
				.thenReturn(List.of(message));

		Map<MessageId, ForumMessageItem> result = forumMessageService.getMessagesMapFromSummaries(groupId, new PageImpl<>(List.of(summary)));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.containsKey(messageId));
		assertEquals(message, result.get(messageId));
	}

	@Test
	void getMessagesMapFromMessages_WithGroupId_ShouldReturnCorrectMap()
	{
		var messageId = IdFakes.createMessageId();
		var parentId = IdFakes.createMessageId();
		var groupId = 1L;

		var message = new ForumMessageItem();
		message.setMessageId(messageId);
		message.setParentId(parentId);

		when(forumRsService.findAllMessages(groupId, Set.of(messageId, parentId)))
				.thenReturn(List.of(message));

		Map<MessageId, ForumMessageItem> result = forumMessageService.getMessagesMapFromMessages(groupId, List.of(message));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.containsKey(messageId));
		assertEquals(message, result.get(messageId));
	}

	@Test
	void getMessagesMapFromMessages_WithoutGroupId_ShouldReturnCorrectMap()
	{
		var messageId = IdFakes.createMessageId();
		var parentId = IdFakes.createMessageId();

		var message = new ForumMessageItem();
		message.setMessageId(messageId);
		message.setParentId(parentId);

		when(forumRsService.findAllMessages(Set.of(messageId, parentId)))
				.thenReturn(List.of(message));

		Map<MessageId, ForumMessageItem> result = forumMessageService.getMessagesMapFromMessages(List.of(message));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.containsKey(messageId));
		assertEquals(message, result.get(messageId));
	}
}