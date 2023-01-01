/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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
import io.xeres.app.database.model.identity.GxsIdFakes;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.xrs.service.chat.ChatRsService;
import io.xeres.app.xrs.service.chat.RoomFlags;
import io.xeres.common.id.LocationId;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static io.xeres.common.rest.PathConfig.CHAT_PATH;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = CHAT_PATH;

	@MockBean
	private ChatRsService chatRsService;

	@Autowired
	public MockMvc mvc;

	@Test
	void ChatController_CreateChatRoom_OK() throws Exception
	{
		var chatRoomRequest = new CreateChatRoomRequest("The Elephant Room", "Nothing to see here", ChatRoomVisibility.PUBLIC, false);

		when(chatRsService.createChatRoom(chatRoomRequest.name(), chatRoomRequest.topic(), EnumSet.of(RoomFlags.PUBLIC), false)).thenReturn(1L);

		mvc.perform(postJson(BASE_URL + "/rooms", chatRoomRequest))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + CHAT_PATH + "/rooms/" + 1L));

		verify(chatRsService).createChatRoom(chatRoomRequest.name(), chatRoomRequest.topic(), EnumSet.of(RoomFlags.PUBLIC), false);
	}

	@Test
	void ChatRoomController_InviteToChatRoom_OK() throws Exception
	{
		var chatRoomId = 1L;
		var locations = Set.of(LocationFakes.createLocation().getLocationId(), LocationFakes.createLocation().getLocationId());

		var inviteRequest = new InviteToChatRoomRequest(chatRoomId, locations.stream()
				.map(LocationId::toString)
				.collect(Collectors.toSet()));

		mvc.perform(postJson(BASE_URL + "/rooms/invite", inviteRequest))
				.andExpect(status().isOk());

		verify(chatRsService).inviteLocationsToChatRoom(chatRoomId, locations);
	}

	@Test
	void ChatController_SubscribeToChatRoom_OK() throws Exception
	{
		var id = 1L;

		mvc.perform(put(BASE_URL + "/rooms/" + id + "/subscription"))
				.andExpect(status().isOk())
				.andExpect(content().string(String.valueOf(id)));

		verify(chatRsService).joinChatRoom(id);
	}

	@Test
	void ChatController_UnsubscribeFromChatRoom_OK() throws Exception
	{
		var id = 1L;

		mvc.perform(delete(BASE_URL + "/rooms/" + id + "/subscription"))
				.andExpect(status().isNoContent());

		verify(chatRsService).leaveChatRoom(id);
	}

	@Test
	void ChatController_GetChatRoomContext_OK() throws Exception
	{
		var subscribedChatRoom = new ChatRoomInfo("SubscribedRoom");
		var availableChatRoom = new ChatRoomInfo("AvailableRoom");
		var chatRoomLists = new ChatRoomLists();
		chatRoomLists.addSubscribed(subscribedChatRoom);
		chatRoomLists.addAvailable(availableChatRoom);
		var ownIdentity = GxsIdFakes.createOwnIdentity();
		var chatRoomUser = new ChatRoomUser(ownIdentity.getName(), ownIdentity.getGxsId(), ownIdentity.getImage());
		when(chatRsService.getChatRoomContext()).thenReturn(new ChatRoomContext(chatRoomLists, chatRoomUser));

		mvc.perform(getJson(BASE_URL + "/rooms"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.chatRooms.subscribed[0].name", is(subscribedChatRoom.getName())))
				.andExpect(jsonPath("$.chatRooms.available[0].name", is(availableChatRoom.getName())))
				.andExpect(jsonPath("$.identity.nickname", is(ownIdentity.getName())))
				.andExpect(jsonPath("$.identity.gxsId.bytes", is(Base64.toBase64String(ownIdentity.getGxsId().getBytes()))));

		verify(chatRsService).getChatRoomContext();
	}
}
