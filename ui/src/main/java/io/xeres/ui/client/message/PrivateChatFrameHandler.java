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

package io.xeres.ui.client.message;

import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.location.Availability;
import io.xeres.common.message.MessageType;
import io.xeres.common.message.chat.ChatAvatar;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;

import static io.xeres.common.message.MessageHeaders.DESTINATION_ID;
import static io.xeres.common.message.MessageHeaders.MESSAGE_TYPE;

/**
 * This handles the incoming private messages from the server to the UI.
 */
public class PrivateChatFrameHandler implements StompFrameHandler
{
	private final WindowManager windowManager;

	public PrivateChatFrameHandler(WindowManager windowManager)
	{
		this.windowManager = windowManager;
	}

	/**
	 * Gets the payload type. It's not possible to use null or new Object(). It has to be a class
	 * that is serializable by jackson.
	 *
	 * @param headers the headers
	 * @return a type
	 */
	@Override
	public Type getPayloadType(StompHeaders headers)
	{
		var messageType = MessageType.valueOf(headers.getFirst(MESSAGE_TYPE));
		return switch (messageType)
				{
					case CHAT_PRIVATE_MESSAGE, CHAT_TYPING_NOTIFICATION -> ChatMessage.class;
					case CHAT_AVATAR -> ChatAvatar.class;
					case CHAT_AVAILABILITY -> Availability.class;
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
						case CHAT_PRIVATE_MESSAGE, CHAT_TYPING_NOTIFICATION -> windowManager.openMessaging(LocationIdentifier.fromString(headers.getFirst(DESTINATION_ID)), (ChatMessage) payload);
						case CHAT_AVATAR -> windowManager.sendMessaging(headers.getFirst(DESTINATION_ID), (ChatAvatar) payload);
						case CHAT_AVAILABILITY -> windowManager.sendMessaging(headers.getFirst(DESTINATION_ID), (Availability) payload);
					}
				}
		);
	}
}
