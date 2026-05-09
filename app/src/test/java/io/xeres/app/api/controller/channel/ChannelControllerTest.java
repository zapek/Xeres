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

package io.xeres.app.api.controller.channel;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.gxs.ChannelGroupItemFakes;
import io.xeres.app.database.model.gxs.ChannelMessageItemFakes;
import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.app.service.ChannelMessageService;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.UnHtmlService;
import io.xeres.app.xrs.service.channel.ChannelRsService;
import io.xeres.app.xrs.service.channel.item.ChannelMessageItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.rest.channel.UpdateChannelMessageReadRequest;
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

import static io.xeres.common.rest.PathConfig.CHANNELS_PATH;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChannelController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChannelControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = CHANNELS_PATH;

	@MockitoBean
	private ChannelRsService channelRsService;

	@MockitoBean
	private IdentityService identityService;

	@MockitoBean
	private ChannelMessageService channelMessageService;

	@MockitoBean
	private UnHtmlService unHtmlService;

	@Test
	void GetChannelGroups_Success() throws Exception
	{
		var channelGroups = List.of(ChannelGroupItemFakes.createChannelGroupItem(), ChannelGroupItemFakes.createChannelGroupItem());

		when(channelRsService.findAllGroups()).thenReturn(channelGroups);

		mvc.perform(getJson(BASE_URL + "/groups"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].id").value(is(channelGroups.get(0).getId()), Long.class))
				.andExpect(jsonPath("$.[0].name", is(channelGroups.get(0).getName())))
				.andExpect(jsonPath("$.[1].id").value(is(channelGroups.get(1).getId()), Long.class));

		verify(channelRsService).findAllGroups();
	}

	@Test
	void CreateChannelGroup_Success() throws Exception
	{
		var ownIdentity = IdentityFakes.createOwn();

		when(identityService.getOwnIdentity()).thenReturn(ownIdentity);
		when(channelRsService.createChannelGroup(eq(ownIdentity.getGxsId()), eq("foo"), eq("the best"), any())).thenReturn(1L);

		mvc.perform(multipart(BASE_URL + "/groups")
						.param("name", "foo")
						.param("description", "the best"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + CHANNELS_PATH + "/groups/" + 1L));

		verify(channelRsService).createChannelGroup(eq(ownIdentity.getGxsId()), anyString(), anyString(), any());
	}

	@Test
	void UpdateChannelGroup_Success() throws Exception
	{
		mvc.perform(multipart(HttpMethod.PUT, BASE_URL + "/groups/1")
						.param("name", "foo")
						.param("description", "the best"))
				.andExpect(status().isNoContent());

		verify(channelRsService).updateChannelGroup(1L, "foo", "the best", null, false);
	}

	@Test
	void UpdateChannelGroup_WithUpdateImageFlag_Success() throws Exception
	{
		mvc.perform(multipart(HttpMethod.PUT, BASE_URL + "/groups/1")
						.param("name", "foo")
						.param("description", "the best")
						.param("updateImage", "true"))
				.andExpect(status().isNoContent());

		verify(channelRsService).updateChannelGroup(1L, "foo", "the best", null, true);
	}

	@Test
	void GetChannelGroupById_Success() throws Exception
	{
		long groupId = 1L;
		var channelGroupItem = ChannelGroupItemFakes.createChannelGroupItem();

		when(channelRsService.findById(groupId)).thenReturn(Optional.of(channelGroupItem));

		mvc.perform(getJson(BASE_URL + "/groups/" + groupId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(is(channelGroupItem.getId()), Long.class));
	}

	@Test
	void UpdateChannelMessageReadFlag_Success() throws Exception
	{
		var request = new UpdateChannelMessageReadRequest(1L, true);

		mvc.perform(patchJson(BASE_URL + "/messages", request))
				.andExpect(status().isOk());

		verify(channelRsService).setMessageReadState(1L, true);
	}

	@Test
	void GetChannelUnreadCount_Success() throws Exception
	{
		long groupId = 1L;
		int unreadCount = 5;

		when(channelRsService.getUnreadCount(groupId)).thenReturn(unreadCount);

		mvc.perform(getJson(BASE_URL + "/groups/" + groupId + "/unread-count"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().string(String.valueOf(unreadCount)));

		verify(channelRsService).getUnreadCount(groupId);
	}

	@Test
	void SubscribeToChannelGroup_Success() throws Exception
	{
		long groupId = 1L;

		mvc.perform(put(BASE_URL + "/groups/" + groupId + "/subscription"))
				.andExpect(status().isNoContent());

		verify(channelRsService).subscribeToChannelGroup(groupId);
	}

	@Test
	void SetAllGroupMessagesReadState_Success() throws Exception
	{
		long groupId = 1L;

		mvc.perform(put(BASE_URL + "/groups/" + groupId + "/read?read=true"))
				.andExpect(status().isNoContent());

		verify(channelRsService).setAllGroupMessagesReadState(groupId, true);
	}

	@Test
	void UnsubscribeFromChannelGroup_Success() throws Exception
	{
		long groupId = 1L;

		mvc.perform(delete(BASE_URL + "/groups/" + groupId + "/subscription"))
				.andExpect(status().isNoContent());

		verify(channelRsService).unsubscribeFromChannelGroup(groupId);
	}

	@Test
	void GetChannelMessages_Success() throws Exception
	{
		long groupId = 1L;
		Page<ChannelMessageItem> channelMessages = new PageImpl<>(List.of(ChannelMessageItemFakes.createChannelMessageItem(), ChannelMessageItemFakes.createChannelMessageItem()));

		when(channelRsService.findAllMessages(eq(groupId), any(Pageable.class))).thenReturn(channelMessages);
		when(channelMessageService.getAuthorsMapFromMessages(channelMessages)).thenReturn(Collections.emptyMap());
		when(channelMessageService.getMessagesMapFromSummaries(groupId, channelMessages)).thenReturn(Collections.emptyMap());

		mvc.perform(getJson(BASE_URL + "/groups/" + groupId + "/messages"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.size()").value(is(channelMessages.getTotalElements()), Long.class));

		verify(channelRsService).findAllMessages(eq(groupId), any(Pageable.class));
		verify(channelMessageService).getAuthorsMapFromMessages(channelMessages);
		verify(channelMessageService).getMessagesMapFromSummaries(groupId, channelMessages);
	}

	@Test
	void GetChannelMessage_Success() throws Exception
	{
		long id = 1L;
		ChannelMessageItem channelMessage = ChannelMessageItemFakes.createChannelMessageItem();

		when(channelRsService.findMessageById(id)).thenReturn(Optional.of(channelMessage));
		when(identityService.findByGxsId(any(GxsId.class))).thenReturn(Optional.empty());
		when(channelRsService.findAllMessagesIncludingOlds(any(GxsId.class), anySet())).thenReturn(List.of());

		mvc.perform(getJson(BASE_URL + "/messages/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(is((int) channelMessage.getId())));

		verify(channelRsService).findMessageById(id);
		verify(identityService).findByGxsId(null);
		verify(channelRsService).findAllMessagesIncludingOlds(any(GxsId.class), anySet());
	}

	@Test
	void CreateChannelMessage_Success() throws Exception
	{
		var ownIdentity = IdentityFakes.createOwn();

		when(identityService.getOwnIdentity()).thenReturn(ownIdentity);
		when(channelRsService.createChannelMessage(
				eq(ownIdentity),
				eq(1L),
				eq("Test Title"),
				eq("Test Content"),
				any(),
				any(),
				eq(0L)
		)).thenReturn(1L);

		mvc.perform(multipart(BASE_URL + "/messages")
						.param("channelId", "1")
						.param("title", "Test Title")
						.param("content", "Test Content"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + CHANNELS_PATH + "/messages/" + 1L));

		verify(channelRsService).createChannelMessage(
				eq(ownIdentity),
				anyLong(),
				anyString(),
				anyString(),
				any(),
				any(),
				anyLong()
		);
	}

	@Test
	void CreateChannelMessage_WithOptionalFields_Success() throws Exception
	{
		var ownIdentity = IdentityFakes.createOwn();

		when(identityService.getOwnIdentity()).thenReturn(ownIdentity);
		when(channelRsService.createChannelMessage(
				eq(ownIdentity),
				eq(1L),
				eq("Test Title"),
				eq("Test Content"),
				any(),
				any(),
				eq(5L)
		)).thenReturn(1L);

		mvc.perform(multipart(BASE_URL + "/messages")
						.param("channelId", "1")
						.param("title", "Test Title")
						.param("content", "Test Content")
						.param("originalId", "5"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + CHANNELS_PATH + "/messages/" + 1L));

		verify(channelRsService).createChannelMessage(
				eq(ownIdentity),
				anyLong(),
				anyString(),
				anyString(),
				any(),
				any(),
				anyLong()
		);
	}
}