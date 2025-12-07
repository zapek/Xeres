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

package io.xeres.ui.model.channel;

import io.xeres.common.dto.channel.ChannelGroupDTO;
import io.xeres.common.dto.channel.ChannelMessageDTO;

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
		channelGroup.setSubscribed(dto.subscribed());
		channelGroup.setExternal(dto.external());
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
		channelMessage.setMessageId(dto.messageId());
		channelMessage.setOriginalId(dto.originalId());
		channelMessage.setParentId(dto.parentId());
		channelMessage.setAuthorId(dto.authorId());
		channelMessage.setAuthorName(dto.authorName());
		channelMessage.setName(dto.name());
		channelMessage.setPublished(dto.published());
		channelMessage.setContent(dto.content());
		channelMessage.setHasImage(dto.hasImage());
		channelMessage.setRead(dto.read());
		return channelMessage;
	}
}
