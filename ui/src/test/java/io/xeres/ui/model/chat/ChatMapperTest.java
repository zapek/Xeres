/*
 * Copyright (c) 2023-2026 by David Gerber - https://zapek.com
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

import io.xeres.common.dto.chat.ChatRoomContextDTOFakes;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatMapperTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ChatMapper.class);
	}

	@Test
	void FromDTO_ChatRoomContext_Success()
	{
		var dto = ChatRoomContextDTOFakes.createChatRoomContextDTO();

		var chatRoomContext = ChatMapper.fromDTO(dto);

		assertEquals(dto.chatRooms().available().size(), chatRoomContext.chatRoomLists().getAvailableRooms().size());
		assertEquals(dto.chatRooms().subscribed().size(), chatRoomContext.chatRoomLists().getSubscribedRooms().size());

		var from = dto.chatRooms().available().getFirst();
		var to = chatRoomContext.chatRoomLists().getAvailableRooms().getFirst();

		assertEquals(from.name(), to.getName());
		assertEquals(from.id(), to.getId());
		assertEquals(from.count(), to.getCount());
		assertEquals(from.roomType(), to.getRoomType());
		assertEquals(from.topic(), to.getTopic());
		assertEquals(from.isSigned(), to.isSigned());

		assertEquals(chatRoomContext.ownUser().nickname(), dto.identity().nickname());
		assertEquals(chatRoomContext.ownUser().gxsId(), dto.identity().gxsId());
		assertEquals(chatRoomContext.ownUser().identityId(), dto.identity().identityId());
	}
}
