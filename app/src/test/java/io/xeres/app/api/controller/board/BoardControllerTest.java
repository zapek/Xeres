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

package io.xeres.app.api.controller.board;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.gxs.BoardGroupItemFakes;
import io.xeres.app.database.model.gxs.BoardMessageItemFakes;
import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.app.service.BoardMessageService;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.UnHtmlService;
import io.xeres.app.xrs.service.board.BoardRsService;
import io.xeres.app.xrs.service.board.item.BoardGroupItem;
import io.xeres.app.xrs.service.board.item.BoardMessageItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.rest.board.UpdateBoardMessageReadRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.xeres.common.rest.PathConfig.BOARDS_PATH;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BoardController.class)
@AutoConfigureMockMvc(addFilters = false)
class BoardControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = BOARDS_PATH;

	@MockitoBean
	private BoardRsService boardRsService;

	@MockitoBean
	private IdentityService identityService;

	@MockitoBean
	private BoardMessageService boardMessageService;

	@MockitoBean
	private UnHtmlService unHtmlService;

	@Test
	void GetBoardGroups_Success() throws Exception
	{
		var boardGroups = List.of(BoardGroupItemFakes.createBoardGroupItem(), BoardGroupItemFakes.createBoardGroupItem());

		when(boardRsService.findAllGroups()).thenReturn(boardGroups);

		mvc.perform(getJson(BASE_URL + "/groups"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].id").value(is(boardGroups.get(0).getId()), Long.class))
				.andExpect(jsonPath("$.[0].name", is(boardGroups.get(0).getName())))
				.andExpect(jsonPath("$.[1].id").value(is(boardGroups.get(1).getId()), Long.class));

		verify(boardRsService).findAllGroups();
	}

	@Test
	void CreateBoardGroup_Success() throws Exception
	{
		var ownIdentity = IdentityFakes.createOwn();

		when(identityService.getOwnIdentity()).thenReturn(ownIdentity);
		when(boardRsService.createBoardGroup(eq(ownIdentity.getGxsId()), eq("foo"), eq("the best"), any())).thenReturn(1L);

		mvc.perform(multipart(BASE_URL + "/groups")
						.param("name", "foo")
						.param("description", "the best"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + BOARDS_PATH + "/groups/" + 1L));

		verify(boardRsService).createBoardGroup(eq(ownIdentity.getGxsId()), anyString(), anyString(), any());
	}

	@Test
	void UpdateBoardGroup_Success() throws Exception
	{
		mvc.perform(multipart(HttpMethod.PUT, BASE_URL + "/groups/1")
						.param("name", "foo")
						.param("description", "the best"))
				.andExpect(status().isNoContent());

		verify(boardRsService).updateBoardGroup(1L, "foo", "the best", null, false);
	}

	@Test
	void UpdateBoardGroup_WithUpdateImageFlag_Success() throws Exception
	{
		mvc.perform(multipart(HttpMethod.PUT, BASE_URL + "/groups/1")
						.param("name", "foo")
						.param("description", "the best")
						.param("updateImage", "true"))
				.andExpect(status().isNoContent());

		verify(boardRsService).updateBoardGroup(1L, "foo", "the best", null, true);
	}

	@Test
	void GetBoardByGroupId_Success() throws Exception
	{
		long groupId = 1L;
		var boardGroupItem = new BoardGroupItem(null, "foobar");

		when(boardRsService.findById(groupId)).thenReturn(Optional.of(boardGroupItem));

		mvc.perform(getJson(BASE_URL + "/groups/" + groupId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(is(boardGroupItem.getId()), Long.class));
	}

	@Test
	void UpdateBoardMessageReadFlag_Success() throws Exception
	{
		var request = new UpdateBoardMessageReadRequest(1L, true);

		mvc.perform(patchJson(BASE_URL + "/messages", request))
				.andExpect(status().isOk());

		verify(boardRsService).setMessageReadState(1L, true);
	}

	@Test
	void GetBoardUnreadCount_Success() throws Exception
	{
		long groupId = 1L;
		int unreadCount = 5;

		when(boardRsService.getUnreadCount(groupId)).thenReturn(unreadCount);

		mvc.perform(getJson(BASE_URL + "/groups/" + groupId + "/unread-count"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().string(String.valueOf(unreadCount)));

		verify(boardRsService).getUnreadCount(groupId);
	}

	@Test
	void SubscribeToBoardGroup_Success() throws Exception
	{
		long groupId = 1L;

		mvc.perform(put(BASE_URL + "/groups/" + groupId + "/subscription"))
				.andExpect(status().isNoContent());

		verify(boardRsService).subscribeToBoardGroup(groupId);
	}

	@Test
	void SetAllGroupMessagesReadState_Success() throws Exception
	{
		long groupId = 1L;

		mvc.perform(put(BASE_URL + "/groups/" + groupId + "/read?read=true"))
				.andExpect(status().isNoContent());

		verify(boardRsService).setAllGroupMessagesReadState(groupId, true);
	}

	@Test
	void UnsubscribeFromBoardGroup_Success() throws Exception
	{
		long groupId = 1L;

		mvc.perform(delete(BASE_URL + "/groups/" + groupId + "/subscription"))
				.andExpect(status().isNoContent());

		verify(boardRsService).unsubscribeFromBoardGroup(groupId);
	}

	@Test
	void GetBoardMessages_Success() throws Exception
	{
		long groupId = 1L;
		Page<BoardMessageItem> boardMessages = new PageImpl<>(List.of(BoardMessageItemFakes.createBoardMessageItem(), BoardMessageItemFakes.createBoardMessageItem()));

		when(boardRsService.findAllMessages(eq(groupId), any(Pageable.class))).thenReturn(boardMessages);
		when(boardMessageService.getAuthorsMapFromMessages(boardMessages)).thenReturn(Collections.emptyMap());
		when(boardMessageService.getMessagesMapFromSummaries(groupId, boardMessages)).thenReturn(Collections.emptyMap());

		mvc.perform(getJson(BASE_URL + "/groups/" + groupId + "/messages"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.size()").value(is(boardMessages.getTotalElements()), Long.class));

		verify(boardRsService).findAllMessages(eq(groupId), any(Pageable.class));
		verify(boardMessageService).getAuthorsMapFromMessages(boardMessages);
		verify(boardMessageService).getMessagesMapFromSummaries(groupId, boardMessages);
	}

	@Test
	void GetBoardMessage_Success() throws Exception
	{
		long id = 1L;
		BoardMessageItem boardMessage = BoardMessageItemFakes.createBoardMessageItem();

		when(boardRsService.findMessageById(id)).thenReturn(Optional.of(boardMessage));
		when(identityService.findByGxsId(any(GxsId.class))).thenReturn(Optional.empty());
		when(boardRsService.findAllMessagesIncludingOlds(any(GxsId.class), anySet())).thenReturn(List.of());

		mvc.perform(getJson(BASE_URL + "/messages/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(is((int) boardMessage.getId())));

		verify(boardRsService).findMessageById(id);
		verify(identityService).findByGxsId(null);
		verify(boardRsService).findAllMessagesIncludingOlds(any(GxsId.class), anySet());
	}

	@Test
	void CreateBoardMessage_Success() throws Exception
	{
		var ownIdentity = IdentityFakes.createOwn();

		when(identityService.getOwnIdentity()).thenReturn(ownIdentity);
		when(boardRsService.createBoardMessage(
				eq(ownIdentity),
				eq(1L),
				eq("Test Title"),
				eq("Test Content"),
				eq("https://zapek.com"),
				any()
		)).thenReturn(1L);

		mvc.perform(multipart(BASE_URL + "/messages")
						.param("boardId", "1")
						.param("title", "Test Title")
						.param("content", "Test Content")
						.param("link", "https://zapek.com"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + BOARDS_PATH + "/messages/" + 1L));

		verify(boardRsService).createBoardMessage(
				eq(ownIdentity),
				anyLong(),
				anyString(),
				anyString(),
				anyString(),
				any()
		);
	}
}