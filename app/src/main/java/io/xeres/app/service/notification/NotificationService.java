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

import io.xeres.common.rest.notification.Notification;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public abstract class NotificationService
{
	final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	/**
	 * Creates the notification that will be sent by sendNotification().
	 * Return 'null' if there's nothing to send.
	 *
	 * @return the notification or null if there's nothing to send
	 */
	protected abstract Notification createNotification();

	private Notification previousNotification;

	protected NotificationService()
	{
	}

	public SseEmitter addClient()
	{
		var emitter = new SseEmitter(-1L); // no timeout
		addEmitter(emitter);
		emitter.onCompletion(() -> removeEmitter(emitter));
		emitter.onTimeout(() -> removeEmitter(emitter));

		CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> sendNotification(null, emitter)); // send a notification to the client that just connected to "sync" it

		return emitter;
	}

	private void sendNotification(Notification notification, SseEmitter specificEmitter)
	{
		if (notification == null)
		{
			notification = createNotification();
		}

		if (notification == null)
		{
			return;
		}

		if (specificEmitter != null)
		{
			sendSseNotification(specificEmitter, notification);
		}
		else
		{
			if (notification.equals(previousNotification))
			{
				return;
			}

			previousNotification = notification;

			sendSseNotification(notification);
		}
	}

	/**
	 * Send a notification. Optimizations are done automatically to avoid sending duplicates so
	 * there's no need to do it yourself.
	 */
	public void sendNotification()
	{
		sendNotification(null, null);
	}

	public void sendNotification(Notification notification)
	{
		sendNotification(notification, null);
	}

	private void addEmitter(SseEmitter emitter)
	{
		emitters.add(emitter);
	}

	private void removeEmitter(SseEmitter emitter)
	{
		emitters.remove(emitter);
	}

	private void sendSseNotification(Notification notification)
	{
		List<SseEmitter> deadEmitters = new ArrayList<>();

		emitters.forEach(emitter ->
		{
			try
			{
				emitter.send(createEventBuilder(notification));
			}
			catch (IOException e)
			{
				deadEmitters.add(emitter);
			}
		});
		emitters.removeAll(deadEmitters);
	}

	private void sendSseNotification(SseEmitter emitter, Notification notification)
	{
		try
		{
			emitter.send(createEventBuilder(notification));
		}
		catch (IOException e)
		{
			emitters.remove(emitter);
		}
	}

	private SseEmitter.SseEventBuilder createEventBuilder(Notification notification)
	{
		var event = SseEmitter.event();
		event.data(notification);
		event.id(notification.id());
		return event;
	}
}
