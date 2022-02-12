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

import io.xeres.common.id.LocationId;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.ui.JavaFxApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.websocket.ContainerProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static io.xeres.common.message.MessageHeaders.DESTINATION_ID;
import static io.xeres.common.message.MessageHeaders.MESSAGE_TYPE;
import static io.xeres.common.message.MessageType.*;
import static io.xeres.common.rest.PathConfig.CHAT_PATH;

/**
 * This sends messages to the server.
 */
@Component
public class MessageClient
{
	private static final Logger log = LoggerFactory.getLogger(MessageClient.class);

	private WebSocketStompClient stompClient;

	private ListenableFuture<StompSession> future;

	private StompSession stompSession;

	private final List<PendingSubscription> pendingSubscriptions = new ArrayList<>();
	private final List<StompSession.Subscription> subscriptions = new ArrayList<>();

	public MessageClient connect()
	{
		String url = "ws://" + JavaFxApplication.getHostnameAndPort() + "/ws";

		var container = ContainerProvider.getWebSocketContainer();
		container.setDefaultMaxTextMessageBufferSize(1024 * 1024); // 1 MB XXX: adjust maybe!
		container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024);

		WebSocketClient client = new StandardWebSocketClient(container);
		stompClient = new WebSocketStompClient(client);
		stompClient.setMessageConverter(new MappingJackson2MessageConverter());
		stompClient.setInboundMessageSizeLimit(1024 * 1024); // 1 MB

		var sessionHandler = new SessionHandler(session ->
		{
			stompSession = session;
			performPendingSubscriptions(stompSession);
		});

		log.debug("Connecting to {}", url);
		future = stompClient.connect(url, sessionHandler);

		return this;
	}

	public void subscribe(String path, StompFrameHandler frameHandler)
	{
		pendingSubscriptions.add(new PendingSubscription(path, frameHandler));

		if (stompSession != null)
		{
			performPendingSubscriptions(stompSession);
		}
	}

	public void sendToLocation(LocationId locationId, ChatMessage message)
	{
		Objects.requireNonNull(stompSession);

		var headers = new StompHeaders();
		headers.setDestination("/app" + CHAT_PATH);
		headers.set(MESSAGE_TYPE, message.isEmpty() ? CHAT_TYPING_NOTIFICATION.name() : CHAT_PRIVATE_MESSAGE.name());
		headers.set(DESTINATION_ID, locationId.toString());
		stompSession.send(headers, message);
	}

	public void sendToChatRoom(long chatRoomId, ChatMessage message)
	{
		Objects.requireNonNull(stompSession);

		var headers = new StompHeaders();
		headers.setDestination("/app" + CHAT_PATH);
		headers.set(MESSAGE_TYPE, message.isEmpty() ? CHAT_ROOM_TYPING_NOTIFICATION.name() : CHAT_ROOM_MESSAGE.name());
		headers.set(DESTINATION_ID, String.valueOf(chatRoomId));
		stompSession.send(headers, message);
	}

	public void sendBroadcast(ChatMessage message)
	{
		Objects.requireNonNull(stompSession);

		var headers = new StompHeaders();
		headers.setDestination("/app" + CHAT_PATH);
		headers.set(MESSAGE_TYPE, CHAT_BROADCAST_MESSAGE.name());
		stompSession.send(headers, message);
	}

	private void performPendingSubscriptions(StompSession session)
	{
		while (!pendingSubscriptions.isEmpty())
		{
			var pendingSubscription = pendingSubscriptions.remove(0);

			var subscription = session.subscribe(pendingSubscription.getPath(), pendingSubscription.getStompFrameHandler());
			subscriptions.add(subscription);
		}
	}

	// XXX: add unsubscribe()? how?

	// XXX: is the following needed still?
	public void disconnect()
	{
		try
		{
			future.get().disconnect();
		}
		catch (InterruptedException | ExecutionException e)
		{
			log.error("Error: {}", e.getMessage());
		}
		stompClient.stop(); // XXX: not sure this is needed actually...
	}
}
