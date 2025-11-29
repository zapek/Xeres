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
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.notification.channel.ChannelNotificationService;
import io.xeres.app.xrs.service.channel.ChannelRsService;
import io.xeres.common.dto.channel.ChannelGroupDTO;
import io.xeres.common.rest.channel.CreateChannelGroupRequest;
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

import static io.xeres.app.database.model.channel.ChannelMapper.toDTO;
import static io.xeres.app.database.model.channel.ChannelMapper.toDTOs;
import static io.xeres.common.rest.PathConfig.CHANNELS_PATH;

@Tag(name = "Channels", description = "Channels")
@RestController
@RequestMapping(value = CHANNELS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ChannelController
{
	private final ChannelRsService channelRsService;
	private final IdentityService identityService;
	private final ChannelNotificationService channelNotificationService;

	public ChannelController(ChannelRsService channelRsService, IdentityService identityService, ChannelNotificationService channelNotificationService)
	{
		this.channelRsService = channelRsService;
		this.identityService = identityService;
		this.channelNotificationService = channelNotificationService;
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
		var channel = channelRsService.saveChannelGroupImage(id, file);
		channelNotificationService.addOrUpdateChannelGroups(List.of(channel));

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(CHANNELS_PATH + "/groups/{id}/image").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
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
}
