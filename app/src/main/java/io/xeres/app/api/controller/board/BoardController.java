/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.service.BoardMessageService;
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.service.board.BoardRsService;
import io.xeres.common.dto.board.BoardGroupDTO;
import io.xeres.common.dto.board.BoardMessageDTO;
import io.xeres.common.rest.board.CreateBoardGroupRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.List;

import static io.xeres.app.database.model.board.BoardMapper.*;
import static io.xeres.common.rest.PathConfig.BOARDS_PATH;

@Tag(name = "Boards", description = "Boards")
@RestController
@RequestMapping(value = BOARDS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardController
{
	private final BoardRsService boardRsService;
	private final IdentityService identityService;
	private final BoardMessageService boardMessageService;

	public BoardController(BoardRsService boardRsService, IdentityService identityService, BoardMessageService boardMessageService)
	{
		this.boardRsService = boardRsService;
		this.identityService = identityService;
		this.boardMessageService = boardMessageService;
	}

	@GetMapping("/groups")
	@Operation(summary = "Gets the list of boards")
	public List<BoardGroupDTO> getBoardGroups()
	{
		return toDTOs(boardRsService.findAllGroups());
	}

	@PostMapping("/groups")
	@Operation(summary = "Creates a board")
	@ApiResponse(responseCode = "201", description = "Board created successfully", headers = @Header(name = "Board", description = "The location of the created board", schema = @Schema(type = "string")))
	public ResponseEntity<Void> createBoardGroup(@Valid @RequestBody CreateBoardGroupRequest createBoardGroupRequest)
	{
		var ownIdentity = identityService.getOwnIdentity();
		var id = boardRsService.createBoardGroup(ownIdentity.getGxsId(), createBoardGroupRequest.name(), createBoardGroupRequest.description());

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(BOARDS_PATH + "/groups/{id}").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}

	@PostMapping(value = "/groups/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Add/Change a board's image")
	@ApiResponse(responseCode = "201", description = "Board's image created")
	@ApiResponse(responseCode = "404", description = "Board not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "415", description = "Image's media type unsupported", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "422", description = "Image unprocessable", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public ResponseEntity<Void> uploadBoardGroupImage(@PathVariable long id, @RequestBody MultipartFile file) throws IOException
	{
		boardRsService.saveBoardGroupImage(id, file);

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(BOARDS_PATH + "/groups/{id}/image").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}

	@GetMapping("/groups/{groupId}")
	@Operation(summary = "Gets the details of a board")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public BoardGroupDTO getBoardGroupById(@PathVariable long groupId)
	{
		return toDTO(boardRsService.findById(groupId).orElseThrow());
	}

	@GetMapping("/groups/{groupId}/unread-count")
	@Operation(summary = "Get the unread count of a board")
	public int getBoardUnreadCount(@PathVariable long groupId)
	{
		return boardRsService.getUnreadCount(groupId);
	}

	@PutMapping("/groups/{groupId}/subscription")
	@Operation(summary = "Subscribes to a board")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void subscribeToBoardGroup(@PathVariable long groupId)
	{
		boardRsService.subscribeToBoardGroup(groupId);
	}

	@DeleteMapping("/groups/{groupId}/subscription")
	@Operation(summary = "Unsubscribes from a board")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribeFromBoardGroup(@PathVariable long groupId)
	{
		boardRsService.unsubscribeFromBoardGroup(groupId);
	}

	@GetMapping("/groups/{groupId}/messages")
	@Operation(summary = "Gets the summary of messages in a group")
	public List<BoardMessageDTO> getForumMessages(@PathVariable long groupId)
	{
		var boardMessages = boardRsService.findAllMessages(groupId);

		return toSummaryMessageDTOs(boardMessages,
				boardMessageService.getAuthorsMapFromMessages(boardMessages),
				boardMessageService.getMessagesMapFromSummaries(groupId, boardMessages));
	}
}
