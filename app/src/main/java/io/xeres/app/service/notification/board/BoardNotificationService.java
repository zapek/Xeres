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

import io.xeres.app.service.BoardMessageService;
import io.xeres.app.service.UnHtmlService;
import io.xeres.app.service.notification.NotificationService;
import io.xeres.app.xrs.service.board.item.BoardGroupItem;
import io.xeres.app.xrs.service.board.item.BoardMessageItem;
import io.xeres.common.rest.notification.board.AddOrUpdateBoardGroups;
import io.xeres.common.rest.notification.board.AddOrUpdateBoardMessages;
import io.xeres.common.rest.notification.board.SetBoardGroupMessagesReadState;
import io.xeres.common.rest.notification.board.SetBoardMessageReadState;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.xeres.app.database.model.board.BoardMapper.toBoardMessageDTOs;
import static io.xeres.app.database.model.board.BoardMapper.toDTOs;

@Service
public class BoardNotificationService extends NotificationService
{
	private final UnHtmlService unHtmlService;
	private final BoardMessageService boardMessageService;

	public BoardNotificationService(UnHtmlService unHtmlService, BoardMessageService boardMessageService)
	{
		this.unHtmlService = unHtmlService;
		this.boardMessageService = boardMessageService;
	}

	public void addOrUpdateGroups(List<BoardGroupItem> groups)
	{
		sendNotification(new AddOrUpdateBoardGroups(toDTOs(groups)));
	}

	public void addOrUpdateMessages(List<BoardMessageItem> messages)
	{
		var page = new PageImpl<>(messages);
		sendNotification(new AddOrUpdateBoardMessages(toBoardMessageDTOs(unHtmlService, page,
				boardMessageService.getAuthorsMapFromMessages(page),
				boardMessageService.getMessagesMapFromMessages(messages))));
	}

	public void setMessageReadState(long groupId, long messageId, boolean read)
	{
		sendNotification(new SetBoardMessageReadState(groupId, messageId, read));
	}

	public void setGroupMessagesReadState(long groupId, boolean read)
	{
		sendNotification(new SetBoardGroupMessagesReadState(groupId, read));
	}
}
