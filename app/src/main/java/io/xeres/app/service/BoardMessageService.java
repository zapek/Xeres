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

package io.xeres.app.service;

import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.xrs.service.board.BoardRsService;
import io.xeres.app.xrs.service.board.item.BoardMessageItem;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.apache.commons.collections4.SetUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BoardMessageService
{
	private final BoardRsService boardRsService;
	private final IdentityService identityService;

	public BoardMessageService(@Lazy BoardRsService boardRsService, IdentityService identityService)
	{
		this.boardRsService = boardRsService;
		this.identityService = identityService;
	}

	public Map<GxsId, IdentityGroupItem> getAuthorsMapFromMessages(Page<BoardMessageItem> boardMessages)
	{
		var authors = boardMessages.stream()
				.map(BoardMessageItem::getAuthorId)
				.collect(Collectors.toSet());

		return identityService.findAll(authors).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, Function.identity()));
	}

	public Map<MessageId, BoardMessageItem> getMessagesMapFromSummaries(long groupId, Page<BoardMessageItem> boardMessages)
	{
		var messageIds = boardMessages.stream()
				.map(BoardMessageItem::getMessageId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		var parentIds = boardMessages.stream()
				.map(BoardMessageItem::getParentId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		return boardRsService.findAllMessages(groupId, SetUtils.union(messageIds, parentIds)).stream()
				.collect(Collectors.toMap(BoardMessageItem::getMessageId, Function.identity()));
	}

	public Map<MessageId, BoardMessageItem> getMessagesMapFromMessages(List<BoardMessageItem> boardMessages)
	{
		var messageIds = boardMessages.stream()
				.map(BoardMessageItem::getMessageId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		var parentIds = boardMessages.stream()
				.map(BoardMessageItem::getParentId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		return boardRsService.findAllMessages(SetUtils.union(messageIds, parentIds)).stream()
				.collect(Collectors.toMap(BoardMessageItem::getMessageId, Function.identity()));
	}
}
