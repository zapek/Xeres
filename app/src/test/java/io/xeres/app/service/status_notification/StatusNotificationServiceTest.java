/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.app.service.status_notification;

import io.xeres.app.api.sse.SsePushNotificationService;
import io.xeres.common.rest.notification.StatusNotificationResponse;
import io.xeres.ui.support.tray.TrayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class StatusNotificationServiceTest
{
	@Mock
	private SsePushNotificationService ssePushNotificationService;

	@Mock
	private TrayService trayService;

	@InjectMocks
	private StatusNotificationService statusNotificationService;

	@Test
	void StatusNotificationService_SendNotification_Once()
	{
		statusNotificationService.setCurrentUsersCount(1); // default is 0

		verify(ssePushNotificationService).sendNotification(argThat(notification -> {
			assertEquals(1, ((StatusNotificationResponse) notification).currentUsers());
			assertNull(((StatusNotificationResponse) notification).totalUsers());
			return true;
		}));
	}

	@Test
	void StatusNotificationService_SendNotification_SetTwiceButOneDiscardedAsSameValue()
	{
		statusNotificationService.setCurrentUsersCount(1); // default is 0
		statusNotificationService.setCurrentUsersCount(1);

		verify(ssePushNotificationService, times(1)).sendNotification(argThat(notification -> {
			assertEquals(1, ((StatusNotificationResponse) notification).currentUsers());
			assertNull(((StatusNotificationResponse) notification).totalUsers());
			return true;
		}));
	}

	@Test
	void StatusNotificationService_AddClient_Sync()
	{
		statusNotificationService.addClient();

		await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> verify(ssePushNotificationService).sendNotification(any(SseEmitter.class),
				argThat(notification -> {
					assertEquals(0, ((StatusNotificationResponse) notification).currentUsers());
					assertEquals(0, ((StatusNotificationResponse) notification).totalUsers());
					return true;
				})));
	}
}
