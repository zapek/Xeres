/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller.forum;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.forum.ForumMessageItemSummary;
import io.xeres.app.database.model.gxs.ForumGroupItemFakes;
import io.xeres.app.database.model.gxs.ForumMessageItemFakes;
import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.app.service.ForumMessageService;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.UnHtmlService;
import io.xeres.app.xrs.service.forum.ForumRsService;
import io.xeres.app.xrs.service.forum.item.ForumGroupItem;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.common.id.GxsId;
import io.xeres.common.rest.forum.CreateForumGroupRequest;
import io.xeres.common.rest.forum.UpdateForumMessagesReadRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.xeres.common.rest.PathConfig.FORUMS_PATH;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ForumController.class)
@AutoConfigureMockMvc(addFilters = false)
class ForumControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = FORUMS_PATH;

	@MockitoBean
	private ForumRsService forumRsService;

	@MockitoBean
	private IdentityRsService identityRsService;

	@MockitoBean
	private IdentityService identityService;

	@MockitoBean
	private ForumMessageService forumMessageService;

	@MockitoBean
	private UnHtmlService unHtmlService;

	@Test
	void GetForumsGroups_Success() throws Exception
	{
		var forumGroups = List.of(ForumGroupItemFakes.createForumGroupItem(), ForumGroupItemFakes.createForumGroupItem());

		when(forumRsService.findAllGroups()).thenReturn(forumGroups);

		mvc.perform(getJson(BASE_URL + "/groups"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].id").value(is(forumGroups.get(0).getId()), Long.class))
				.andExpect(jsonPath("$.[0].name", is(forumGroups.get(0).getName())))
				.andExpect(jsonPath("$.[1].id").value(is(forumGroups.get(1).getId()), Long.class));

		verify(forumRsService).findAllGroups();
	}

	@Test
	void CreateForumGroup_Success() throws Exception
	{
		var ownIdentity = IdentityFakes.createOwn();

		when(identityService.getOwnIdentity()).thenReturn(ownIdentity);
		when(forumRsService.createForumGroup(eq(ownIdentity.getGxsId()), anyString(), anyString())).thenReturn(1L);

		var request = new CreateForumGroupRequest("foo", "the best");

		mvc.perform(postJson(BASE_URL + "/groups", request))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + FORUMS_PATH + "/groups/" + 1L));

		verify(forumRsService).createForumGroup(eq(ownIdentity.getGxsId()), anyString(), anyString());
	}

	@Test
	void GetForumByGroupId_Success() throws Exception
	{
		long groupId = 1L;
		var forumGroupItem = new ForumGroupItem(null, "foobar");

		when(forumRsService.findById(groupId)).thenReturn(Optional.of(forumGroupItem));

		mvc.perform(getJson(BASE_URL + "/groups/" + groupId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(is(forumGroupItem.getId()), Long.class));
	}

	@Test
	void UpdateMessagesReadFlag_Success() throws Exception
	{
		var ids = Map.of(1L, true, 2L, true, 3L, false);
		var request = new UpdateForumMessagesReadRequest(ids);

		mvc.perform(patchJson(BASE_URL + "/messages", request))
				.andExpect(status().isOk());

		verify(forumRsService).setForumMessagesAsRead(ids);
	}

	@Test
	void GetForumUnreadCount_Success() throws Exception
	{
		long groupId = 1L;
		int unreadCount = 5;

		when(forumRsService.getUnreadCount(groupId)).thenReturn(unreadCount);

		mvc.perform(getJson(BASE_URL + "/groups/" + groupId + "/unread-count"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().string(String.valueOf(unreadCount)));

		verify(forumRsService).getUnreadCount(groupId);
	}

	@Test
	void SubscribeToForumGroup_Success() throws Exception
	{
		long groupId = 1L;

		mvc.perform(put(BASE_URL + "/groups/" + groupId + "/subscription"))
				.andExpect(status().isNoContent());

		verify(forumRsService).subscribeToForumGroup(groupId);
	}

	@Test
	void UnsubscribeFromForumGroup_Success() throws Exception
	{
		long groupId = 1L;

		mvc.perform(delete(BASE_URL + "/groups/" + groupId + "/subscription"))
				.andExpect(status().isNoContent());

		verify(forumRsService).unsubscribeFromForumGroup(groupId);
	}

	@Test
	void GetForumMessages_Success() throws Exception
	{
		long groupId = 1L;
		List<ForumMessageItemSummary> forumMessages = List.of(ForumMessageItemFakes.createForumMessageItemSummary(), ForumMessageItemFakes.createForumMessageItemSummary());

		when(forumRsService.findAllMessagesSummary(groupId)).thenReturn(forumMessages);
		when(forumMessageService.getAuthorsMapFromSummaries(forumMessages)).thenReturn(Map.of());
		when(forumMessageService.getMessagesMapFromSummaries(groupId, forumMessages)).thenReturn(Map.of());

		mvc.perform(getJson(BASE_URL + "/groups/" + groupId + "/messages"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size()", is(forumMessages.size())));

		verify(forumRsService).findAllMessagesSummary(groupId);
		verify(forumMessageService).getAuthorsMapFromSummaries(forumMessages);
		verify(forumMessageService).getMessagesMapFromSummaries(groupId, forumMessages);
	}

	@Test
	void GetForumMessage_Success() throws Exception
	{
		long id = 1L;
		ForumMessageItem forumMessage = ForumMessageItemFakes.createForumMessageItem();

		when(forumRsService.findMessageById(id)).thenReturn(forumMessage);
		when(identityService.findByGxsId(any(GxsId.class))).thenReturn(Optional.empty());
		when(forumRsService.findAllMessages(any(GxsId.class), anySet())).thenReturn(List.of());

		mvc.perform(getJson(BASE_URL + "/messages/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(is((int) forumMessage.getId())));

		verify(forumRsService).findMessageById(id);
		verify(identityService).findByGxsId(null);
		verify(forumRsService).findAllMessages(any(GxsId.class), anySet());
	}

	@Test
	void CreateForumMessage_Success() throws Exception
	{
		var ownIdentity = IdentityFakes.createOwn();

		when(identityService.getOwnIdentity()).thenReturn(ownIdentity);
		when(forumRsService.createForumMessage(
				eq(ownIdentity),
				anyLong(),
				anyString(),
				anyString(),
				anyLong(),
				anyLong()
		)).thenReturn(1L);

		String requestBody = "{\"forumId\":1,\"title\":\"Test Title\",\"content\":\"Test Content\"}";

		mvc.perform(post(BASE_URL + "/messages")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + FORUMS_PATH + "/messages/" + 1L));

		verify(forumRsService).createForumMessage(
				eq(ownIdentity),
				anyLong(),
				anyString(),
				anyString(),
				anyLong(),
				anyLong()
		);
	}
}
