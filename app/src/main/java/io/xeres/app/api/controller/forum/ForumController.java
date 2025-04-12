/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.service.ForumMessageService;
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.service.forum.ForumRsService;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.common.dto.forum.ForumGroupDTO;
import io.xeres.common.dto.forum.ForumMessageDTO;
import io.xeres.common.id.MessageId;
import io.xeres.common.rest.forum.CreateForumGroupRequest;
import io.xeres.common.rest.forum.CreateForumMessageRequest;
import io.xeres.common.rest.forum.UpdateForumMessagesReadRequest;
import jakarta.validation.Valid;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.xeres.app.database.model.forum.ForumMapper.*;
import static io.xeres.common.rest.PathConfig.FORUMS_PATH;

@Tag(name = "Forums", description = "Forums")
@RestController
@RequestMapping(value = FORUMS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ForumController
{
	private final ForumRsService forumRsService;
	private final IdentityService identityService;
	private final ForumMessageService forumMessageService;

	public ForumController(ForumRsService forumRsService, IdentityService identityService, ForumMessageService forumMessageService)
	{
		this.forumRsService = forumRsService;
		this.identityService = identityService;
		this.forumMessageService = forumMessageService;
	}

	@GetMapping("/groups")
	@Operation(summary = "Gets the list of forums")
	public List<ForumGroupDTO> getForumGroups()
	{
		return toDTOs(forumRsService.findAllGroups());
	}

	@PostMapping("/groups")
	@Operation(summary = "Creates a forum")
	@ApiResponse(responseCode = "201", description = "Forum created successfully", headers = @Header(name = "Forum", description = "The location of the created forum", schema = @Schema(type = "string")))
	public ResponseEntity<Void> createForumGroup(@Valid @RequestBody CreateForumGroupRequest createForumGroupRequest)
	{
		var ownIdentity = identityService.getOwnIdentity();
		var id = forumRsService.createForumGroup(ownIdentity.getGxsId(), createForumGroupRequest.name(), createForumGroupRequest.description());

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(FORUMS_PATH + "/groups/{id}").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}

	@GetMapping("/groups/{groupId}")
	@Operation(summary = "Gets the details of a forum")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public ForumGroupDTO getForumGroupById(@PathVariable long groupId)
	{
		return toDTO(forumRsService.findById(groupId).orElseThrow());
	}

	@PutMapping("/groups/{groupId}/subscription")
	@Operation(summary = "Subscribes to a forum")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void subscribeToForumGroup(@PathVariable long groupId)
	{
		forumRsService.subscribeToForumGroup(groupId);
	}

	@DeleteMapping("/groups/{groupId}/subscription")
	@Operation(summary = "Unsubscribes from a forum")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribeFromForumGroup(@PathVariable long groupId)
	{
		forumRsService.unsubscribeFromForumGroup(groupId);
	}

	@GetMapping("/groups/{groupId}/messages")
	@Operation(summary = "Gets the summary of messages in a group")
	public List<ForumMessageDTO> getForumMessages(@PathVariable long groupId)
	{
		var forumMessages = forumRsService.findAllMessagesSummary(groupId);

		return toSummaryMessageDTOs(forumMessages,
				forumMessageService.getAuthorsMapFromSummaries(forumMessages),
				forumMessageService.getMessagesMapFromSummaries(groupId, forumMessages));
	}

	@GetMapping("/messages/{messageId}")
	@Operation(summary = "Gets a message")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public ForumMessageDTO getForumMessage(@PathVariable long messageId)
	{
		var forumMessage = forumRsService.findMessageById(messageId);
		Objects.requireNonNull(forumMessage, "MessageId " + messageId + " not found");

		var author = identityService.findByGxsId(forumMessage.getAuthorId());

		HashSet<MessageId> messageSet = HashSet.newHashSet(2); // they can be null so no Set.of() possible
		CollectionUtils.addIgnoreNull(messageSet, forumMessage.getOriginalMessageId());
		CollectionUtils.addIgnoreNull(messageSet, forumMessage.getParentId());

		var messages = forumRsService.findAllMessages(forumMessage.getGxsId(), messageSet).stream()
				.collect(Collectors.toMap(ForumMessageItem::getMessageId, ForumMessageItem::getId));

		return toDTO(forumMessage,
				author.map(GxsGroupItem::getName).orElse(null),
				messages.getOrDefault(forumMessage.getOriginalMessageId(), 0L),
				messages.getOrDefault(forumMessage.getParentId(), 0L),
				true
		);
	}

	@PostMapping("/messages")
	@Operation(summary = "Creates a forum message")
	@ApiResponse(responseCode = "201", description = "Forum message created successfully", headers = @Header(name = "Message", description = "The location of the created message", schema = @Schema(type = "string")))
	public ResponseEntity<Void> createForumMessage(@Valid @RequestBody CreateForumMessageRequest createMessageRequest)
	{
		var ownIdentity = identityService.getOwnIdentity();
		var id = forumRsService.createForumMessage(
				ownIdentity,
				createMessageRequest.forumId(),
				createMessageRequest.title(),
				createMessageRequest.content(),
				createMessageRequest.parentId(),
				createMessageRequest.originalId()
		);

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(FORUMS_PATH + "/messages/{id}").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}

	@PatchMapping("/messages")
	@Operation(summary = "Modifies forum messages read flag")
	@ResponseStatus(HttpStatus.OK)
	public void updateMessagesReadFlags(@Valid @RequestBody UpdateForumMessagesReadRequest updateForumMessagesReadRequest)
	{
		forumRsService.setForumMessagesAsRead(updateForumMessagesReadRequest.messageMap());
	}
}
