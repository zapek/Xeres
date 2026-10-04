/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller.mail;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.database.model.FileMapper;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.service.*;
import io.xeres.app.xrs.common.FileSet;
import io.xeres.common.dto.mail.MailMessageDTO;
import io.xeres.common.mail.MailType;
import io.xeres.common.rest.ScrollRequest;
import io.xeres.common.rest.mail.CreateMailMessageRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Window;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static io.xeres.app.database.model.mail.MailMapper.toDTO;
import static io.xeres.app.database.model.mail.MailMapper.toSummaryMessageDTOs;
import static io.xeres.common.rest.PathConfig.MAIL_PATH;

@Tag(name = "Mail", description = "Mail")
@RestController
@RequestMapping(value = MAIL_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class MailController
{
	private final MailService mailService;
	private final MailMessageService mailMessageService;
	private final IdentityService identityService;
	private final LocationService locationService;
	private final UnHtmlService unHtmlService;

	public MailController(MailService mailService, MailMessageService mailMessageService, IdentityService identityService, LocationService locationService, UnHtmlService unHtmlService)
	{
		this.mailService = mailService;
		this.mailMessageService = mailMessageService;
		this.identityService = identityService;
		this.locationService = locationService;
		this.unHtmlService = unHtmlService;
	}

	@GetMapping("/folders/{mailType}/messages")
	@Operation(summary = "Gets the summary of messages in a folder")
	public Window<MailMessageDTO> getMailMessages(@PathVariable MailType mailType, ScrollRequest scrollRequest)
	{
		var mailMessages = mailService.findAllMailMessagesSummary(mailType, scrollRequest.position(), scrollRequest.sort(), scrollRequest.limit());
		return Window.from(toSummaryMessageDTOs(mailMessages,
						mailMessageService.getLocationsMapFromSummaries(mailMessages),
						mailMessageService.getIdentitiesMapFromSummaries(mailMessages)),
				mailMessages::positionAt,
				mailMessages.hasNext());
	}

	@GetMapping("/messages/{messageId}")
	@Operation(summary = "Gets a message")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public MailMessageDTO getMailMessage(@PathVariable long messageId)
	{
		var mailMessage = mailService.findMessageById(messageId).orElseThrow();

		String fromName;

		if (mailMessage.isDirect())
		{
			var author = locationService.findLocationByLocationIdentifier(mailMessage.getLocationFrom());
			fromName = author.map(Location::getName).orElse(null);
		}
		else
		{
			var author = identityService.findByGxsId(mailMessage.getIdentityFrom());
			fromName = author.map(GxsGroupItem::getName).orElse(null);
		}

		return toDTO(
				unHtmlService,
				mailMessage,
				fromName
		);
	}

	@PostMapping("/messages")
	@Operation(summary = "Creates a mail message")
	@ApiResponse(responseCode = "201", description = "Mail message created successfully", headers = @Header(name = "Message", description = "The location of the created message", schema = @Schema(type = "string")))
	public ResponseEntity<Void> createMailMessage(@Valid @RequestBody CreateMailMessageRequest createMailMessageRequest)
	{
		var ownLocation = locationService.findOwnLocation().orElseThrow();

		var id = mailService.createMailMessage(
				ownLocation.getLocationIdentifier(),
				createMailMessageRequest.locationsTo(),
				createMailMessageRequest.locationsCc(),
				createMailMessageRequest.locationsBcc(),
				createMailMessageRequest.identitiesTo(),
				createMailMessageRequest.identitiesCc(),
				createMailMessageRequest.identitiesBcc(),
				createMailMessageRequest.subject(),
				createMailMessageRequest.content(),
				new FileSet(FileMapper.toFileItems(createMailMessageRequest.files()), null, null)
		);

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(MAIL_PATH + "/messages/{id}").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}
}
