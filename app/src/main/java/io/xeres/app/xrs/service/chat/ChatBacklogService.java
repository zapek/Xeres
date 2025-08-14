/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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
import io.xeres.app.database.model.chat.DistantChatBacklog;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.repository.*;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ChatBacklogService
{
	private static final Duration MAXIMUM_DURATION = Duration.ofDays(31);

	private final ChatBacklogRepository chatBacklogRepository;
	private final ChatRoomBacklogRepository chatRoomBacklogRepository;
	private final DistantChatBacklogRepository distantChatBacklogRepository;
	private final LocationRepository locationRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final GxsIdentityRepository gxsIdentityRepository;

	ChatBacklogService(ChatBacklogRepository chatBacklogRepository, ChatRoomBacklogRepository chatRoomBacklogRepository, DistantChatBacklogRepository distantChatBacklogRepository, LocationRepository locationRepository, ChatRoomRepository chatRoomRepository, GxsIdentityRepository gxsIdentityRepository)
	{
		this.chatBacklogRepository = chatBacklogRepository;
		this.chatRoomBacklogRepository = chatRoomBacklogRepository;
		this.distantChatBacklogRepository = distantChatBacklogRepository;
		this.locationRepository = locationRepository;
		this.chatRoomRepository = chatRoomRepository;
		this.gxsIdentityRepository = gxsIdentityRepository;
	}

	@Transactional
	public void storeIncomingChatRoomMessage(long chatRoomId, GxsId from, String nickname, String message)
	{
		var chatRoom = chatRoomRepository.findByRoomId(chatRoomId).orElseThrow();
		chatRoomBacklogRepository.save(new ChatRoomBacklog(chatRoom, from, nickname, message));
	}

	@Transactional
	public void storeOutgoingChatRoomMessage(long chatRoomId, String nickname, String message)
	{
		var chatRoom = chatRoomRepository.findByRoomId(chatRoomId).orElseThrow();
		chatRoomBacklogRepository.save(new ChatRoomBacklog(chatRoom, nickname, message));
	}

	@Transactional(readOnly = true)
	public List<ChatRoomBacklog> getChatRoomMessages(long chatRoomId, Instant from, int maxLines)
	{
		var chatRoom = chatRoomRepository.findByRoomId(chatRoomId).orElseThrow();
		return chatRoomBacklogRepository.findAllByRoomAndCreatedAfterOrderByCreatedDesc(chatRoom, from, Limit.of(maxLines)).reversed();
	}

	@Transactional
	public void deleteChatRoomMessages(long chatRoomId)
	{
		var chatRoom = chatRoomRepository.findByRoomId(chatRoomId).orElseThrow();
		chatRoomBacklogRepository.deleteAllByRoom(chatRoom);
	}

	@Transactional
	public void storeIncomingMessage(LocationIdentifier from, String message)
	{
		var location = locationRepository.findByLocationIdentifier(from).orElseThrow();
		chatBacklogRepository.save(new ChatBacklog(location, false, message));
	}

	@Transactional
	public void storeOutgoingMessage(LocationIdentifier to, String message)
	{
		var location = locationRepository.findByLocationIdentifier(to).orElseThrow();
		chatBacklogRepository.save(new ChatBacklog(location, true, message));
	}

	public List<ChatBacklog> getMessages(Location with, Instant from, int maxLines)
	{
		return chatBacklogRepository.findAllByLocationAndCreatedAfterOrderByCreatedDesc(with, from, Limit.of(maxLines)).reversed();
	}

	@Transactional
	public void deleteMessages(Location of)
	{
		chatBacklogRepository.deleteAllByLocation(of);
	}

	@Transactional
	public void storeIncomingDistantMessage(GxsId from, String message)
	{
		var gxsId = gxsIdentityRepository.findByGxsId(from).orElseThrow();
		distantChatBacklogRepository.save(new DistantChatBacklog(gxsId, false, message));
	}

	@Transactional
	public void storeOutgoingDistantMessage(GxsId to, String message)
	{
		var gxsId = gxsIdentityRepository.findByGxsId(to).orElseThrow();
		distantChatBacklogRepository.save(new DistantChatBacklog(gxsId, true, message));
	}

	public List<DistantChatBacklog> getDistantMessages(IdentityGroupItem with, Instant from, int maxLines)
	{
		return distantChatBacklogRepository.findAllByIdentityGroupItemAndCreatedAfterOrderByCreatedDesc(with, from, Limit.of(maxLines)).reversed();
	}

	@Transactional
	public void deleteDistantMessages(IdentityGroupItem of)
	{
		distantChatBacklogRepository.deleteAllByIdentityGroupItem(of);
	}

	@Transactional
	public void cleanup()
	{
		chatBacklogRepository.deleteAllByCreatedBefore(Instant.now().minus(MAXIMUM_DURATION));
		chatRoomBacklogRepository.deleteAllByCreatedBefore(Instant.now().minus(MAXIMUM_DURATION));
	}
}
