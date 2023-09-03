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

package io.xeres.app.database.model.forum;

import io.xeres.app.util.UnHtml;
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

	public static ForumGroupDTO toDTO(ForumGroupItem forumGroupItem)
	{
		if (forumGroupItem == null)
		{
			return null;
		}

		return new ForumGroupDTO(
				forumGroupItem.getId(),
				forumGroupItem.getGxsId(),
				forumGroupItem.getName(),
				forumGroupItem.getDescription(),
				forumGroupItem.isSubscribed(),
				forumGroupItem.isExternal()
		);
	}

	public static List<ForumGroupDTO> toDTOs(List<ForumGroupItem> forumGroupItems)
	{
		return emptyIfNull(forumGroupItems).stream()
				.map(ForumMapper::toDTO)
				.toList();
	}

	public static ForumMessageDTO toDTO(ForumMessageItemSummary forumMessageItemSummary, String authorName, long originalId, long parentId)
	{
		if (forumMessageItemSummary == null)
		{
			return null;
		}

		return new ForumMessageDTO(
				forumMessageItemSummary.getId(),
				forumMessageItemSummary.getGxsId(),
				forumMessageItemSummary.getMessageId(),
				originalId,
				parentId,
				forumMessageItemSummary.getAuthorId(),
				authorName,
				forumMessageItemSummary.getName(),
				forumMessageItemSummary.getPublished(),
				null,
				forumMessageItemSummary.isRead()
		);
	}

	public static List<ForumMessageDTO> toSummaryMessageDTOs(List<ForumMessageItemSummary> forumMessageItemSummaries, Map<GxsId, IdentityGroupItem> authorsMap, Map<MessageId, ForumMessageItem> messagesMap)
	{
		return emptyIfNull(forumMessageItemSummaries).stream()
				.map(forumMessageItemSummary -> toDTO(forumMessageItemSummary,
						authorsMap.getOrDefault(forumMessageItemSummary.getAuthorId(), IdentityGroupItem.EMPTY).getName(),
						messagesMap.getOrDefault(forumMessageItemSummary.getOriginalMessageId(), ForumMessageItem.EMPTY).getId(),
						messagesMap.getOrDefault(forumMessageItemSummary.getParentId(), ForumMessageItem.EMPTY).getId()
				))
				.toList();
	}

	public static ForumMessageDTO toDTO(ForumMessageItem forumMessageItem, String authorName, long originalId, long parentId)
	{
		if (forumMessageItem == null)
		{
			return null;
		}

		return new ForumMessageDTO(
				forumMessageItem.getId(),
				forumMessageItem.getGxsId(),
				forumMessageItem.getMessageId(),
				originalId,
				parentId,
				forumMessageItem.getAuthorId(),
				authorName,
				forumMessageItem.getName(),
				forumMessageItem.getPublished(),
				UnHtml.cleanupMessage(forumMessageItem.getContent()),
				forumMessageItem.isRead()
		);
	}

	public static List<ForumMessageDTO> toForumMessageDTOs(List<ForumMessageItem> forumMessageItems, Map<GxsId, IdentityGroupItem> authorsMap, Map<MessageId, ForumMessageItem> messagesMap)
	{
		return emptyIfNull(forumMessageItems).stream()
				.map(forumMessageItem -> toDTO(forumMessageItem,
						authorsMap.getOrDefault(forumMessageItem.getAuthorId(), IdentityGroupItem.EMPTY).getName(),
						messagesMap.getOrDefault(forumMessageItem.getOriginalMessageId(), ForumMessageItem.EMPTY).getId(),
						messagesMap.getOrDefault(forumMessageItem.getParentId(), ForumMessageItem.EMPTY).getId()
				))
				.toList();
	}
}
