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

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.service.notification.file.FileNotificationService;
import io.xeres.app.service.notification.file.FileSearchNotificationService;
import io.xeres.app.service.notification.forum.ForumNotificationService;
import io.xeres.app.service.notification.status.StatusNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static io.xeres.common.rest.PathConfig.NOTIFICATIONS_PATH;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = NOTIFICATIONS_PATH;

	@MockBean
	private StatusNotificationService statusNotificationService;

	@MockBean
	private ForumNotificationService forumNotificationService;

	@MockBean
	private FileNotificationService fileNotificationService;

	@MockBean
	private FileSearchNotificationService fileSearchNotificationService;

	@Autowired
	public MockMvc mvc;

	@Test
	void NotificationController_SetupStatusNotification_OK() throws Exception
	{
		var sseEmitter = new SseEmitter();

		when(statusNotificationService.addClient()).thenReturn(sseEmitter);

		mvc.perform(getJson(BASE_URL + "/status", MediaType.TEXT_EVENT_STREAM))
				.andExpect(status().isOk());
	}

	@Test
	void NotificationController_SetupForumNotification_OK() throws Exception
	{
		var sseEmitter = new SseEmitter();

		when(forumNotificationService.addClient()).thenReturn(sseEmitter);

		mvc.perform(getJson(BASE_URL + "/forum", MediaType.TEXT_EVENT_STREAM))
				.andExpect(status().isOk());
	}

	@Test
	void NotificationController_SetupFileNotification_OK() throws Exception
	{
		var sseEmitter = new SseEmitter();

		when(fileNotificationService.addClient()).thenReturn(sseEmitter);

		mvc.perform(getJson(BASE_URL + "/file", MediaType.TEXT_EVENT_STREAM))
				.andExpect(status().isOk());
	}
}
