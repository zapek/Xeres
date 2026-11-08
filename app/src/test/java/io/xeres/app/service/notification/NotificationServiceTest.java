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

package io.xeres.app.service.notification;

import io.xeres.common.rest.notification.Notification;
import io.xeres.common.rest.notification.file.FileTrendNotification;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceTest
{
	private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

	@Test
	void AddClient_WhenStopped_Throws()
	{
		var notificationService = new FakeNotificationService(publisher);

		assertThrows(ResponseStatusException.class, notificationService::addClient);
	}

	@Test
	void AddClient_WhenRunning_AddsEmitter()
	{
		var notificationService = new FakeNotificationService(publisher);
		notificationService.start();

		var emitter = notificationService.addClient();

		assertNotNull(emitter);
		assertEquals(1, notificationService.emitters.size());
	}

	@Test
	void Stop_CompletesAndClearsEmitters()
	{
		var notificationService = new FakeNotificationService(publisher);
		notificationService.start();
		notificationService.addClient();

		notificationService.stop();

		assertFalse(notificationService.isRunning());
		assertTrue(notificationService.emitters.isEmpty());
	}

	@Test
	void Stop_WhenCalledTwice_DoesNotThrow()
	{
		var notificationService = new FakeNotificationService(publisher);
		notificationService.start();
		notificationService.addClient();

		notificationService.stop();

		assertDoesNotThrow(() -> notificationService.stop());
	}

	@Test
	void Publish_PublishesEvent()
	{
		var notificationService = new FakeNotificationService(publisher);
		var notification = new FileTrendNotification("sender", "keywords");

		notificationService.publishNotification(notification);

		verify(publisher).publishEvent(notification);
	}

	private static class FakeNotificationService extends NotificationService
	{
		FakeNotificationService(ApplicationEventPublisher publisher)
		{
			super(publisher);
		}

		void publishNotification(Notification notification)
		{
			publish(notification);
		}
	}
}
