/*
 * Copyright (c) 2023-2026 by David Gerber - https://zapek.com
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

public abstract class NotificationService implements SmartLifecycle
{
	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

	final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	private volatile Notification previousNotification;
	private final AtomicBoolean running = new AtomicBoolean();

	private final ApplicationEventPublisher publisher;

	protected NotificationService(ApplicationEventPublisher publisher)
	{
		this.publisher = publisher;
	}

	/// Publishes a notification to the clients. The notification is sent after the current
	/// transaction commits, or immediately if there's no transaction.
	///
	/// Notifications are synchronous events, so the delivery order is preserved within a
	/// transaction.
	///
	/// @param notification the notification to publish
	protected void publish(Notification notification)
	{
		publisher.publishEvent(notification);
	}

	@Override
	public void start()
	{
		running.set(true);
	}

	@Override
	public boolean isRunning()
	{
		return running.get();
	}

	@Override
	public void stop()
	{
		running.set(false);
		emitters.forEach(emitter -> {
			try
			{
				emitter.complete();
			}
			catch (RuntimeException e)
			{
				log.error("Error while stopping emitter: {}", e.getMessage());
			}
		});
		emitters.clear();
	}

	/// Sends that notification to all connecting clients. It's a kind of "sync" notification so that we
	/// get immediate data available. Use it for notifications that report a "state".
	///
	/// @return the initial notification to send
	protected Notification initialNotification()
	{
		return null;
	}

	public SseEmitter addClient()
	{
		if (!running.get())
		{
			throw new ResponseStatusException(SERVICE_UNAVAILABLE);
		}

		var emitter = new SseEmitter(-1L); // no timeout
		addEmitter(emitter);
		emitter.onCompletion(() -> removeEmitter(emitter));
		emitter.onTimeout(() -> removeEmitter(emitter));
		emitter.onError(e -> {
			log.error("Notification emitter error", e);
			removeEmitter(emitter);
		});

		sendInitialNotificationIfNeeded(emitter); // send a notification to the client that just connected to "sync" it

		return emitter;
	}

	protected void sendNotification(Notification notification)
	{
		sendNotification(notification, null);
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
			catch (IOException | RuntimeException _)
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
		catch (IOException | RuntimeException _)
		{
			emitters.remove(emitter);
		}
	}

	private static SseEmitter.SseEventBuilder createEventBuilder(Notification notification)
	{
		var event = SseEmitter.event();
		event.data(notification, MediaType.APPLICATION_JSON);
		return event;
	}
}
