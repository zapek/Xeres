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

package io.xeres.app.service.notification;

import io.xeres.app.api.sse.SsePushNotificationService;
import io.xeres.common.rest.notification.Notification;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class NotificationService
{
	private final SsePushNotificationService ssePushNotificationService;

	/**
	 * Creates the notification that will be sent by sendNotification().
	 * Return 'null' if there's nothing to send.
	 *
	 * @return the notification or null if there's nothing to send
	 */
	protected abstract Notification createNotification();

	private Notification previousNotification;

	protected NotificationService(SsePushNotificationService ssePushNotificationService)
	{
		this.ssePushNotificationService = ssePushNotificationService;
	}

	public SseEmitter addClient()
	{
		var emitter = new SseEmitter(-1L); // no timeout
		ssePushNotificationService.addEmitter(emitter);
		emitter.onCompletion(() -> ssePushNotificationService.removeEmitter(emitter));
		emitter.onTimeout(() -> ssePushNotificationService.removeEmitter(emitter));

		CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> sendNotification(emitter)); // send a notification to the client that just connected to "sync" it

		return emitter;
	}

	private void sendNotification(SseEmitter specificEmitter)
	{
		Notification notification = createNotification();

		if (notification == null)
		{
			return;
		}

		if (notification.equals(previousNotification))
		{
			return;
		}

		previousNotification = notification;

		if (specificEmitter != null)
		{
			ssePushNotificationService.sendNotification(specificEmitter, notification);
		}
		else
		{
			ssePushNotificationService.sendNotification(notification);
		}
	}

	/**
	 * Send a notification. Optimizations are done automatically to avoid sending duplicates so
	 * there's no need to do it yourself.
	 */
	public void sendNotification()
	{
		sendNotification(null);
	}
}
