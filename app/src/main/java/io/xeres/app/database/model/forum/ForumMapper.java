/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.database.model.forum;

import io.xeres.app.service.UnHtmlService;
import io.xeres.app.xrs.service.forum.item.ForumGroupItem;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.dto.forum.ForumGroupDTO;
import io.xeres.common.dto.forum.ForumMessageDTO;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;

import java.util.List;
import java.util.Map;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public final class ForumMapper
{
	private ForumMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ForumGroupDTO toDTO(ForumGroupItem item)
	{
		if (item == null)
		{
			return null;
		}

		return new ForumGroupDTO(
				item.getId(),
				item.getGxsId(),
				item.getName(),
				item.getDescription(),
				item.isSubscribed(),
				item.isExternal()
		);
	}

	public static List<ForumGroupDTO> toDTOs(List<ForumGroupItem> items)
	{
		return emptyIfNull(items).stream()
				.map(ForumMapper::toDTO)
				.toList();
	}

	public static ForumMessageDTO toDTO(ForumMessageItemSummary item, String authorName, long originalId, long parentId)
	{
		if (item == null)
		{
			return null;
		}

		return new ForumMessageDTO(
				item.getId(),
				item.getGxsId(),
				item.getMessageId(),
				originalId,
				parentId,
				item.getAuthorId(),
				authorName,
				item.getName(),
				item.getPublished(),
				null,
				item.isRead()
		);
	}

	public static List<ForumMessageDTO> toSummaryMessageDTOs(List<ForumMessageItemSummary> items, Map<GxsId, IdentityGroupItem> authorsMap, Map<MessageId, ForumMessageItem> messagesMap)
	{
		return emptyIfNull(items).stream()
				.map(item -> toDTO(item,
						authorsMap.getOrDefault(item.getAuthorId(), IdentityGroupItem.EMPTY).getName(),
						messagesMap.getOrDefault(item.getOriginalMessageId(), ForumMessageItem.EMPTY).getId(),
						messagesMap.getOrDefault(item.getParentId(), ForumMessageItem.EMPTY).getId()
				))
				.toList();
	}

	public static ForumMessageDTO toDTO(UnHtmlService unHtmlService, ForumMessageItem item, String authorName, long originalId, long parentId, boolean withMessageContent)
	{
		if (item == null)
		{
			return null;
		}

		return new ForumMessageDTO(
				item.getId(),
				item.getGxsId(),
				item.getMessageId(),
				originalId,
				parentId,
				item.getAuthorId(),
				authorName,
				item.getName(),
				item.getPublished(),
				withMessageContent ? unHtmlService.cleanupMessage(item.getContent()) : "",
				item.isRead()
		);
	}

	public static List<ForumMessageDTO> toForumMessageDTOs(UnHtmlService unHtmlService, List<ForumMessageItem> items, Map<GxsId, IdentityGroupItem> authorsMap, Map<MessageId, ForumMessageItem> messagesMap, boolean withMessageContent)
	{
		return emptyIfNull(items).stream()
				.map(item -> toDTO(unHtmlService,
						item,
						authorsMap.getOrDefault(item.getAuthorId(), IdentityGroupItem.EMPTY).getName(),
						messagesMap.getOrDefault(item.getOriginalMessageId(), ForumMessageItem.EMPTY).getId(),
						messagesMap.getOrDefault(item.getParentId(), ForumMessageItem.EMPTY).getId(),
						withMessageContent
				))
				.toList();
	}
}
