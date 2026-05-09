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

package io.xeres.app.api.controller.chat;

import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.service.MessageService;
import io.xeres.app.xrs.service.chat.ChatRsService;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.message.MessagePath;
import io.xeres.common.message.MessageType;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.common.message.chat.ChatRoomMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMessageControllerTest
{
	@Mock
	private ChatRsService chatRsService;

	@Mock
	private MessageService messageService;

	@InjectMocks
	private ChatMessageController controller;

	@Test
	void processPrivateChatMessage_sendsPrivateMessageAndNotifiesConsumers()
	{
		var location = LocationFakes.createLocation();
		String dest = location.getLocationIdentifier().toString();
		var msg = new ChatMessage("hello");

		controller.processPrivateChatMessageFromProducer(dest, MessageType.CHAT_PRIVATE_MESSAGE, msg);

		verify(chatRsService).sendPrivateMessage(LocationIdentifier.fromString(dest), "hello");
		verify(messageService).sendToConsumers(MessagePath.chatPrivateDestination(), MessageType.CHAT_PRIVATE_MESSAGE, LocationIdentifier.fromString(dest), msg);
		assertTrue(msg.isOwn());
	}

	@Test
	void processDistantChatMessage_sendsPrivateMessageAndNotifiesConsumers()
	{
		var location = LocationFakes.createLocation();
		String dest = location.getLocationIdentifier().toString();
		var msg = new ChatMessage("hiya");

		controller.processDistantChatMessageFromProducer(dest, MessageType.CHAT_PRIVATE_MESSAGE, msg);

		verify(chatRsService).sendPrivateMessage(GxsId.fromString(dest), "hiya");
		verify(messageService).sendToConsumers(MessagePath.chatDistantDestination(), MessageType.CHAT_PRIVATE_MESSAGE, GxsId.fromString(dest), msg);
		assertTrue(msg.isOwn());
	}

	@Test
	void processChatRoomMessage_sendsRoomMessageAndNotifiesConsumers()
	{
		String dest = "42";
		var crm = new ChatRoomMessage(null, null, "roommsg");

		controller.processChatRoomMessageFromProducer(dest, MessageType.CHAT_ROOM_MESSAGE, crm);

		verify(chatRsService).sendChatRoomMessage(42L, "roommsg");
		verify(messageService).sendToConsumers(MessagePath.chatRoomDestination(), MessageType.CHAT_ROOM_MESSAGE, 42L, crm);
	}

	@Test
	void processBroadcastMessage_sendsBroadcast()
	{
		var msg = new ChatMessage("broadcast");

		controller.processBroadcastMessageFromProducer(MessageType.CHAT_BROADCAST_MESSAGE, msg);

		verify(chatRsService).sendBroadcastMessage("broadcast");
	}

	@Test
	void handleException_returnsMessage()
	{
		var ex = new RuntimeException("boom");
		var result = controller.handleException(ex);
		assertEquals("boom", result);
	}

	@Test
	void processingUnexpectedMessageType_throwsIllegalStateException()
	{
		String dest = "00000000000000000000000000000000";
		var msg = new ChatMessage("oops");

		assertThrows(IllegalStateException.class, () -> controller.processPrivateChatMessageFromProducer(dest, MessageType.CHAT_BROADCAST_MESSAGE, msg));
	}
}
