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
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.service.board.BoardRsService;
import io.xeres.common.dto.board.BoardGroupDTO;
import io.xeres.common.rest.board.CreateBoardGroupRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.List;

import static io.xeres.app.database.model.board.BoardMapper.toDTOs;
import static io.xeres.common.rest.PathConfig.BOARDS_PATH;

@Tag(name = "Boards", description = "Boards")
@RestController
@RequestMapping(value = BOARDS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardController
{
	private final BoardRsService boardRsService;
	private final IdentityService identityService;

	public BoardController(BoardRsService boardRsService, IdentityService identityService)
	{
		this.boardRsService = boardRsService;
		this.identityService = identityService;
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
		// XXX: like forums... and how do I supply the image? you can't... upload the image in a separate endpoint, see below. the client has to take care of it
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
		// XXX: add, like identityRsService.saveOwnIdentityImage(id, file);
		// XXX: and call the notifications...

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(BOARDS_PATH + "/groups/{id}/image").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}

}
