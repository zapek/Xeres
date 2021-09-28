/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.service;

import io.xeres.app.database.model.chatroom.ChatRoom;
import io.xeres.app.database.model.identity.Identity;
import io.xeres.app.database.repository.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ChatRoomService
{
	private final ChatRoomRepository chatRoomRepository;

	public ChatRoomService(ChatRoomRepository chatRoomRepository)
	{
		this.chatRoomRepository = chatRoomRepository;
	}

	@Transactional
	public ChatRoom createChatRoom(io.xeres.app.xrs.service.chat.ChatRoom chatRoom, Identity identity)
	{
		return chatRoomRepository.save(ChatRoom.createChatRoom(chatRoom, identity));
	}

	@Transactional
	public ChatRoom subscribeToChatRoomAndJoin(io.xeres.app.xrs.service.chat.ChatRoom chatRoom, Identity identity)
	{
		var entity = chatRoomRepository.findByRoomIdAndIdentity(chatRoom.getId(), identity).orElseGet(() -> createChatRoom(chatRoom, identity));
		entity.setSubscribed(true);
		entity.setJoined(true);
		return chatRoomRepository.save(entity);
	}

	@Transactional
	public ChatRoom unsubscribeFromChatRoomAndLeave(long chatRoomId, Identity identity)
	{
		Optional<ChatRoom> foundRoom = chatRoomRepository.findByRoomIdAndIdentity(chatRoomId, identity);

		foundRoom.ifPresent(subscribedRoom -> {
			subscribedRoom.setSubscribed(false);
			subscribedRoom.setJoined(false);
			chatRoomRepository.save(subscribedRoom);
		});
		return foundRoom.orElse(null);
	}

	@Transactional
	public void deleteChatRoom(long chatRoomId, Identity identity)
	{
		chatRoomRepository.findByRoomIdAndIdentity(chatRoomId, identity).ifPresent(chatRoomRepository::delete);
	}

	public List<ChatRoom> getAllChatRoomsPendingToSubscribe()
	{
		return chatRoomRepository.findAllBySubscribedTrueAndJoinedFalse();
	}

	@Transactional
	public void markAllChatRoomsAsLeft()
	{
		chatRoomRepository.putAllJoinedToFalse();
	}
}
