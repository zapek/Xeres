/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.service.chat.ChatRsService;
import io.xeres.app.xrs.service.chat.RoomFlags;
import io.xeres.common.dto.chat.ChatRoomContextDTO;
import io.xeres.common.dto.chat.ChatRoomVisibility;
import io.xeres.common.rest.chat.CreateChatRoomRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.util.EnumSet;

import static io.xeres.app.database.model.chat.ChatMapper.toDTO;
import static io.xeres.common.rest.PathConfig.CHAT_PATH;

@Tag(name = "Chat", description = "Chat service", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/chat", description = "Chat documentation"))
@RestController
@RequestMapping(value = CHAT_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ChatController
{
	private final ChatRsService chatRsService;

	public ChatController(ChatRsService chatRsService)
	{
		this.chatRsService = chatRsService;
	}

	@PostMapping("/rooms")
	@Operation(summary = "Create a chat room")
	@ApiResponse(responseCode = "201", description = "Room created successfully", headers = @Header(name = "Room", description = "The location of the created room", schema = @Schema(type = "string")))
	public ResponseEntity<Void> createChatRoom(@Valid @RequestBody CreateChatRoomRequest createChatRoomRequest)
	{
		var id = chatRsService.createChatRoom(createChatRoomRequest.name(),
				createChatRoomRequest.topic(),
				null,
				createChatRoomRequest.visibility() == ChatRoomVisibility.PUBLIC ? EnumSet.of(RoomFlags.PUBLIC) : EnumSet.noneOf(RoomFlags.class),
				createChatRoomRequest.signedIdentities());

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath("/rooms/{id}").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}

	@PutMapping("/rooms/{id}/subscription")
	@ResponseStatus(HttpStatus.OK)
	public long subscribeToChatRoom(@PathVariable long id)
	{
		chatRsService.joinChatRoom(id); // XXX: error if we're already subscribed
		return id;
	}

	@DeleteMapping("/rooms/{id}/subscription")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribeFromChatRoom(@PathVariable long id)
	{
		chatRsService.leaveChatRoom(id); // XXX: error if we're not subscribed
	}

	@GetMapping("/rooms")
	@Operation(summary = "Get a chat room context (all rooms, status, current nickname, etc...)")
	public ChatRoomContextDTO getChatRoomContext()
	{
		return toDTO(chatRsService.getChatRoomContext());
	}
}
