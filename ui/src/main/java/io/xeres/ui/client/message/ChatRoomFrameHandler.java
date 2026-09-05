/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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
import io.xeres.common.message.chat.*;
import io.xeres.ui.controller.chat.ChatRoomViewController;
import javafx.application.Platform;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;
import java.util.Objects;

import static io.xeres.common.message.MessageHeaders.DESTINATION_ID;
import static io.xeres.common.message.MessageHeaders.MESSAGE_TYPE;

/// This handles the incoming chat room messages from the server to the UI.
public class ChatRoomFrameHandler implements StompFrameHandler
{
	private final ChatRoomViewController chatRoomViewController;

	public ChatRoomFrameHandler(ChatRoomViewController chatRoomViewController)
	{
		this.chatRoomViewController = chatRoomViewController;
	}

	/// Gets the payload type. It's not possible to use null or new Object(). It has to be a class
	/// that is serializable by jackson.
	///
	/// @param headers the headers
	/// @return a type
	@Override
	public @NonNull Type getPayloadType(@NonNull StompHeaders headers)
	{
		var messageType = MessageType.valueOf(headers.getFirst(MESSAGE_TYPE));
		return switch (messageType)
		{
			case CHAT_ROOM_JOIN, CHAT_ROOM_LEAVE, CHAT_ROOM_MESSAGE, CHAT_ROOM_TYPING_NOTIFICATION -> ChatRoomMessage.class;
			case CHAT_ROOM_LIST -> ChatRoomLists.class;
			case CHAT_ROOM_USER_JOIN, CHAT_ROOM_USER_LEAVE, CHAT_ROOM_USER_KEEP_ALIVE -> ChatRoomUserEvent.class;
			case CHAT_ROOM_USER_TIMEOUT -> ChatRoomTimeoutEvent.class;
			case CHAT_ROOM_INVITE -> ChatRoomInviteEvent.class;
			default -> throw new IllegalStateException("Unexpected value: " + messageType);
		};
	}

	@Override
	public void handleFrame(StompHeaders headers, Object payload)
	{
		var messageType = MessageType.valueOf(headers.getFirst(MESSAGE_TYPE));
		Platform.runLater(() -> {
					switch (messageType)
					{
						case CHAT_ROOM_MESSAGE, CHAT_ROOM_TYPING_NOTIFICATION -> chatRoomViewController.showMessage(getChatRoomMessage(headers, payload));
						case CHAT_ROOM_JOIN -> chatRoomViewController.roomJoined(getRoomId(headers));
						case CHAT_ROOM_LEAVE -> chatRoomViewController.roomLeft(getRoomId(headers));
						case CHAT_ROOM_LIST -> chatRoomViewController.addRooms((ChatRoomLists) payload);
						case CHAT_ROOM_USER_JOIN -> chatRoomViewController.userJoined(getRoomId(headers), (ChatRoomUserEvent) payload);
						case CHAT_ROOM_USER_LEAVE -> chatRoomViewController.userLeft(getRoomId(headers), (ChatRoomUserEvent) payload);
						case CHAT_ROOM_USER_KEEP_ALIVE -> chatRoomViewController.userKeepAlive(getRoomId(headers), (ChatRoomUserEvent) payload);
						case CHAT_ROOM_USER_TIMEOUT -> chatRoomViewController.userTimeout(getRoomId(headers), (ChatRoomTimeoutEvent) payload);
						case CHAT_ROOM_INVITE -> chatRoomViewController.openInvite(getRoomId(headers), (ChatRoomInviteEvent) payload);
						default -> throw new IllegalStateException("Unexpected value: " + messageType);
					}
				}
		);
	}

	private static ChatRoomMessage getChatRoomMessage(StompHeaders headers, Object payload)
	{
		var chatRoomMessage = (ChatRoomMessage) payload;
		chatRoomMessage.setRoomId(Long.parseLong(Objects.requireNonNull(headers.getFirst(DESTINATION_ID))));
		return chatRoomMessage;
	}

	private static long getRoomId(StompHeaders headers)
	{
		return Long.parseLong(Objects.requireNonNull(headers.getFirst(DESTINATION_ID)));
	}
}
