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

package io.xeres.app.api.sse;

import io.xeres.common.rest.notification.NotificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SsePushNotificationService
{
	final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	public void addEmitter(SseEmitter emitter)
	{
		emitters.add(emitter);
	}

	public void removeEmitter(SseEmitter emitter)
	{
		emitters.remove(emitter);
	}

	public void sendNotification(NotificationResponse notificationResponse)
	{
		List<SseEmitter> deadEmitters = new ArrayList<>();

		emitters.forEach(emitter ->
		{
			try
			{
				emitter.send(SseEmitter.event().data(notificationResponse)); // XXX: there are other options... see send()<tab>
			}
			catch (IOException e)
			{
				deadEmitters.add(emitter);
			}
		});
		emitters.removeAll(deadEmitters);
	}
}
