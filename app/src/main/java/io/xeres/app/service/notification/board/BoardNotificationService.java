/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.service.notification.board;

import io.xeres.app.service.board.BoardMessageService;
import io.xeres.app.service.notification.NotificationService;
import io.xeres.app.service.unhtml.UnHtmlService;
import io.xeres.app.xrs.service.board.item.BoardGroupItem;
import io.xeres.app.xrs.service.board.item.BoardMessageItem;
import io.xeres.common.rest.notification.board.AddOrUpdateBoardGroups;
import io.xeres.common.rest.notification.board.AddOrUpdateBoardMessages;
import io.xeres.common.rest.notification.board.SetBoardGroupMessagesReadState;
import io.xeres.common.rest.notification.board.SetBoardMessageReadState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

import static io.xeres.app.database.model.board.BoardMapper.toBoardMessageDTOs;
import static io.xeres.app.database.model.board.BoardMapper.toDTOs;

@Service
public class BoardNotificationService extends NotificationService
{
	private final UnHtmlService unHtmlService;
	private final BoardMessageService boardMessageService;

	public BoardNotificationService(ApplicationEventPublisher publisher, UnHtmlService unHtmlService, BoardMessageService boardMessageService)
	{
		super(publisher);
		this.unHtmlService = unHtmlService;
		this.boardMessageService = boardMessageService;
	}

	public void addOrUpdateGroups(List<BoardGroupItem> groups)
	{
		publish(new AddOrUpdateBoardGroups(toDTOs(groups)));
	}

	public void addOrUpdateMessages(List<BoardMessageItem> messages)
	{
		var page = new PageImpl<>(messages);
		publish(new AddOrUpdateBoardMessages(toBoardMessageDTOs(unHtmlService, page,
				boardMessageService.getAuthorsMapFromMessages(page),
				boardMessageService.getMessagesMapFromMessages(messages))));
	}

	public void setMessageReadState(long groupId, long messageId, boolean read)
	{
		publish(new SetBoardMessageReadState(groupId, messageId, read));
	}

	public void setGroupMessagesReadState(long groupId, boolean read)
	{
		publish(new SetBoardGroupMessagesReadState(groupId, read));
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void onGroupsUpdated(AddOrUpdateBoardGroups notification)
	{
		sendNotification(notification);
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void onMessagesUpdated(AddOrUpdateBoardMessages notification)
	{
		sendNotification(notification);
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void onMessageReadStateChanged(SetBoardMessageReadState notification)
	{
		sendNotification(notification);
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void onGroupMessagesReadStateChanged(SetBoardGroupMessagesReadState notification)
	{
		sendNotification(notification);
	}
}
