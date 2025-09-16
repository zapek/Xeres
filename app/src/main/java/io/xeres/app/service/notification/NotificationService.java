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

package io.xeres.app.service.notification;

import io.xeres.common.rest.notification.Notification;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class NotificationService
{
	final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	private Notification previousNotification;
	private final AtomicBoolean running = new AtomicBoolean();

	protected NotificationService()
	{
		running.lazySet(true);
	}

	/**
	 * Sends that notification to all connecting clients. It's a kind of "sync" notification so that we
	 * get immediate data available. Use it for notifications that report a "state".
	 *
	 * @return the initial notification to send
	 */
	protected Notification initialNotification()
	{
		return null;
	}

	public SseEmitter addClient()
	{
		if (!running.get())
		{
			return null;
		}

		var emitter = new SseEmitter(-1L); // no timeout
		addEmitter(emitter);
		emitter.onCompletion(() -> removeEmitter(emitter));
		emitter.onTimeout(() -> removeEmitter(emitter));

		CompletableFuture.delayedExecutor(1, TimeUnit.MILLISECONDS).execute(() -> sendInitialNotificationIfNeeded(emitter)); // send a notification to the client that just connected to "sync" it (XXX: remove? what happens if the event is sent immediately? test it... I don't like that delay stuff...)

		return emitter;
	}

	public void sendNotification(Notification notification)
	{
		sendNotification(notification, null);
	}

	/**
	 * Closes all the emitters. If not called, tomcat will complain about non-closed connections
	 * on shutdown.
	 */
	public void shutdown()
	{
		running.set(false);
		emitters.forEach(ResponseBodyEmitter::complete);
	}

	private void sendInitialNotificationIfNeeded(SseEmitter emitter)
	{
		var notification = initialNotification();
		if (notification != null)
		{
			sendNotification(notification, emitter);
		}
	}

	private void sendNotification(Notification notification, SseEmitter specificEmitter)
	{
		Objects.requireNonNull(notification);

		if (!running.get())
		{
			return;
		}

		if (specificEmitter != null)
		{
			sendSseNotification(specificEmitter, notification);
		}
		else
		{
			if (notification.ignoreDuplicates() && notification.equals(previousNotification))
			{
				return;
			}

			previousNotification = notification;

			sendSseNotification(notification);
		}
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
			catch (IOException _)
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
		catch (IOException _)
		{
			emitters.remove(emitter);
		}
	}

	private static SseEmitter.SseEventBuilder createEventBuilder(Notification notification)
	{
		var event = SseEmitter.event();
		event.data(notification); // There's no way to serialize subclasses or classes implementing an interface without tricking with the id and doing it manually
		event.id(notification.id());
		return event;
	}
}
