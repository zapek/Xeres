package io.xeres.app.xrs.service.chat;

import io.xeres.app.database.model.chat.ChatRoom;
import io.xeres.app.database.model.chat.ChatRoomFakes;
import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.app.database.repository.ChatRoomRepository;
import io.xeres.common.message.chat.RoomType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ChatRoomServiceTest
{
	@Mock
	private ChatRoomRepository chatRoomRepository;

	@InjectMocks
	private ChatRoomService chatRoomService;

	@Test
	void CreateChatRoom_Success()
	{
		chatRoomService.createChatRoom(createSignedChatRoom(), IdentityFakes.createOwn());
		verify(chatRoomRepository).save(any(ChatRoom.class));
	}

	@Test
	void SubscribeToChatRoomAndJoin_Success()
	{
		var serviceChatRoom = createSignedChatRoom();
		var identity = IdentityFakes.createOwn();
		var chatRoom = ChatRoomFakes.createChatRoomEntity(serviceChatRoom.getId(), identity, serviceChatRoom.getName(), serviceChatRoom.getTopic(), 0);

		when(chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoom.getRoomId(), identity)).thenReturn(Optional.of(chatRoom));
		when(chatRoomRepository.save(chatRoom)).thenReturn(chatRoom);

		var subscribedChatRoom = chatRoomService.subscribeToChatRoomAndJoin(serviceChatRoom, identity);

		assertTrue(subscribedChatRoom.isSubscribed());
		assertTrue(subscribedChatRoom.isJoined());

		verify(chatRoomRepository).findByRoomIdAndIdentityGroupItem(chatRoom.getRoomId(), identity);
	}

	@Test
	void UnsubscribeFromChatRoomAndLeave_Success()
	{
		var serviceChatRoom = createSignedChatRoom();
		var identity = IdentityFakes.createOwn();
		var chatRoom = ChatRoomFakes.createChatRoomEntity(serviceChatRoom.getId(), identity, serviceChatRoom.getName(), serviceChatRoom.getTopic(), 0);

		when(chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoom.getRoomId(), identity)).thenReturn(Optional.of(chatRoom));
		when(chatRoomRepository.save(chatRoom)).thenReturn(chatRoom);

		var unsubscribedChatRoom = chatRoomService.unsubscribeFromChatRoomAndLeave(serviceChatRoom.getId(), identity);

		assertFalse(unsubscribedChatRoom.isSubscribed());
		assertFalse(unsubscribedChatRoom.isJoined());

		verify(chatRoomRepository).findByRoomIdAndIdentityGroupItem(chatRoom.getRoomId(), identity);
	}

	private io.xeres.app.xrs.service.chat.ChatRoom createSignedChatRoom()
	{
		return new io.xeres.app.xrs.service.chat.ChatRoom(1L, "test", "something", RoomType.PUBLIC, 1, true);
	}
}
