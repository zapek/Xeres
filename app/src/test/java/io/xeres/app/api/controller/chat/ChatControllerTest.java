/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller.chat;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.chat.ChatBacklog;
import io.xeres.app.database.model.chat.ChatRoomBacklog;
import io.xeres.app.database.model.chat.ChatRoomFakes;
import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.LocationService;
import io.xeres.app.xrs.service.chat.ChatBacklogService;
import io.xeres.app.xrs.service.chat.ChatRsService;
import io.xeres.app.xrs.service.chat.RoomFlags;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.message.chat.ChatRoomContext;
import io.xeres.common.message.chat.ChatRoomInfo;
import io.xeres.common.message.chat.ChatRoomLists;
import io.xeres.common.message.chat.ChatRoomUser;
import io.xeres.common.rest.chat.ChatRoomVisibility;
import io.xeres.common.rest.chat.CreateChatRoomRequest;
import io.xeres.common.rest.chat.InviteToChatRoomRequest;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.xeres.common.rest.PathConfig.CHAT_PATH;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = CHAT_PATH;

	@MockitoBean
	private ChatRsService chatRsService;

	@MockitoBean
	private ChatBacklogService chatBacklogService;

	@MockitoBean
	private LocationService locationService;

	@MockitoBean
	private IdentityService identityService;

	@Autowired
	public MockMvc mvc;

	@Test
	void CreateChatRoom_Success() throws Exception
	{
		var chatRoomRequest = new CreateChatRoomRequest("The Elephant Room", "Nothing to see here", ChatRoomVisibility.PUBLIC, false);

		when(chatRsService.createChatRoom(chatRoomRequest.name(), chatRoomRequest.topic(), EnumSet.of(RoomFlags.PUBLIC), false)).thenReturn(1L);

		mvc.perform(postJson(BASE_URL + "/rooms", chatRoomRequest))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + CHAT_PATH + "/rooms/" + 1L));

		verify(chatRsService).createChatRoom(chatRoomRequest.name(), chatRoomRequest.topic(), EnumSet.of(RoomFlags.PUBLIC), false);
	}

	@Test
	void InviteToChatRoom_Success() throws Exception
	{
		var chatRoomId = 1L;
		var locations = Set.of(LocationFakes.createLocation().getLocationIdentifier(), LocationFakes.createLocation().getLocationIdentifier());

		var inviteRequest = new InviteToChatRoomRequest(chatRoomId, locations.stream()
				.map(LocationIdentifier::toString)
				.collect(Collectors.toSet()));

		mvc.perform(postJson(BASE_URL + "/rooms/invite", inviteRequest))
				.andExpect(status().isOk());

		verify(chatRsService).inviteLocationsToChatRoom(chatRoomId, locations);
	}

	@Test
	void SubscribeToChatRoom_Success() throws Exception
	{
		var id = 1L;

		mvc.perform(put(BASE_URL + "/rooms/" + id + "/subscription"))
				.andExpect(status().isNoContent());

		verify(chatRsService).joinChatRoom(id);
	}

	@Test
	void UnsubscribeFromChatRoom_Success() throws Exception
	{
		var id = 1L;

		mvc.perform(delete(BASE_URL + "/rooms/" + id + "/subscription"))
				.andExpect(status().isNoContent());

		verify(chatRsService).leaveChatRoom(id);
	}

	@Test
	void GetChatRoomContext_Success() throws Exception
	{
		var subscribedChatRoom = new ChatRoomInfo("SubscribedRoom");
		var availableChatRoom = new ChatRoomInfo("AvailableRoom");
		var chatRoomLists = new ChatRoomLists();
		chatRoomLists.addSubscribed(subscribedChatRoom);
		chatRoomLists.addAvailable(availableChatRoom);
		var ownIdentity = IdentityFakes.createOwn();
		var chatRoomUser = new ChatRoomUser(ownIdentity.getName(), ownIdentity.getGxsId(), ownIdentity.getId());
		when(chatRsService.getChatRoomContext()).thenReturn(new ChatRoomContext(chatRoomLists, chatRoomUser));

		mvc.perform(getJson(BASE_URL + "/rooms"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.chatRooms.subscribed[0].name", is(subscribedChatRoom.getName())))
				.andExpect(jsonPath("$.chatRooms.available[0].name", is(availableChatRoom.getName())))
				.andExpect(jsonPath("$.identity.nickname", is(ownIdentity.getName())))
				.andExpect(jsonPath("$.identity.gxsId.bytes", is(Base64.toBase64String(ownIdentity.getGxsId().getBytes()))));

		verify(chatRsService).getChatRoomContext();
	}

	@Test
	void GetChatMessages_Default_Success() throws Exception
	{
		var creation = Instant.now();
		var location = LocationFakes.createLocation();
		var chatBacklog = new ChatBacklog(location, false, "hey");
		chatBacklog.setCreated(creation);

		when(locationService.findLocationById(location.getId())).thenReturn(Optional.of(location));
		when(chatBacklogService.getMessages(eq(location), any(Instant.class), anyInt())).thenReturn(List.of(chatBacklog));

		mvc.perform(getJson(BASE_URL + "/chats/" + location.getId() + "/messages"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].created", is(creation.toString())))
				.andExpect(jsonPath("$[0].own", is(false)))
				.andExpect(jsonPath("$[0].message", is("hey")));

		verify(chatBacklogService).getMessages(eq(location), any(Instant.class), eq(20));
	}

	@Test
	void GetChatMessages_WithParameters_Success() throws Exception
	{
		var creation = Instant.now();
		var location = LocationFakes.createLocation();
		var chatBacklog = new ChatBacklog(location, false, "hey");
		chatBacklog.setCreated(creation);

		when(locationService.findLocationById(location.getId())).thenReturn(Optional.of(location));
		when(chatBacklogService.getMessages(eq(location), any(Instant.class), anyInt())).thenReturn(List.of(chatBacklog));

		mvc.perform(getJson(BASE_URL + "/chats/" + location.getId() + "/messages?maxLines=30&from=2024-12-23T22:13"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].created", is(creation.toString())))
				.andExpect(jsonPath("$[0].own", is(false)))
				.andExpect(jsonPath("$[0].message", is("hey")));

		verify(chatBacklogService).getMessages(location, LocalDateTime.parse("2024-12-23T22:13").toInstant(ZoneOffset.UTC), 30);
	}

	@Test
	void GetChatRoomMessages_Default_Success() throws Exception
	{
		var creation = Instant.now();
		var chatRoom = ChatRoomFakes.createChatRoomEntity();
		var ownIdentity = IdentityFakes.createOwn();
		var chatRoomBacklog = new ChatRoomBacklog(chatRoom, ownIdentity.getGxsId(), "Foobar", "blabla");
		chatRoomBacklog.setCreated(creation);

		when(chatBacklogService.getChatRoomMessages(eq(chatRoom.getRoomId()), any(Instant.class), anyInt())).thenReturn(List.of(chatRoomBacklog));

		mvc.perform(getJson(BASE_URL + "/rooms/" + chatRoom.getRoomId() + "/messages"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].created", is(creation.toString())))
				.andExpect(jsonPath("$[0].gxsId.bytes", is(Base64.toBase64String(ownIdentity.getGxsId().getBytes()))))
				.andExpect(jsonPath("$[0].nickname", is("Foobar")))
				.andExpect(jsonPath("$[0].message", is("blabla")));

		verify(chatBacklogService).getChatRoomMessages(eq(chatRoom.getRoomId()), any(Instant.class), eq(50));
	}

	@Test
	void GetChatRoomMessages_WithParameters_Success() throws Exception
	{
		var creation = Instant.now();
		var chatRoom = ChatRoomFakes.createChatRoomEntity();
		var ownIdentity = IdentityFakes.createOwn();
		var chatRoomBacklog = new ChatRoomBacklog(chatRoom, ownIdentity.getGxsId(), "Foobar", "blabla");
		chatRoomBacklog.setCreated(creation);

		when(chatBacklogService.getChatRoomMessages(eq(chatRoom.getRoomId()), any(Instant.class), anyInt())).thenReturn(List.of(chatRoomBacklog));

		mvc.perform(getJson(BASE_URL + "/rooms/" + chatRoom.getRoomId() + "/messages?maxLines=80&from=2024-12-24T01:27"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].created", is(creation.toString())))
				.andExpect(jsonPath("$[0].gxsId.bytes", is(Base64.toBase64String(ownIdentity.getGxsId().getBytes()))))
				.andExpect(jsonPath("$[0].nickname", is("Foobar")))
				.andExpect(jsonPath("$[0].message", is("blabla")));

		verify(chatBacklogService).getChatRoomMessages(chatRoom.getRoomId(), LocalDateTime.parse("2024-12-24T01:27").toInstant(ZoneOffset.UTC), 80);
	}
}
