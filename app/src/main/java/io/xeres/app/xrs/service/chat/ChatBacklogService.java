/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.chat.ChatBacklog;
import io.xeres.app.database.model.chat.ChatRoomBacklog;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.repository.ChatBacklogRepository;
import io.xeres.app.database.repository.ChatRoomBacklogRepository;
import io.xeres.app.database.repository.ChatRoomRepository;
import io.xeres.app.database.repository.LocationRepository;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationId;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ChatBacklogService
{
	private static final int LAST_LINES_CHAT = 20;
	private static final int LAST_LINES_CHAT_ROOMS = 50;

	private final ChatBacklogRepository chatBacklogRepository;
	private final ChatRoomBacklogRepository chatRoomBacklogRepository;
	private final LocationRepository locationRepository;
	private final ChatRoomRepository chatRoomRepository;

	ChatBacklogService(ChatBacklogRepository chatBacklogRepository, ChatRoomBacklogRepository chatRoomBacklogRepository, LocationRepository locationRepository, ChatRoomRepository chatRoomRepository)
	{
		this.chatBacklogRepository = chatBacklogRepository;
		this.chatRoomBacklogRepository = chatRoomBacklogRepository;
		this.locationRepository = locationRepository;
		this.chatRoomRepository = chatRoomRepository;
	}

	@Transactional
	public void storeIncomingChatRoomMessage(long chatRoomId, GxsId from, String nickname, String message)
	{
		var chatRoom = chatRoomRepository.findByRoomId(chatRoomId).orElseThrow();
		chatRoomBacklogRepository.save(new ChatRoomBacklog(chatRoom, from, nickname, message));
	}

	@Transactional
	public void storeOutgoingChatRoomMessage(long chatRoomId, String message)
	{
		var chatRoom = chatRoomRepository.findByRoomId(chatRoomId).orElseThrow();
		chatRoomBacklogRepository.save(new ChatRoomBacklog(chatRoom, message));
	}

	@Transactional(readOnly = true)
	public List<ChatRoomBacklog> getChatRoomMessages(long chatRoomId, Instant from)
	{
		var chatRoom = chatRoomRepository.findByRoomId(chatRoomId).orElseThrow();
		return chatRoomBacklogRepository.findAllByRoomAndCreatedAfterOrderByCreatedDesc(chatRoom, from, Limit.of(LAST_LINES_CHAT_ROOMS)).reversed();
	}

	@Transactional
	public void storeIncomingMessage(LocationId from, String message)
	{
		var location = locationRepository.findByLocationId(from).orElseThrow();
		chatBacklogRepository.save(new ChatBacklog(location, false, message));
	}

	@Transactional
	public void storeOutgoingMessage(LocationId to, String message)
	{
		var location = locationRepository.findByLocationId(to).orElseThrow();
		chatBacklogRepository.save(new ChatBacklog(location, true, message));
	}

	public List<ChatBacklog> getMessages(Location with, Instant from)
	{
		return chatBacklogRepository.findAllByLocationAndCreatedAfterOrderByCreatedDesc(with, from, Limit.of(LAST_LINES_CHAT)).reversed();
	}

	public void storeIncomingDistantMessage(GxsId from, String message)
	{

	}

	public void storeOutgoingDistantMessage(GxsId to, String message)
	{

	}
}
