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

package io.xeres.app.database.model.chat;

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
	void toDTO_Success()
	{
		var chatRoom = ChatRoomFakes.createChatRoom();
		var chatRoomDTO = ChatMapper.toDTO(chatRoom.getAsRoomInfo());

		assertEquals(chatRoom.getId(), chatRoomDTO.id());
		assertEquals(chatRoom.getName(), chatRoomDTO.name());
		assertEquals(chatRoom.getTopic(), chatRoomDTO.topic());
		assertEquals(chatRoom.isSigned(), chatRoomDTO.isSigned());
		// flags aren't compared as their logic is different
	}
}
