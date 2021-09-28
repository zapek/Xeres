/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.web.api.controller.notification;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.web.api.sse.SsePushNotificationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static io.xeres.common.rest.PathConfig.NOTIFICATIONS_PATH;

@Tag(name = "Notification", description = "Out of band notifications", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/notification", description = "Notification documentation"))
@RestController
@RequestMapping(value = NOTIFICATIONS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class NotificationController
{
	private final SsePushNotificationService ssePushNotificationService;

	public NotificationController(SsePushNotificationService ssePushNotificationService)
	{
		this.ssePushNotificationService = ssePushNotificationService;
	}

	@GetMapping
	@Operation(summary = "Subscribe to notifications")
	@ApiResponse(responseCode = "200", description = "Request completed successfully")
	public SseEmitter setupNotification()
	{
		var emitter = new SseEmitter();
		ssePushNotificationService.addEmitter(emitter);
		emitter.onCompletion(() -> ssePushNotificationService.removeEmitter(emitter));
		emitter.onTimeout(() -> ssePushNotificationService.removeEmitter(emitter));

		return emitter;
	}
}
