package io.xeres.app.xrs.service.chat;

import io.xeres.app.database.repository.ChatRoomRepository;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Helper service to manage chat room subscriptions and so on.
 */
@Service
public class ChatRoomService
{
	private final ChatRoomRepository chatRoomRepository;

	public ChatRoomService(ChatRoomRepository chatRoomRepository)
	{
		this.chatRoomRepository = chatRoomRepository;
	}

	@Transactional
	public io.xeres.app.database.model.chat.ChatRoom createChatRoom(io.xeres.app.xrs.service.chat.ChatRoom chatRoom, IdentityGroupItem identityGroupItem)
	{
		return chatRoomRepository.save(io.xeres.app.database.model.chat.ChatRoom.createChatRoom(chatRoom, identityGroupItem));
	}

	@Transactional
	public io.xeres.app.database.model.chat.ChatRoom subscribeToChatRoomAndJoin(io.xeres.app.xrs.service.chat.ChatRoom chatRoom, IdentityGroupItem identityGroupItem)
	{
		var entity = chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoom.getId(), identityGroupItem).orElseGet(() -> createChatRoom(chatRoom, identityGroupItem));
		entity.setSubscribed(true);
		entity.setJoined(true);
		return entity;
	}

	@Transactional
	public io.xeres.app.database.model.chat.ChatRoom unsubscribeFromChatRoomAndLeave(long chatRoomId, IdentityGroupItem identityGroupItem)
	{
		var foundRoom = chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoomId, identityGroupItem);

		foundRoom.ifPresent(subscribedRoom -> {
			subscribedRoom.setSubscribed(false);
			subscribedRoom.setJoined(false);
		});
		return foundRoom.orElse(null);
	}

	@Transactional
	public void deleteChatRoom(long chatRoomId, IdentityGroupItem identityGroupItem)
	{
		chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoomId, identityGroupItem).ifPresent(chatRoomRepository::delete);
	}

	@Transactional
	public List<io.xeres.app.database.model.chat.ChatRoom> getAllChatRoomsPendingToSubscribe()
	{
		return chatRoomRepository.findAllBySubscribedTrueAndJoinedFalse(); // Remember joined is set to false on startup
	}

	@Transactional
	public void markAllChatRoomsAsLeft()
	{
		chatRoomRepository.putAllJoinedToFalse();
	}
}
