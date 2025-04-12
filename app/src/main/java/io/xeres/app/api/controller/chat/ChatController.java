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

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.LocationService;
import io.xeres.app.xrs.service.chat.ChatBacklogService;
import io.xeres.app.xrs.service.chat.ChatRsService;
import io.xeres.app.xrs.service.chat.RoomFlags;
import io.xeres.common.dto.chat.ChatBacklogDTO;
import io.xeres.common.dto.chat.ChatRoomBacklogDTO;
import io.xeres.common.dto.chat.ChatRoomContextDTO;
import io.xeres.common.dto.location.LocationDTO;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.rest.chat.ChatRoomVisibility;
import io.xeres.common.rest.chat.CreateChatRoomRequest;
import io.xeres.common.rest.chat.DistantChatRequest;
import io.xeres.common.rest.chat.InviteToChatRoomRequest;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static io.xeres.app.database.model.chat.ChatMapper.fromDistantChatBacklogToChatBacklogDTOs;
import static io.xeres.app.database.model.chat.ChatMapper.toChatBacklogDTOs;
import static io.xeres.app.database.model.chat.ChatMapper.toChatRoomBacklogDTOs;
import static io.xeres.app.database.model.chat.ChatMapper.toDTO;
import static io.xeres.app.database.model.location.LocationMapper.toDTO;
import static io.xeres.common.rest.PathConfig.CHAT_PATH;

@Tag(name = "Chat", description = "Chat rooms, private messages, distant chats, ...", externalDocs = @ExternalDocumentation(url = "https://github.com/zapek/Xeres/wiki/Chat", description = "Chat protocol"))
@RestController
@RequestMapping(value = CHAT_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ChatController
{
	private static final int PRIVATE_CHAT_DEFAULT_MAX_LINES = 20;
	private static final Duration PRIVATE_CHAT_DEFAULT_DURATION = Duration.ofDays(7);
	private static final int ROOM_CHAT_DEFAULT_MAX_LINES = 50;
	private static final Duration ROOM_CHAT_DEFAULT_DURATION = Duration.ofDays(7);

	private final ChatRsService chatRsService;
	private final ChatBacklogService chatBacklogService;
	private final LocationService locationService;
	private final IdentityService identityService;

	public ChatController(ChatRsService chatRsService, ChatBacklogService chatBacklogService, LocationService locationService, IdentityService identityService)
	{
		this.chatRsService = chatRsService;
		this.chatBacklogService = chatBacklogService;
		this.locationService = locationService;
		this.identityService = identityService;
	}

	@PostMapping("/rooms")
	@Operation(summary = "Creates a chat room")
	@ApiResponse(responseCode = "201", description = "Chat room created successfully", headers = @Header(name = "Room", description = "The location of the created chat room", schema = @Schema(type = "string")))
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
	@Operation(summary = "Invites one or several locations to a chat room")
	public void inviteToChatRoom(@Valid @RequestBody InviteToChatRoomRequest inviteToChatRoomRequest)
	{
		chatRsService.inviteLocationsToChatRoom(inviteToChatRoomRequest.chatRoomId(), inviteToChatRoomRequest.locationIdentifiers().stream()
				.map(LocationIdentifier::fromString)
				.collect(Collectors.toSet()));
	}

	@PutMapping("/rooms/{id}/subscription")
	@Operation(summary = "Subscribes to a chat room")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void subscribeToChatRoom(@PathVariable @Parameter(description = "The room's unique 64-bit identifier") long id)
	{
		chatRsService.joinChatRoom(id);
	}

	@DeleteMapping("/rooms/{id}/subscription")
	@Operation(summary = "Unsubscribes from a chat room")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribeFromChatRoom(@PathVariable @Parameter(description = "The room's unique 64-bit identifier") long id)
	{
		chatRsService.leaveChatRoom(id);
	}

	@GetMapping("/rooms")
	@Operation(summary = "Gets a chat room context", description = "The context contains all rooms, status, current nickname, etc...")
	public ChatRoomContextDTO getChatRoomContext()
	{
		return toDTO(chatRsService.getChatRoomContext());
	}

	@GetMapping("/rooms/{roomId}/messages")
	@Operation(summary = "Gets the chat room messages backlog")
	@ApiResponse(responseCode = "200", description = "OK")
	@ApiResponse(responseCode = "404", description = "No room found for given id", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public List<ChatRoomBacklogDTO> getChatRoomMessages(@PathVariable @Parameter(description = "The room's unique 64-bit identifier") long roomId,
	                                                    @RequestParam(value = "maxLines", required = false) @Min(1) @Max(500) Integer maxLines,
	                                                    @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from)
	{
		return toChatRoomBacklogDTOs(chatBacklogService.getChatRoomMessages(
				roomId,
				from != null ? from.toInstant(ZoneOffset.UTC) : Instant.now().minus(ROOM_CHAT_DEFAULT_DURATION),
				maxLines != null ? maxLines : ROOM_CHAT_DEFAULT_MAX_LINES));
	}

	@GetMapping("/chats/{locationId}/messages")
	@Operation(summary = "Gets the private chat messages backlog")
	@ApiResponse(responseCode = "200", description = "OK")
	@ApiResponse(responseCode = "404", description = "No location found for given id", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public List<ChatBacklogDTO> getChatMessages(@PathVariable @Parameter(description = "The location id") long locationId,
	                                            @RequestParam(value = "maxLines", required = false) @Min(1) @Max(500) Integer maxLines,
	                                            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from)
	{
		var location = locationService.findLocationById(locationId).orElseThrow();
		return toChatBacklogDTOs(chatBacklogService.getMessages(
				location,
				from != null ? from.toInstant(ZoneOffset.UTC) : Instant.now().minus(PRIVATE_CHAT_DEFAULT_DURATION),
				maxLines != null ? maxLines : PRIVATE_CHAT_DEFAULT_MAX_LINES));
	}

	@DeleteMapping("/chats/{locationId}/messages")
	@Operation(summary = "Clears the private chat messages backlog of a given location")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ApiResponse(responseCode = "404", description = "No location found for given id", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public void deleteChatMessages(@PathVariable @Parameter(description = "The location id") long locationId)
	{
		var location = locationService.findLocationById(locationId).orElseThrow();
		chatBacklogService.deleteMessages(location);
	}

	@PostMapping("/distant-chats")
	@Operation(summary = "Creates a distant chat")
	@ApiResponse(responseCode = "200", description = "OK")
	@ApiResponse(responseCode = "404", description = "No identity found for given id", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "Tunnel already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public LocationDTO createDistantChat(@Valid @RequestBody DistantChatRequest distantChatRequest)
	{
		var identity = identityService.findById(distantChatRequest.identityId()).orElseThrow();
		var location = toDTO(chatRsService.createDistantChat(identity));
		if (location == null)
		{
			throw new EntityExistsException("Distant chat already active");
		}
		return location;
	}

	@DeleteMapping("/distant-chats/{identityId}")
	@Operation(summary = "Closes a distant chat")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ApiResponse(responseCode = "404", description = "No identity found for given id", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	void closeDistantChat(@PathVariable long identityId)
	{
		var identity = identityService.findById(identityId).orElseThrow();
		if (!chatRsService.closeDistantChat(identity))
		{
			throw new EntityNotFoundException("No distant chat for identity id " + identityId);
		}
	}

	@GetMapping("/distant-chats/{identityId}/messages")
	@Operation(summary = "Gets the distant chat messages backlog of a given identity")
	@ApiResponse(responseCode = "200", description = "OK")
	@ApiResponse(responseCode = "404", description = "No identity found for given id", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public List<ChatBacklogDTO> getDistantChatMessages(@PathVariable @Parameter(description = "The identity id") long identityId,
	                                                   @RequestParam(value = "maxLines", required = false) @Min(1) @Max(500) Integer maxLines,
	                                                   @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from)
	{
		var identity = identityService.findById(identityId).orElseThrow();
		return fromDistantChatBacklogToChatBacklogDTOs(chatBacklogService.getDistantMessages(
				identity,
				from != null ? from.toInstant(ZoneOffset.UTC) : Instant.now().minus(PRIVATE_CHAT_DEFAULT_DURATION),
				maxLines != null ? maxLines : PRIVATE_CHAT_DEFAULT_MAX_LINES));
	}

	@DeleteMapping("/distant-chats/{identityId}/messages")
	@Operation(summary = "Clears the distant chat messages backlog of a given identity")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ApiResponse(responseCode = "404", description = "No identity found for given id", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public void deleteDistantChatMessages(@PathVariable @Parameter(description = "The identity id") long identityId)
	{
		var identity = identityService.findById(identityId).orElseThrow();
		chatBacklogService.deleteDistantMessages(identity);
	}
}
