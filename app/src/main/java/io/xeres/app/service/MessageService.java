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

package io.xeres.app.service;

import io.xeres.common.id.Identifier;
import io.xeres.common.message.MessageType;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.xeres.common.message.MessageHeaders.DESTINATION_ID;
import static io.xeres.common.message.MessageHeaders.MESSAGE_TYPE;

@Service
public class MessageService
{
	private final SimpMessageSendingOperations messagingTemplate;

	public MessageService(SimpMessageSendingOperations messagingTemplate)
	{
		this.messagingTemplate = messagingTemplate;
	}

	public void sendToConsumers(String path, MessageType type, Object payload)
	{
		var headers = buildMessageHeaders(type);
		sendToConsumers(path, headers, payload);
	}

	public void sendToConsumers(String path, MessageType type, long destination, Object payload)
	{
		var headers = buildMessageHeaders(type, String.valueOf(destination));
		sendToConsumers(path, headers, payload);
	}

	public void sendToConsumers(String path, MessageType type, Identifier destination, Object payload)
	{
		var headers = buildMessageHeaders(type, destination.toString());
		sendToConsumers(path, headers, payload);
	}

	private void sendToConsumers(String path, Map<String, Object> headers, Object payload)
	{
		Objects.requireNonNull(payload, "Payload *must* be an object that can be serialized to JSON");
		messagingTemplate.convertAndSend(path, payload, headers);
	}

	private static Map<String, Object> buildMessageHeaders(MessageType messageType, String id)
	{
		Map<String, Object> headers = new HashMap<>();
		headers.put(MESSAGE_TYPE, messageType.name());
		if (id != null)
		{
			headers.put(DESTINATION_ID, id);
		}
		return headers;
	}

	private static Map<String, Object> buildMessageHeaders(MessageType messageType)
	{
		return buildMessageHeaders(messageType, null);
	}
}
