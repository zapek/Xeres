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

package io.xeres.app.database.model.board;

import io.xeres.app.service.UnHtmlService;
import io.xeres.app.xrs.service.board.item.BoardGroupItem;
import io.xeres.app.xrs.service.board.item.BoardMessageItem;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.dto.board.BoardGroupDTO;
import io.xeres.common.dto.board.BoardMessageDTO;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public final class BoardMapper
{
	private BoardMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static BoardGroupDTO toDTO(BoardGroupItem item)
	{
		if (item == null)
		{
			return null;
		}

		return new BoardGroupDTO(
				item.getId(),
				item.getGxsId(),
				item.getName(),
				item.getDescription(),
				item.hasImage(),
				item.isSubscribed(),
				item.isExternal()
		);
	}

	public static List<BoardGroupDTO> toDTOs(List<BoardGroupItem> items)
	{
		return emptyIfNull(items).stream()
				.map(BoardMapper::toDTO)
				.toList();
	}

	public static BoardMessageDTO toDTO(UnHtmlService unHtmlService, BoardMessageItem item, String authorName, long originalId, long parentId)
	{
		if (item == null)
		{
			return null;
		}

		return new BoardMessageDTO(
				item.getId(),
				item.getGxsId(),
				item.getMessageId(),
				originalId,
				parentId,
				item.getAuthorId(),
				authorName,
				item.getName(),
				item.getPublished(),
				item.getLink(),
				unHtmlService.cleanupMessage(item.getContent()),
				item.hasImage(),
				item.getImageWidth(),
				item.getImageHeight(),
				item.isRead()
		);
	}

	public static List<BoardMessageDTO> toBoardMessageDTOs(UnHtmlService unHtmlService, Page<BoardMessageItem> items, Map<GxsId, IdentityGroupItem> authorsMap, Map<MessageId, BoardMessageItem> messagesMap)
	{
		return items.stream()
				.map(item -> toDTO(unHtmlService,
						item,
						authorsMap.getOrDefault(item.getAuthorId(), IdentityGroupItem.EMPTY).getName(),
						messagesMap.getOrDefault(item.getOriginalMessageId(), BoardMessageItem.EMPTY).getId(),
						messagesMap.getOrDefault(item.getParentId(), BoardMessageItem.EMPTY).getId()
				))
				.toList();
	}
}
