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

package io.xeres.app.api.controller.channel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.service.ChannelMessageService;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.UnHtmlService;
import io.xeres.app.xrs.service.channel.ChannelRsService;
import io.xeres.app.xrs.service.channel.item.ChannelMessageItem;
import io.xeres.common.dto.channel.ChannelGroupDTO;
import io.xeres.common.dto.channel.ChannelMessageDTO;
import io.xeres.common.id.MessageId;
import io.xeres.common.rest.channel.CreateChannelGroupRequest;
import io.xeres.common.rest.channel.CreateChannelMessageRequest;
import io.xeres.common.rest.channel.UpdateChannelMessagesReadRequest;
import io.xeres.common.util.image.ImageUtils;
import jakarta.validation.Valid;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.xeres.app.database.model.channel.ChannelMapper.*;
import static io.xeres.common.rest.PathConfig.CHANNELS_PATH;

@Tag(name = "Channels", description = "Channels")
@RestController
@RequestMapping(value = CHANNELS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ChannelController
{
	private final ChannelRsService channelRsService;
	private final IdentityService identityService;
	private final ChannelMessageService channelMessageService;
	private final UnHtmlService unHtmlService;

	public ChannelController(ChannelRsService channelRsService, IdentityService identityService, ChannelMessageService channelMessageService, UnHtmlService unHtmlService)
	{
		this.channelRsService = channelRsService;
		this.identityService = identityService;
		this.channelMessageService = channelMessageService;
		this.unHtmlService = unHtmlService;
	}

	@GetMapping("/groups")
	@Operation(summary = "Gets the list of channels")
	public List<ChannelGroupDTO> getChannelGroups()
	{
		return toDTOs(channelRsService.findAllGroups());
	}

	@PostMapping("/groups")
	@Operation(summary = "Creates a channel")
	@ApiResponse(responseCode = "201", description = "Channel created successfully", headers = @Header(name = "Channel", description = "The location of the created channel", schema = @Schema(type = "string")))
	public ResponseEntity<Void> createChannelGroup(@Valid @RequestBody CreateChannelGroupRequest createChannelGroupRequest)
	{
		var ownIdentity = identityService.getOwnIdentity();
		var id = channelRsService.createChannelGroup(ownIdentity.getGxsId(), createChannelGroupRequest.name(), createChannelGroupRequest.description());

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(CHANNELS_PATH + "/groups/{id}").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}

	@PostMapping(value = "/groups/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Add/Change a channel's image")
	@ApiResponse(responseCode = "201", description = "Channel's image created")
	@ApiResponse(responseCode = "404", description = "Channel not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "415", description = "Image's media type unsupported", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "422", description = "Image unprocessable", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public ResponseEntity<Void> uploadChannelGroupImage(@PathVariable long id, @RequestBody MultipartFile file) throws IOException
	{
		channelRsService.saveChannelGroupImage(id, file);

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(CHANNELS_PATH + "/groups/{id}/image").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}

	@GetMapping(value = "/groups/{id}/image", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
	@Operation(summary = "Returns a channel's image")
	@ApiResponse(responseCode = "200", description = "Channel's image found")
	@ApiResponse(responseCode = "204", description = "Channel's image is empty")
	@ApiResponse(responseCode = "404", description = "Board not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public ResponseEntity<InputStreamResource> downloadChannelGroupImage(@PathVariable long id)
	{
		var group = channelRsService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); // Bypass the global controller advice because it only knows about application/json mimetype
		var imageType = ImageUtils.getImageMimeType(group.getImage());
		if (imageType == null)
		{
			// XXX: do we want an identicon?!
			return null; // XXX
		}
		return ResponseEntity.ok()
				.contentLength(group.getImage().length)
				.contentType(imageType)
				.body(new InputStreamResource(new ByteArrayInputStream(group.getImage())));
	}

	@GetMapping("/groups/{groupId}")
	@Operation(summary = "Gets the details of a channel")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public ChannelGroupDTO getChannelGroupById(@PathVariable long groupId)
	{
		return toDTO(channelRsService.findById(groupId).orElseThrow());
	}

	@GetMapping("/groups/{groupId}/unread-count")
	@Operation(summary = "Get the unread count of a channel")
	public int getChannelUnreadCount(@PathVariable long groupId)
	{
		return channelRsService.getUnreadCount(groupId);
	}

	@PutMapping("/groups/{groupId}/subscription")
	@Operation(summary = "Subscribes to a channel")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void subscribeToChannelGroup(@PathVariable long groupId)
	{
		channelRsService.subscribeToChannelGroup(groupId);
	}

	@DeleteMapping("/groups/{groupId}/subscription")
	@Operation(summary = "Unsubscribes from a channel")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribeFromChannelGroup(@PathVariable long groupId)
	{
		channelRsService.unsubscribeFromChannelGroup(groupId);
	}

	@GetMapping("/groups/{groupId}/messages")
	@Operation(summary = "Gets the summary of messages in a group")
	public List<ChannelMessageDTO> getChannelMessages(@PathVariable long groupId)
	{
		var channelMessages = channelRsService.findAllMessages(groupId);

		return toSummaryMessageDTOs(channelMessages,
				channelMessageService.getAuthorsMapFromMessages(channelMessages),
				channelMessageService.getMessagesMapFromSummaries(groupId, channelMessages));
	}

	@GetMapping("/messages/{messageId}")
	@Operation(summary = "Gets a message")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public ChannelMessageDTO getChannelMessage(@PathVariable long messageId)
	{
		var channelMessage = channelRsService.findMessageById(messageId);
		Objects.requireNonNull(channelMessage, "MessageId " + messageId + " not found");

		var author = identityService.findByGxsId(channelMessage.getAuthorId());

		HashSet<MessageId> messageSet = HashSet.newHashSet(2); // they can be null so no Set.of() possible
		CollectionUtils.addIgnoreNull(messageSet, channelMessage.getOriginalMessageId());
		CollectionUtils.addIgnoreNull(messageSet, channelMessage.getParentId());

		var messages = channelRsService.findAllMessages(channelMessage.getGxsId(), messageSet).stream()
				.collect(Collectors.toMap(ChannelMessageItem::getMessageId, ChannelMessageItem::getId));

		return toDTO(
				unHtmlService,
				channelMessage,
				author.map(GxsGroupItem::getName).orElse(null),
				messages.getOrDefault(channelMessage.getOriginalMessageId(), 0L),
				messages.getOrDefault(channelMessage.getParentId(), 0L),
				true
		);
	}

	@PostMapping("/messages")
	@Operation(summary = "Creates a channel message")
	@ApiResponse(responseCode = "201", description = "Channel message created successfully", headers = @Header(name = "Message", description = "The location of the created message", schema = @Schema(type = "string")))
	public ResponseEntity<Void> createChannelMessage(@Valid @RequestBody CreateChannelMessageRequest createMessageRequest)
	{
		var ownIdentity = identityService.getOwnIdentity();
		var id = channelRsService.createChannelMessage(
				ownIdentity,
				createMessageRequest.channelId(),
				createMessageRequest.title(),
				createMessageRequest.content(),
				createMessageRequest.parentId(),
				createMessageRequest.originalId()
		);

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(CHANNELS_PATH + "/messages/{id}").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}

	// XXX: endpoint to add files (all at the same time? one by one?)

	@PostMapping(value = "/messages/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Add/Change a channel message's image")
	@ApiResponse(responseCode = "201", description = "Channel message image created")
	@ApiResponse(responseCode = "404", description = "Channel message not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "415", description = "Image's media type unsupported", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "422", description = "Image unprocessable", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public ResponseEntity<Void> uploadChannelMessageGroupImage(@PathVariable long id, @RequestBody MultipartFile file) throws IOException
	{
		channelRsService.saveChannelMessageImage(id, file);

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(CHANNELS_PATH + "/messages/{id}/image").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}

	@PatchMapping("/messages")
	@Operation(summary = "Modifies channel messages read flag")
	@ResponseStatus(HttpStatus.OK)
	public void updateMessagesReadFlags(@Valid @RequestBody UpdateChannelMessagesReadRequest updateMessagesReadRequest)
	{
		channelRsService.setChannelMessagesAsRead(updateMessagesReadRequest.messageMap());
	}
}
