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

package io.xeres.ui.client.message;

import io.xeres.common.message.MessageType;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.common.message.chat.ChatRoomListMessage;
import io.xeres.common.message.chat.ChatRoomMessage;
import io.xeres.common.message.chat.ChatRoomUserEvent;
import io.xeres.ui.controller.chat.ChatViewController;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;
import java.util.Objects;

import static io.xeres.common.message.MessageHeaders.DESTINATION_ID;
import static io.xeres.common.message.MessageHeaders.MESSAGE_TYPE;

/**
 * This handles the incoming messages from the server to the UI.
 */
public class ChatFrameHandler implements StompFrameHandler
{
	private static final Logger log = LoggerFactory.getLogger(ChatFrameHandler.class);

	private final WindowManager windowManager;
	private final ChatViewController chatViewController;

	public ChatFrameHandler(WindowManager windowManager, ChatViewController chatViewController)
	{
		this.windowManager = windowManager;
		this.chatViewController = chatViewController;
	}

	@Override
	public Type getPayloadType(StompHeaders headers)
	{
		var messageType = MessageType.valueOf(headers.getFirst(MESSAGE_TYPE));
		return switch (messageType)
				{
					case CHAT_PRIVATE_MESSAGE, CHAT_TYPING_NOTIFICATION -> ChatMessage.class;
					case CHAT_ROOM_JOIN, CHAT_ROOM_LEAVE, CHAT_ROOM_MESSAGE, CHAT_ROOM_TYPING_NOTIFICATION -> ChatRoomMessage.class;
					case CHAT_ROOM_LIST -> ChatRoomListMessage.class;
					case CHAT_ROOM_USER_JOIN, CHAT_ROOM_USER_LEAVE, CHAT_ROOM_USER_KEEP_ALIVE -> ChatRoomUserEvent.class;
					default -> throw new IllegalArgumentException("Missing class for message type " + messageType);
				};
	}

	@Override
	public void handleFrame(StompHeaders headers, Object payload)
	{
		var messageType = MessageType.valueOf(headers.getFirst(MESSAGE_TYPE));
		Platform.runLater(() -> {
					switch (messageType)
					{
						case CHAT_PRIVATE_MESSAGE, CHAT_TYPING_NOTIFICATION -> windowManager.openMessaging(headers.getFirst(DESTINATION_ID), (ChatMessage) payload);
						case CHAT_ROOM_MESSAGE, CHAT_ROOM_TYPING_NOTIFICATION -> chatViewController.showMessage(getChatRoomMessage(headers, payload));
						case CHAT_ROOM_JOIN -> chatViewController.roomJoined(getRoomId(headers));
						case CHAT_ROOM_LEAVE -> chatViewController.roomLeft(getRoomId(headers));
						case CHAT_ROOM_LIST -> chatViewController.addRooms(((ChatRoomListMessage) payload).getRooms());
						case CHAT_ROOM_USER_JOIN -> chatViewController.userJoined(getRoomId(headers), (ChatRoomUserEvent) payload);
						case CHAT_ROOM_USER_LEAVE -> chatViewController.userLeft(getRoomId(headers), (ChatRoomUserEvent) payload);
						case CHAT_ROOM_USER_KEEP_ALIVE -> chatViewController.userKeepAlive(getRoomId(headers), (ChatRoomUserEvent) payload);
						default -> log.error("Missing handling of {}", messageType);
					}
				}
		);
	}

	private ChatRoomMessage getChatRoomMessage(StompHeaders headers, Object payload)
	{
		var chatRoomMessage = (ChatRoomMessage) payload;
		chatRoomMessage.setRoomId(Long.parseLong(Objects.requireNonNull(headers.getFirst(DESTINATION_ID))));
		return chatRoomMessage;
	}

	private long getRoomId(StompHeaders headers)
	{
		return Long.parseLong(Objects.requireNonNull(headers.getFirst(DESTINATION_ID)));
	}
}
