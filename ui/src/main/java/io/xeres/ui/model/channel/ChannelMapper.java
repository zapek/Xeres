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

package io.xeres.ui.model.channel;

import io.xeres.common.dto.channel.ChannelFileDTO;
import io.xeres.common.dto.channel.ChannelGroupDTO;
import io.xeres.common.dto.channel.ChannelMessageDTO;
import io.xeres.common.id.Sha1Sum;
import io.xeres.ui.client.PaginatedResponse;

import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public final class ChannelMapper
{
	private ChannelMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ChannelGroup fromDTO(ChannelGroupDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var channelGroup = new ChannelGroup();
		channelGroup.setId(dto.id());
		channelGroup.setName(dto.name());
		channelGroup.setGxsId(dto.gxsId());
		channelGroup.setDescription(dto.description());
		channelGroup.setHasImage(dto.hasImage());
		channelGroup.setSubscribed(dto.subscribed());
		channelGroup.setExternal(dto.external());
		channelGroup.setVisibleMessageCount(dto.visibleMessageCount());
		channelGroup.setLastActivity(dto.lastActivity());
		return channelGroup;
	}

	public static ChannelMessage fromDTO(ChannelMessageDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var channelMessage = new ChannelMessage();
		channelMessage.setId(dto.id());
		channelMessage.setGxsId(dto.gxsId());
		channelMessage.setMsgId(dto.msgId());
		channelMessage.setOriginalId(dto.originalId());
		channelMessage.setParentId(dto.parentId());
		channelMessage.setAuthorGxsId(dto.authorGxsId());
		channelMessage.setAuthorName(dto.authorName());
		channelMessage.setName(dto.name());
		channelMessage.setPublished(dto.published());
		channelMessage.setContent(dto.content());
		channelMessage.setHasImage(dto.hasImage());
		channelMessage.setImageWidth(dto.imageWidth());
		channelMessage.setImageHeight(dto.imageHeight());
		channelMessage.setHasFiles(dto.hasFiles());
		channelMessage.addFiles(fromFileDTOs(dto.files()));
		channelMessage.setRead(dto.read());
		return channelMessage;
	}

	private static List<ChannelFile> fromFileDTOs(List<ChannelFileDTO> dtos)
	{
		return emptyIfNull(dtos).stream()
				.map(ChannelMapper::fromFileDTO)
				.toList();
	}

	private static ChannelFile fromFileDTO(ChannelFileDTO dto)
	{
		if (dto == null)
		{
			return null;
		}
		return new ChannelFile(dto.name(), dto.path(), ChannelFile.State.DONE, dto.size(), dto.hash().toString());
	}

	public static PaginatedResponse<ChannelMessage> fromDTO(PaginatedResponse<ChannelMessageDTO> dto)
	{
		return new PaginatedResponse<>(
				dto.content().stream()
						.map(ChannelMapper::fromDTO)
						.toList(),
				dto.page()
		);
	}

	public static List<ChannelFileDTO> toChannelFileDTOs(List<ChannelFile> files)
	{
		return emptyIfNull(files).stream()
				.map(ChannelMapper::toDTO)
				.toList();
	}

	public static ChannelFileDTO toDTO(ChannelFile channelFile)
	{
		if (channelFile == null)
		{
			return null;
		}
		return new ChannelFileDTO(channelFile.getSize(), Sha1Sum.fromString(channelFile.getHash()), channelFile.getName(), channelFile.getPath(), 0);
	}
}
