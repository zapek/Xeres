/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.xrs.service.forum.ForumRsService;
import io.xeres.common.dto.forum.ForumDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static io.xeres.app.database.model.forum.ForumMapper.toDTOs;
import static io.xeres.common.rest.PathConfig.FORUMS_PATH;

@Tag(name = "Forums", description = "Forums", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/forums", description = "Forums documentation"))
@RestController
@RequestMapping(value = FORUMS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ForumController
{
	private final ForumRsService forumRsService;

	public ForumController(ForumRsService forumRsService)
	{
		this.forumRsService = forumRsService;
	}

	@GetMapping
	@Operation(summary = "Get the list of forums")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public List<ForumDTO> getForums()
	{
		return toDTOs(forumRsService.getForums());
	}

	@PutMapping("/{id}/subscription")
	@ResponseStatus(HttpStatus.OK)
	public long subscribeToForum(@PathVariable long id)
	{
		forumRsService.subscribeToForum(id);
		return id;
	}

	@DeleteMapping("/{id}/subscription")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribeFromForum(@PathVariable long id)
	{
		forumRsService.unsubscribeFromForum(id);
	}
}
