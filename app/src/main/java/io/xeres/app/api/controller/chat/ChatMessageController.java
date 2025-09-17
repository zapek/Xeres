/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller.chat;

import io.xeres.app.service.MessageService;
import io.xeres.app.xrs.service.chat.ChatRsService;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.message.MessageType;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.common.message.chat.ChatRoomMessage;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Objects;

import static io.xeres.common.message.MessageHeaders.DESTINATION_ID;
import static io.xeres.common.message.MessageHeaders.MESSAGE_TYPE;
import static io.xeres.common.message.MessagePath.*;

/**
 * This controller receives WebSocket messages sent to /app, which means they're produced by the app user.
 * <p>
 * <img src="doc-files/websocket.svg" alt="WebSocket diagram">
 */
@Controller
@MessageMapping(CHAT_ROOT)
public class ChatMessageController
{
	private static final Logger log = LoggerFactory.getLogger(ChatMessageController.class);

	private final ChatRsService chatRsService;
	private final MessageService messageService;

	public ChatMessageController(ChatRsService chatRsService, MessageService messageService)
	{
		this.chatRsService = chatRsService;
		this.messageService = messageService;
	}

	@MessageMapping(CHAT_PRIVATE_DESTINATION)
	public void processPrivateChatMessageFromProducer(@Header(DESTINATION_ID) String destinationId, @Header(MESSAGE_TYPE) MessageType messageType, @Payload @Valid ChatMessage chatMessage)
	{
		switch (messageType)
		{
			case CHAT_PRIVATE_MESSAGE ->
			{
				logMessage("Received private chat websocket message, sending to peer location: " + destinationId, chatMessage.getContent());
				var locationIdentifier = LocationIdentifier.fromString(destinationId);
				chatRsService.sendPrivateMessage(locationIdentifier, chatMessage.getContent());
				chatMessage.setOwn(true);
				messageService.sendToConsumers(BROKER_PREFIX + CHAT_ROOT + CHAT_PRIVATE_DESTINATION, messageType, locationIdentifier, chatMessage);
			}
			case CHAT_TYPING_NOTIFICATION ->
			{
				log.debug("Sending private chat typing notification...");
				Objects.requireNonNull(destinationId);
				chatRsService.sendPrivateTypingNotification(LocationIdentifier.fromString(destinationId));
			}
			case CHAT_AVATAR ->
			{
				log.debug("Requesting private chat avatar...");
				Objects.requireNonNull(destinationId);
				chatRsService.sendAvatarRequest(LocationIdentifier.fromString(destinationId));
			}
			default -> throw new IllegalStateException("Unexpected value: " + messageType);
		}
	}

	@MessageMapping(CHAT_DISTANT_DESTINATION)
	public void processDistantChatMessageFromProducer(@Header(DESTINATION_ID) String destinationId, @Header(MESSAGE_TYPE) MessageType messageType, @Payload @Valid ChatMessage chatMessage)
	{
		switch (messageType)
		{
			case CHAT_PRIVATE_MESSAGE ->
			{
				logMessage("Received distant chat websocket message, sending to peer gxsId: " + destinationId, chatMessage.getContent());
				var gxsId = GxsId.fromString(destinationId);
				chatRsService.sendPrivateMessage(gxsId, chatMessage.getContent());
				chatMessage.setOwn(true);
				messageService.sendToConsumers(BROKER_PREFIX + CHAT_ROOT + CHAT_DISTANT_DESTINATION, messageType, gxsId, chatMessage);
			}
			case CHAT_TYPING_NOTIFICATION ->
			{
				log.debug("Sending distant chat typing notification...");
				Objects.requireNonNull(destinationId);
				chatRsService.sendPrivateTypingNotification(GxsId.fromString(destinationId));
			}
			default -> throw new IllegalStateException("Unexpected value: " + messageType);
		}
	}

	@MessageMapping(CHAT_ROOM_DESTINATION)
	public void processChatRoomMessageFromProducer(@Header(DESTINATION_ID) String destinationId, @Header(MESSAGE_TYPE) MessageType messageType, @Payload @Valid ChatRoomMessage chatRoomMessage)
	{
		switch (messageType)
		{
			case CHAT_ROOM_MESSAGE ->
			{
				logMessage("Sending to room " + destinationId + ", size: " + (chatRoomMessage.isEmpty() ? 0 : chatRoomMessage.getContent().length()), chatRoomMessage.getContent());
				Objects.requireNonNull(destinationId);
				var chatRoomId = Long.parseLong(destinationId);
				chatRsService.sendChatRoomMessage(chatRoomId, chatRoomMessage.getContent());
				messageService.sendToConsumers(BROKER_PREFIX + CHAT_ROOT + CHAT_ROOM_DESTINATION, messageType, chatRoomId, chatRoomMessage);
			}
			case CHAT_ROOM_TYPING_NOTIFICATION ->
			{
				log.debug("Sending chat room typing notification...");
				Objects.requireNonNull(destinationId);
				chatRsService.sendChatRoomTypingNotification(Long.parseLong(destinationId));
			}
			default -> throw new IllegalStateException("Unexpected value: " + messageType);
		}
	}

	@MessageMapping(CHAT_BROADCAST_DESTINATION)
	public void processBroadcastMessageFromProducer(@Header(MESSAGE_TYPE) MessageType messageType, @Payload @Valid ChatMessage chatMessage)
	{
		switch (messageType)
		{
			case CHAT_BROADCAST_MESSAGE ->
			{
				logMessage("Sending broadcast message", chatMessage.getContent());
				chatRsService.sendBroadcastMessage(chatMessage.getContent());
			}
			default -> throw new IllegalStateException("Unexpected value: " + messageType);
		}
	}

	@MessageExceptionHandler
	@SendToUser(DIRECT_PREFIX + "/errors") // XXX: how can we use this? Well, it works... just have to subscribe to it
	public String handleException(Throwable e)
	{
		log.debug("Got exception: {}", e.getMessage(), e);
		return e.getMessage();
	}

	private void logMessage(String info, String message)
	{
		if (log.isTraceEnabled())
		{
			log.trace("{}, content: {}", info, message);
		}
		else if (log.isDebugEnabled())
		{
			log.debug("{}", info);
		}
	}
}
