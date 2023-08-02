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
import io.xeres.app.database.model.forum.ForumMessageItemSummary;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.xrs.service.forum.ForumRsService;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.common.dto.forum.ForumGroupDTO;
import io.xeres.common.dto.forum.ForumMessageDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.xeres.app.database.model.forum.ForumMapper.*;
import static io.xeres.common.rest.PathConfig.FORUMS_PATH;

@Tag(name = "Forums", description = "Forums", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/forums", description = "Forums documentation"))
@RestController
@RequestMapping(value = FORUMS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ForumController
{
	private final ForumRsService forumRsService;
	private final IdentityRsService identityRsService;

	public ForumController(ForumRsService forumRsService, IdentityRsService identityRsService)
	{
		this.forumRsService = forumRsService;
		this.identityRsService = identityRsService;
	}

	@GetMapping("/groups")
	@Operation(summary = "Get the list of forums")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public List<ForumGroupDTO> getForumGroups()
	{
		return toDTOs(forumRsService.findAllGroups());
	}

	@GetMapping("/groups/{groupId}")
	@Operation(summary = "Get a forum details")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public ForumGroupDTO getForumGroupById(@PathVariable long groupId)
	{
		return toDTO(forumRsService.findById(groupId).orElseThrow());
	}

	@PutMapping("/groups/{groupId}/subscription")
	@ResponseStatus(HttpStatus.OK)
	public long subscribeToForumGroup(@PathVariable long groupId)
	{
		forumRsService.subscribeToForumGroup(groupId);
		return groupId;
	}

	@DeleteMapping("/groups/{groupId}/subscription")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribeFromForumGroup(@PathVariable long groupId)
	{
		forumRsService.unsubscribeFromForumGroup(groupId);
	}

	@GetMapping("/groups/{groupId}/messages")
	@Operation(summary = "Get the summary of messages in a group")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public List<ForumMessageDTO> getForumMessages(@PathVariable long groupId)
	{
		var forumMessages = forumRsService.findAllMessagesSummary(groupId);

		var authors = forumMessages.stream()
				.map(ForumMessageItemSummary::getAuthorId)
				.collect(Collectors.toSet());

		var authorsMap = identityRsService.findAll(authors).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, Function.identity()));

		return toSummaryMessageDTOs(forumMessages, authorsMap);
	}

	@GetMapping("/messages/{messageId}")
	@Operation(summary = "Get a message")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public ForumMessageDTO getForumMessage(@PathVariable long messageId)
	{
		var forumMessage = forumRsService.findMessageById(messageId);
		var author = identityRsService.findByGxsId(forumMessage.getAuthorId());

		return toDTO(forumMessage, author.map(GxsGroupItem::getName).orElse(null));
	}
}
