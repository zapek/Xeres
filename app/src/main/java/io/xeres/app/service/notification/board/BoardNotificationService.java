/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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
import io.xeres.common.rest.notification.board.BoardNotification;
import io.xeres.common.rest.notification.board.MarkBoardMessagesAsRead;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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

	public void addOrUpdateBoardGroups(List<BoardGroupItem> boardGroups)
	{
		var action = new AddOrUpdateBoardGroups(toDTOs(boardGroups));
		sendNotification(new BoardNotification(action.getClass().getSimpleName(), action));
	}

	public void addOrUpdateBoardMessages(List<BoardMessageItem> boardMessages)
	{
		var page = new PageImpl<>(boardMessages);
		var action = new AddOrUpdateBoardMessages(toBoardMessageDTOs(unHtmlService, page,
				boardMessageService.getAuthorsMapFromMessages(page),
				boardMessageService.getMessagesMapFromMessages(boardMessages)));

		sendNotification(new BoardNotification(action.getClass().getSimpleName(), action));
	}

	public void markBoardMessagesAsRead(Map<Long, Boolean> messageMap)
	{
		var action = new MarkBoardMessagesAsRead(messageMap);
		sendNotification(new BoardNotification(action.getClass().getSimpleName(), action));
	}
}
