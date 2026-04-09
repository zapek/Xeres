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

package io.xeres.app.service.notification.forum;

import io.xeres.app.service.ForumMessageService;
import io.xeres.app.service.UnHtmlService;
import io.xeres.app.service.notification.NotificationService;
import io.xeres.app.xrs.service.forum.item.ForumGroupItem;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.common.rest.notification.forum.AddOrUpdateForumGroups;
import io.xeres.common.rest.notification.forum.AddOrUpdateForumMessages;
import io.xeres.common.rest.notification.forum.SetForumGroupMessagesReadState;
import io.xeres.common.rest.notification.forum.SetForumMessagesReadState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static io.xeres.app.database.model.forum.ForumMapper.toDTOs;
import static io.xeres.app.database.model.forum.ForumMapper.toForumMessageDTOs;

@Service
public class ForumNotificationService extends NotificationService
{
	private final ForumMessageService forumMessageService;
	private final UnHtmlService unHtmlService;

	public ForumNotificationService(ForumMessageService forumMessageService, UnHtmlService unHtmlService)
	{
		super();
		this.forumMessageService = forumMessageService;
		this.unHtmlService = unHtmlService;
	}

	public void addOrUpdateGroups(List<ForumGroupItem> groups)
	{
		sendNotification(new AddOrUpdateForumGroups(toDTOs(groups)));
	}

	public void addOrUpdateMessages(List<ForumMessageItem> messages)
	{
		sendNotification(new AddOrUpdateForumMessages(toForumMessageDTOs(unHtmlService, messages,
				forumMessageService.getAuthorsMapFromMessages(messages),
				forumMessageService.getMessagesMapFromMessages(messages),
				false)));
	}

	public void setMessagesReadState(Map<Long, Boolean> messageMap)
	{
		sendNotification(new SetForumMessagesReadState(messageMap));
	}

	public void setGroupMessagesReadState(long groupId, boolean read)
	{
		sendNotification(new SetForumGroupMessagesReadState(groupId, read));
	}
}
