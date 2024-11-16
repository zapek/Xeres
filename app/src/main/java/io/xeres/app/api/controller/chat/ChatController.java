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

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.service.LocationService;
import io.xeres.app.xrs.service.chat.ChatBacklogService;
import io.xeres.app.xrs.service.chat.ChatRsService;
import io.xeres.app.xrs.service.chat.RoomFlags;
import io.xeres.common.dto.chat.ChatBacklogDTO;
import io.xeres.common.dto.chat.ChatRoomBacklogDTO;
import io.xeres.common.dto.chat.ChatRoomContextDTO;
import io.xeres.common.id.LocationId;
import io.xeres.common.rest.chat.ChatRoomVisibility;
import io.xeres.common.rest.chat.CreateChatRoomRequest;
import io.xeres.common.rest.chat.InviteToChatRoomRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static io.xeres.app.database.model.chat.ChatMapper.*;
import static io.xeres.common.rest.PathConfig.CHAT_PATH;

@Tag(name = "Chat", description = "Chat service", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/chat", description = "Chat documentation"))
@RestController
@RequestMapping(value = CHAT_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ChatController
{
	private final ChatRsService chatRsService;
	private final ChatBacklogService chatBacklogService;
	private final LocationService locationService;

	public ChatController(ChatRsService chatRsService, ChatBacklogService chatBacklogService, LocationService locationService)
	{
		this.chatRsService = chatRsService;
		this.chatBacklogService = chatBacklogService;
		this.locationService = locationService;
	}

	@PostMapping("/rooms")
	@Operation(summary = "Create a chat room")
	@ApiResponse(responseCode = "201", description = "Room created successfully", headers = @Header(name = "Room", description = "The location of the created room", schema = @Schema(type = "string")))
	public ResponseEntity<Void> createChatRoom(@Valid @RequestBody CreateChatRoomRequest createChatRoomRequest)
	{
		var id = chatRsService.createChatRoom(createChatRoomRequest.name(),
				createChatRoomRequest.topic(),
				createChatRoomRequest.visibility() == ChatRoomVisibility.PUBLIC ? EnumSet.of(RoomFlags.PUBLIC) : EnumSet.noneOf(RoomFlags.class),
				createChatRoomRequest.signedIdentities());

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(CHAT_PATH + "/rooms/{id}").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}

	@PostMapping("/rooms/invite")
	@Operation(summary = "Invite locations to a chat room")
	@ApiResponse(responseCode = "200", description = "Peers invited successfully")
	public void inviteToChatRoom(@Valid @RequestBody InviteToChatRoomRequest inviteToChatRoomRequest)
	{
		chatRsService.inviteLocationsToChatRoom(inviteToChatRoomRequest.chatRoomId(), inviteToChatRoomRequest.locationIds().stream()
				.map(LocationId::fromString)
				.collect(Collectors.toSet()));
	}

	@PutMapping("/rooms/{id}/subscription")
	@ResponseStatus(HttpStatus.OK)
	public long subscribeToChatRoom(@PathVariable long id)
	{
		chatRsService.joinChatRoom(id);
		return id;
	}

	@DeleteMapping("/rooms/{id}/subscription")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribeFromChatRoom(@PathVariable long id)
	{
		chatRsService.leaveChatRoom(id);
	}

	@GetMapping("/rooms")
	@Operation(summary = "Get a chat room context (all rooms, status, current nickname, etc...)")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public ChatRoomContextDTO getChatRoomContext()
	{
		return toDTO(chatRsService.getChatRoomContext());
	}

	@GetMapping("/rooms/{roomId}/messages")
	@Operation(summary = "Get the chat room messages backlog")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public List<ChatRoomBacklogDTO> getChatRoomMessages(@PathVariable long roomId)
	{
		return toChatRoomBacklogDTOs(chatBacklogService.getChatRoomMessages(roomId, Instant.now().minus(Duration.ofDays(7))));
	}

	@GetMapping("/chats/{locationId}/messages")
	@Operation(summary = "Get the chat messages backlog")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public List<ChatBacklogDTO> getChatMessages(@PathVariable long locationId)
	{
		var location = locationService.findLocationById(locationId).orElseThrow();
		return toChatBacklogDTOs(chatBacklogService.getMessages(location, Instant.now().minus(Duration.ofDays(7))));
	}
}
