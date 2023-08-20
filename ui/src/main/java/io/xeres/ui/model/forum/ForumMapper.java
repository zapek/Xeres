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

package io.xeres.ui.model.forum;

import io.xeres.common.dto.forum.ForumGroupDTO;
import io.xeres.common.dto.forum.ForumMessageDTO;
import io.xeres.common.message.forum.ForumGroup;
import io.xeres.common.message.forum.ForumMessage;

public final class ForumMapper
{
	private ForumMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ForumGroup fromDTO(ForumGroupDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var forumGroup = new ForumGroup();
		forumGroup.setId(dto.id());
		forumGroup.setName(dto.name());
		forumGroup.setGxsId(dto.gxsId());
		forumGroup.setDescription(dto.description());
		forumGroup.setSubscribed(dto.subscribed());
		forumGroup.setExternal(dto.external());
		return forumGroup;
	}

	public static ForumMessage fromDTO(ForumMessageDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var forumMessage = new ForumMessage();
		forumMessage.setId(dto.id());
		forumMessage.setGxsId(dto.gxsId());
		forumMessage.setMessageId(dto.messageId());
		forumMessage.setOriginalId(dto.originalId());
		forumMessage.setParentId(dto.parentId());
		forumMessage.setAuthorId(dto.authorId());
		forumMessage.setAuthorName(dto.authorName());
		forumMessage.setName(dto.name());
		forumMessage.setPublished(dto.published());
		forumMessage.setContent(dto.content());
		return forumMessage;
	}
}
