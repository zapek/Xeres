/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.service.notification.file;

import io.xeres.app.service.notification.NotificationService;
import io.xeres.common.id.Id;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.rest.notification.file.FileSearchNotification;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class FileSearchNotificationService extends NotificationService
{
	public FileSearchNotificationService(ApplicationEventPublisher publisher)
	{
		super(publisher);
	}

	public void foundFile(int requestId, String name, long size, Sha1Sum hash)
	{
		publish(new FileSearchNotification(requestId, name, size, Id.toString(hash)));
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void onFileFound(FileSearchNotification notification)
	{
		sendNotification(notification);
	}
}
