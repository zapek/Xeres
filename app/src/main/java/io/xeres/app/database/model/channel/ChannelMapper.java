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

package io.xeres.app.database.model.channel;

import io.xeres.app.service.UnHtmlService;
import io.xeres.app.xrs.common.FileItem;
import io.xeres.app.xrs.service.channel.item.ChannelGroupItem;
import io.xeres.app.xrs.service.channel.item.ChannelMessageItem;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.dto.channel.ChannelFileDTO;
import io.xeres.common.dto.channel.ChannelGroupDTO;
import io.xeres.common.dto.channel.ChannelMessageDTO;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public final class ChannelMapper
{
	private ChannelMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ChannelGroupDTO toDTO(ChannelGroupItem item)
	{
		if (item == null)
		{
			return null;
		}

		return new ChannelGroupDTO(
				item.getId(),
				item.getGxsId(),
				item.getName(),
				item.getDescription(),
				item.hasImage(),
				item.isSubscribed(),
				item.isExternal(),
				item.getVisibleMessageCount(),
				item.getLastActivity()
		);
	}

	public static List<ChannelGroupDTO> toDTOs(List<ChannelGroupItem> items)
	{
		return emptyIfNull(items).stream()
				.map(ChannelMapper::toDTO)
				.toList();
	}

	public static ChannelMessageDTO toDTO(ChannelMessageItem item, String authorName, long originalId, long parentId)
	{
		if (item == null)
		{
			return null;
		}

		return new ChannelMessageDTO(
				item.getId(),
				item.getGxsId(),
				item.getMsgId(),
				originalId,
				parentId,
				item.getAuthorGxsId(),
				authorName,
				item.getName(),
				item.getPublished(),
				null,
				item.hasImage(),
				item.getImageWidth(),
				item.getImageHeight(),
				item.hasFiles(),
				List.of(),
				item.isRead()
		);
	}

	public static List<ChannelMessageDTO> toSummaryMessageDTOs(Page<ChannelMessageItem> items, Map<GxsId, IdentityGroupItem> authorsMap, Map<MsgId, ChannelMessageItem> messagesMap)
	{
		return items.stream()
				.map(item -> toDTO(item,
						authorsMap.getOrDefault(item.getAuthorGxsId(), IdentityGroupItem.EMPTY).getName(),
						messagesMap.getOrDefault(item.getOriginalMsgId(), ChannelMessageItem.EMPTY).getId(),
						messagesMap.getOrDefault(item.getParentMsgId(), ChannelMessageItem.EMPTY).getId()
				))
				.toList();
	}

	public static ChannelMessageDTO toDTO(UnHtmlService unHtmlService, ChannelMessageItem item, String authorName, long originalId, long parentId, boolean withMessageContent)
	{
		if (item == null)
		{
			return null;
		}

		return new ChannelMessageDTO(
				item.getId(),
				item.getGxsId(),
				item.getMsgId(),
				originalId,
				parentId,
				item.getAuthorGxsId(),
				authorName,
				item.getName(),
				item.getPublished(),
				withMessageContent ? unHtmlService.cleanupMessage(item.getContent()) : "",
				item.hasImage(),
				item.getImageWidth(),
				item.getImageHeight(),
				item.hasFiles(),
				withMessageContent ? toChannelFileDTOs(item.getFiles()) : List.of(),
				item.isRead()
		);
	}

	private static List<ChannelFileDTO> toChannelFileDTOs(List<FileItem> files)
	{
		return emptyIfNull(files).stream()
				.map(ChannelMapper::toChannelFileDTO)
				.toList();
	}

	private static ChannelFileDTO toChannelFileDTO(FileItem item)
	{
		if (item == null)
		{
			return null;
		}
		return new ChannelFileDTO(
				item.size(),
				item.hash(),
				item.name(),
				item.path(),
				item.age()
		);
	}

	public static List<ChannelMessageDTO> toChannelMessageDTOs(UnHtmlService unHtmlService, List<ChannelMessageItem> items, Map<GxsId, IdentityGroupItem> authorsMap, Map<MsgId, ChannelMessageItem> messagesMap, boolean withMessageContent)
	{
		return emptyIfNull(items).stream()
				.map(item -> toDTO(unHtmlService,
						item,
						authorsMap.getOrDefault(item.getAuthorGxsId(), IdentityGroupItem.EMPTY).getName(),
						messagesMap.getOrDefault(item.getOriginalMsgId(), ChannelMessageItem.EMPTY).getId(),
						messagesMap.getOrDefault(item.getParentMsgId(), ChannelMessageItem.EMPTY).getId(),
						withMessageContent
				))
				.toList();
	}
}
