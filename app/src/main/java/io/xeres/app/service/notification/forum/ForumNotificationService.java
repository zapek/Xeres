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

package io.xeres.app.service.notification.forum;

import io.xeres.app.service.notification.NotificationService;
import io.xeres.app.xrs.service.forum.item.ForumGroupItem;
import io.xeres.common.rest.notification.Notification;
import io.xeres.common.rest.notification.forum.AddForums;
import io.xeres.common.rest.notification.forum.ForumNotification;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.xeres.app.database.model.forum.ForumMapper.toDTOs;

@Service
public class ForumNotificationService extends NotificationService
{
	public ForumNotificationService()
	{
		super();
	}

	public void addForums(List<ForumGroupItem> forums)
	{
		var action = new AddForums(toDTOs(forums));
		sendNotification(new ForumNotification(action.getClass().getSimpleName(), action));
	}

	@Override
	protected Notification createNotification()
	{
		return null;
	}
}
