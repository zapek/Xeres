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

package io.xeres.app.service.script;

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.board.BoardMapper;
import io.xeres.app.service.*;
import io.xeres.app.xrs.service.board.BoardRsService;
import io.xeres.app.xrs.service.chat.ChatRsService;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.message.MessageType;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.common.message.chat.ChatRoomMessage;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.ByteSequence;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.xeres.app.database.model.board.BoardMapper.toBoardMessageDTOs;
import static io.xeres.common.message.MessagePath.*;

/// The Xeres API callable by JS scripts.
@Component
public class JsXeres
{
	private final Map<String, Value> eventHandlers = new ConcurrentHashMap<>();
	private final ChatRsService chatRsService;
	private final MessageService messageService;
	private final IdentityService identityService;
	private final LocationService locationService;
	private final BoardRsService boardRsService;
	private final DatabaseSessionManager databaseSessionManager;
	private final UnHtmlService unHtmlService;
	private final BoardMessageService boardMessageService;

	public JsXeres(@Lazy ChatRsService chatRsService, MessageService messageService, IdentityService identityService, LocationService locationService, BoardRsService boardRsService, DatabaseSessionManager databaseSessionManager, UnHtmlService unHtmlService, BoardMessageService boardMessageService)
	{

		this.chatRsService = chatRsService;
		this.messageService = messageService;
		this.identityService = identityService;
		this.locationService = locationService;
		this.boardRsService = boardRsService;
		this.databaseSessionManager = databaseSessionManager;
		this.unHtmlService = unHtmlService;
		this.boardMessageService = boardMessageService;
	}

	public Value getEventHandler(String event)
	{
		return eventHandlers.get(event);
	}

	/// Registers an event handler. Those are called by Xeres.
	///
	/// @param eventType the event type
	/// @param handler   the handler
	@HostAccess.Export
	public void registerEventHandler(String eventType, Value handler)
	{
		eventHandlers.put(eventType, handler);
	}

	/// Sends a message to a chat room.
	///
	/// @param roomId  the room id
	/// @param message the message
	@HostAccess.Export
	public void sendChatRoomMessage(long roomId, String message)
	{
		chatRsService.sendChatRoomMessage(roomId, message);
		messageService.sendToConsumers(chatRoomDestination(), MessageType.CHAT_ROOM_MESSAGE, roomId, new ChatRoomMessage(identityService.getOwnIdentity().getName(), identityService.getOwnIdentity().getGxsId(), message));
	}

	/// Sends a private chat message.
	///
	/// @param destination the destination (location)
	/// @param message     the message
	@HostAccess.Export
	public void sendPrivateMessage(String destination, String message)
	{
		var location = LocationIdentifier.fromString(destination);
		chatRsService.sendPrivateMessage(location, message);
		var chatMessage = new ChatMessage(message);
		chatMessage.setOwn(true);
		messageService.sendToConsumers(chatPrivateDestination(), MessageType.CHAT_PRIVATE_MESSAGE, location, chatMessage);
	}

	/// Sends a distant chat message.
	///
	/// @param destination the destination (gxsId)
	/// @param message     the message
	@HostAccess.Export
	public void sendDistantMessage(String destination, String message)
	{
		var gxsId = GxsId.fromString(destination);
		chatRsService.sendPrivateMessage(gxsId, message);
		var chatMessage = new ChatMessage(message);
		chatMessage.setOwn(true);
		messageService.sendToConsumers(chatDistantDestination(), MessageType.CHAT_PRIVATE_MESSAGE, gxsId, chatMessage);
	}

	/// Gets the user's availability
	///
	/// @return the availability ("AVAILABLE", "BUSY", "AWAY" ,"OFFLINE").
	@HostAccess.Export
	public String getAvailability()
	{
		return locationService.findOwnLocation().orElseThrow().getAvailability().name();
	}

	@HostAccess.Export
	public long writeBoardMessage(long boardId, String title, String content, String link, Value image) throws IOException
	{
		var byteSequence = image.as(ByteSequence.class);
		MultipartFile file = new JsMultipartFile("image", byteSequence.toByteArray());

		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			return boardRsService.createBoardMessage(identityService.getOwnIdentity(), boardId, title, content, link, file);
		}
	}

	@HostAccess.Export
	public List<Map<String, Object>> getAllSubscribedBoards()
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			return boardRsService.findAllSubscribedGroups().stream()
					.map(item -> {
						var dto = BoardMapper.toDTO(item);
						return Map.<String, Object>of(
								"id", dto.id(),
								"name", dto.name(),
								"description", dto.description(),
								"gxsId", dto.gxsId().asString());
					})
					.toList();
		}
	}

	@HostAccess.Export
	public List<Map<String, Object>> findAllMessages(long boardId, int pageNumber, int pageSize)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			var pageable = PageRequest.of(pageNumber, pageSize);
			var boardMessages = boardRsService.findAllMessages(boardId, pageable);

			return toBoardMessageDTOs(unHtmlService,
					boardMessages,
					boardMessageService.getAuthorsMapFromMessages(boardMessages),
					boardMessageService.getMessagesMapFromSummaries(boardId, boardMessages)).stream()
					.map(dto -> Map.<String, Object>of(
							"id", dto.id(),
							"name", dto.name(),
							"link", dto.link(),
							"authorName", dto.authorName(),
							"published", dto.published().getEpochSecond()))
					.toList();
		}
	}
}
