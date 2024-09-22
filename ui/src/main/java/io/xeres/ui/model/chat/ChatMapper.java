/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.model.chat;

import io.xeres.common.dto.chat.*;
import io.xeres.common.message.chat.*;

public final class ChatMapper
{
	private ChatMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ChatRoomContext fromDTO(ChatRoomContextDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		return new ChatRoomContext(fromDTO(dto.chatRooms()), fromDTO(dto.identity()));
	}

	private static ChatRoomLists fromDTO(ChatRoomsDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var chatRoomLists = new ChatRoomLists();
		dto.available().forEach(chatRoomDTO -> chatRoomLists.addAvailable(fromDTO(chatRoomDTO)));
		dto.subscribed().forEach(chatRoomDTO -> chatRoomLists.addSubscribed(fromDTO(chatRoomDTO)));
		return chatRoomLists;
	}

	private static ChatRoomUser fromDTO(ChatIdentityDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		return new ChatRoomUser(dto.nickname(), dto.gxsId(), dto.image());
	}

	public static ChatRoomInfo fromDTO(ChatRoomDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		return new ChatRoomInfo(
				dto.id(),
				dto.name(),
				dto.roomType(),
				dto.topic(),
				dto.count(),
				dto.isSigned());
	}

	public static ChatRoomBacklog fromDTO(ChatRoomBacklogDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		return new ChatRoomBacklog(
				dto.created(),
				dto.gxsId(),
				dto.nickname(),
				dto.message()
		);
	}

	public static ChatBacklog fromDTO(ChatBacklogDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		return new ChatBacklog(
				dto.created(),
				dto.own(),
				dto.message()
		);
	}
}
