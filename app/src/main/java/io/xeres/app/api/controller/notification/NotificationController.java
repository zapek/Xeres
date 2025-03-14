/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller.notification;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.service.notification.availability.AvailabilityNotificationService;
import io.xeres.app.service.notification.contact.ContactNotificationService;
import io.xeres.app.service.notification.file.FileNotificationService;
import io.xeres.app.service.notification.file.FileSearchNotificationService;
import io.xeres.app.service.notification.file.FileTrendNotificationService;
import io.xeres.app.service.notification.forum.ForumNotificationService;
import io.xeres.app.service.notification.status.StatusNotificationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static io.xeres.common.rest.PathConfig.NOTIFICATIONS_PATH;

@Tag(name = "Notification", description = "Out of band notifications", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/notification", description = "Notification documentation"))
@RestController
@RequestMapping(value = NOTIFICATIONS_PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public class NotificationController
{
	private final StatusNotificationService statusNotificationService;
	private final ForumNotificationService forumNotificationService;
	private final FileNotificationService fileNotificationService;
	private final FileSearchNotificationService fileSearchNotificationService;
	private final FileTrendNotificationService fileTrendNotificationService;
	private final ContactNotificationService contactNotificationService;
	private final AvailabilityNotificationService availabilityNotificationService;

	public NotificationController(StatusNotificationService statusNotificationService, ForumNotificationService forumNotificationService, FileNotificationService fileNotificationService, FileSearchNotificationService fileSearchNotificationService, FileTrendNotificationService fileTrendNotificationService, ContactNotificationService contactNotificationService, AvailabilityNotificationService availabilityNotificationService)
	{
		this.statusNotificationService = statusNotificationService;
		this.forumNotificationService = forumNotificationService;
		this.fileNotificationService = fileNotificationService;
		this.fileSearchNotificationService = fileSearchNotificationService;
		this.fileTrendNotificationService = fileTrendNotificationService;
		this.contactNotificationService = contactNotificationService;
		this.availabilityNotificationService = availabilityNotificationService;
	}

	@GetMapping("/status")
	@Operation(summary = "Subscribe to status notifications")
	@ApiResponse(responseCode = "200", description = "Request completed successfully")
	public SseEmitter setupStatusNotification()
	{
		return statusNotificationService.addClient();
	}

	@GetMapping("/forum")
	@Operation(summary = "Subscribe to forum notifications")
	@ApiResponse(responseCode = "200", description = "Request completed successfully")
	public SseEmitter setupForumNotification()
	{
		return forumNotificationService.addClient();
	}

	@GetMapping("/file")
	@Operation(summary = "Subscribe to file notifications")
	@ApiResponse(responseCode = "200", description = "Request completed successfully")
	public SseEmitter setupFileNotification()
	{
		return fileNotificationService.addClient();
	}

	@GetMapping("/file-search")
	@Operation(summary = "Subscribe to file search notifications")
	@ApiResponse(responseCode = "200", description = "Request completed successfully")
	public SseEmitter setupFileSearchNotification()
	{
		return fileSearchNotificationService.addClient();
	}

	@GetMapping("/file-trend")
	@Operation(summary = "Subscribe to file trend notifications")
	@ApiResponse(responseCode = "200", description = "Request completed successfully")
	public SseEmitter setupFileTrendNotification()
	{
		return fileTrendNotificationService.addClient();
	}

	@GetMapping("/contact")
	@Operation(summary = "Subscribe to contact notifications")
	@ApiResponse(responseCode = "200", description = "Request completed successfully")
	public SseEmitter setupContactNotification()
	{
		return contactNotificationService.addClient();
	}

	@GetMapping("/availability")
	@Operation(summary = "Subscribe to connection notifications")
	@ApiResponse(responseCode = "200", description = "Request completed successfully")
	public SseEmitter setupConnectionNotification()
	{
		return availabilityNotificationService.addClient();
	}
}
