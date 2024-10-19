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

import io.xeres.app.service.ForumMessageService;
import io.xeres.app.service.notification.NotificationService;
import io.xeres.app.xrs.service.forum.item.ForumGroupItem;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.common.rest.notification.forum.AddForumGroups;
import io.xeres.common.rest.notification.forum.AddForumMessages;
import io.xeres.common.rest.notification.forum.ForumNotification;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.xeres.app.database.model.forum.ForumMapper.toDTOs;
import static io.xeres.app.database.model.forum.ForumMapper.toForumMessageDTOs;

@Service
public class ForumNotificationService extends NotificationService
{
	private final ForumMessageService forumMessageService;

	public ForumNotificationService(ForumMessageService forumMessageService)
	{
		super();
		this.forumMessageService = forumMessageService;
	}

	public void addForumGroups(List<ForumGroupItem> forumGroups)
	{
		var action = new AddForumGroups(toDTOs(forumGroups));
		sendNotification(new ForumNotification(action.getClass().getSimpleName(), action));
	}

	public void addForumMessages(List<ForumMessageItem> forumMessages)
	{
		var action = new AddForumMessages(toForumMessageDTOs(forumMessages,
				forumMessageService.getAuthorsMapFromMessages(forumMessages),
				forumMessageService.getMessagesMapFromMessages(forumMessages),
				false));

		sendNotification(new ForumNotification(action.getClass().getSimpleName(), action));
	}
}
