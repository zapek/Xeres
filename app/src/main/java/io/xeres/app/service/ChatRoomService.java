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

package io.xeres.app.service;

import io.xeres.app.database.model.chat.ChatRoom;
import io.xeres.app.database.repository.ChatRoomRepository;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
	public ChatRoom createChatRoom(io.xeres.app.xrs.service.chat.ChatRoom chatRoom, IdentityGroupItem identityGroupItem)
	{
		return chatRoomRepository.save(ChatRoom.createChatRoom(chatRoom, identityGroupItem));
	}

	@Transactional
	public ChatRoom subscribeToChatRoomAndJoin(io.xeres.app.xrs.service.chat.ChatRoom chatRoom, IdentityGroupItem identityGroupItem)
	{
		var entity = chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoom.getId(), identityGroupItem).orElseGet(() -> createChatRoom(chatRoom, identityGroupItem));
		entity.setSubscribed(true);
		entity.setJoined(true);
		return chatRoomRepository.save(entity);
	}

	@Transactional
	public ChatRoom unsubscribeFromChatRoomAndLeave(long chatRoomId, IdentityGroupItem identityGroupItem)
	{
		var foundRoom = chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoomId, identityGroupItem);

		foundRoom.ifPresent(subscribedRoom -> {
			subscribedRoom.setSubscribed(false);
			subscribedRoom.setJoined(false);
			chatRoomRepository.save(subscribedRoom);
		});
		return foundRoom.orElse(null);
	}

	@Transactional
	public void deleteChatRoom(long chatRoomId, IdentityGroupItem identityGroupItem)
	{
		chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoomId, identityGroupItem).ifPresent(chatRoomRepository::delete);
	}

	public List<ChatRoom> getAllChatRoomsPendingToSubscribe()
	{
		return chatRoomRepository.findAllBySubscribedTrueAndJoinedFalse(); // Remember joined is set to false on startup
	}

	@Transactional
	public void markAllChatRoomsAsLeft()
	{
		chatRoomRepository.putAllJoinedToFalse();
	}
}
