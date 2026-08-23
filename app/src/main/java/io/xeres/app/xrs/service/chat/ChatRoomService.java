/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.chat;

import io.xeres.app.database.repository.ChatRoomRepository;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/// Helper service to manage chat room subscriptions and so on.
@Service
class ChatRoomService
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
			subscribedRoom.clearLocations();
		});
		return foundRoom.orElse(null);
	}

	public void deleteChatRoom(long chatRoomId, IdentityGroupItem identityGroupItem)
	{
		chatRoomRepository.findByRoomIdAndIdentityGroupItem(chatRoomId, identityGroupItem).ifPresent(chatRoomRepository::delete);
	}

	public List<io.xeres.app.database.model.chat.ChatRoom> getAllChatRoomsPendingToSubscribe()
	{
		return chatRoomRepository.findAllBySubscribedTrueAndJoinedFalse(); // Remember joined is set to false on startup
	}

	public void markAllChatRoomsAsLeft()
	{
		chatRoomRepository.putAllJoinedToFalse();
	}

	@Transactional
	public void syncParticipatingLocations(io.xeres.app.xrs.service.chat.ChatRoom chatRoom)
	{
		var room = chatRoomRepository.findByRoomId(chatRoom.getId()).orElseThrow();
		room.clearLocations();
		chatRoom.getParticipatingLocations().forEach(room::addLocation);
	}
}
