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
import io.xeres.common.rest.notification.file.FileTrendNotification;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class FileTrendNotificationService extends NotificationService
{
	public FileTrendNotificationService(ApplicationEventPublisher publisher)
	{
		super(publisher);
	}

	public void receivedSearch(String senderName, String keywords)
	{
		publish(new FileTrendNotification(senderName, keywords));
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void onSearchReceived(FileTrendNotification notification)
	{
		sendNotification(notification);
	}
}
