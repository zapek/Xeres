package io.xeres.app.database.model.chat;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatMapperTest
{
	@Test
	void ChatMapper_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ChatMapper.class);
	}

	@Test
	void ChatMapper_toDTO_OK()
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
