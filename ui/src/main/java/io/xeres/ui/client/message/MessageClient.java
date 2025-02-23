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

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Identifier;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.common.properties.StartupProperties;
import io.xeres.common.util.RemoteUtils;
import jakarta.websocket.ContainerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.xeres.common.message.MessageHeaders.DESTINATION_ID;
import static io.xeres.common.message.MessageHeaders.MESSAGE_TYPE;
import static io.xeres.common.message.MessagePath.*;
import static io.xeres.common.message.MessageType.*;
import static io.xeres.common.message.MessagingConfiguration.MAXIMUM_MESSAGE_SIZE;

/**
 * This sends messages to the server.
 */
@Component
public class MessageClient
{
	private static final Logger log = LoggerFactory.getLogger(MessageClient.class);

	private CompletableFuture<StompSession> future;

	private StompSession stompSession;
	private String username;
	private String password;

	private final List<PendingSubscription> pendingSubscriptions = new ArrayList<>();
	private final List<StompSession.Subscription> subscriptions = new ArrayList<>();

	public MessageClient connect()
	{
		var useHttps = StartupProperties.getBoolean(StartupProperties.Property.HTTPS, true);

		var url = (useHttps ? "wss://" : "ws://") + RemoteUtils.getHostnameAndPort() + "/ws";

		var container = ContainerProvider.getWebSocketContainer();
		container.setDefaultMaxTextMessageBufferSize(MAXIMUM_MESSAGE_SIZE);
		container.setDefaultMaxBinaryMessageBufferSize(MAXIMUM_MESSAGE_SIZE);

		var client = new StandardWebSocketClient(container);

		if (useHttps)
		{
			try
			{
				var sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, InsecureTrustManagerFactory.INSTANCE.getTrustManagers(), null);

				client.setSslContext(sslContext);
			}
			catch (KeyManagementException | NoSuchAlgorithmException e)
			{
				throw new RuntimeException(e);
			}
		}

		var stompClient = new WebSocketStompClient(client);
		stompClient.setMessageConverter(new MappingJackson2MessageConverter());
		stompClient.setInboundMessageSizeLimit(MAXIMUM_MESSAGE_SIZE);

		var sessionHandler = new SessionHandler(session ->
		{
			stompSession = session;
			performPendingSubscriptions(stompSession);
		});

		log.debug("Connecting to {}", url);
		var httpHeaders = new WebSocketHttpHeaders();
		if (password != null)
		{
			httpHeaders.setBasicAuth(username, password);
		}
		var connectHeaders = new StompHeaders();
		future = stompClient.connectAsync(url, httpHeaders, connectHeaders, sessionHandler);

		return this;
	}

	public void setAuthentication(String username, String password)
	{
		this.username = username;
		this.password = password;
	}

	public MessageClient subscribe(String path, StompFrameHandler frameHandler)
	{
		pendingSubscriptions.add(new PendingSubscription(path, frameHandler));

		if (stompSession != null)
		{
			performPendingSubscriptions(stompSession);
		}
		return this;
	}

	public void sendToDestination(Identifier identifier, ChatMessage message)
	{
		Objects.requireNonNull(stompSession);

		switch (identifier)
		{
			case LocationIdentifier locationIdentifier -> sendToLocation(locationIdentifier, message);
			case GxsId gxsId -> sendToGxsId(gxsId, message);
			default -> throw new IllegalStateException("Unexpected value: " + identifier);
		}
	}

	private void sendToLocation(LocationIdentifier locationIdentifier, ChatMessage message)
	{
		var headers = new StompHeaders();
		headers.setDestination(APP_PREFIX + CHAT_ROOT + CHAT_PRIVATE_DESTINATION);
		headers.set(MESSAGE_TYPE, message.isEmpty() ? CHAT_TYPING_NOTIFICATION.name() : CHAT_PRIVATE_MESSAGE.name());
		headers.set(DESTINATION_ID, locationIdentifier.toString());
		stompSession.send(headers, message);
	}

	private void sendToGxsId(GxsId gxsId, ChatMessage message)
	{
		var headers = new StompHeaders();
		headers.setDestination(APP_PREFIX + CHAT_ROOT + CHAT_DISTANT_DESTINATION);
		headers.set(MESSAGE_TYPE, message.isEmpty() ? CHAT_TYPING_NOTIFICATION.name() : CHAT_PRIVATE_MESSAGE.name());
		headers.set(DESTINATION_ID, gxsId.toString());
		stompSession.send(headers, message);
	}

	public void requestAvatar(Identifier identifier)
	{
		Objects.requireNonNull(stompSession);

		switch (identifier)
		{
			case LocationIdentifier locationIdentifier -> requestAvatarFromLocation(locationIdentifier);
			case GxsId gxsId -> requestAvatarFromGxsId(gxsId);
			default -> throw new IllegalStateException("Unexpected value: " + identifier);
		}
	}

	private void requestAvatarFromLocation(LocationIdentifier locationIdentifier)
	{
		var headers = new StompHeaders();
		headers.setDestination(APP_PREFIX + CHAT_ROOT + CHAT_PRIVATE_DESTINATION);
		headers.set(MESSAGE_TYPE, CHAT_AVATAR.name());
		headers.set(DESTINATION_ID, locationIdentifier.toString());
		stompSession.send(headers, new ChatMessage());
	}

	private void requestAvatarFromGxsId(GxsId gxsId)
	{
		var headers = new StompHeaders();
		headers.setDestination(APP_PREFIX + CHAT_ROOT + CHAT_DISTANT_DESTINATION);
		headers.set(MESSAGE_TYPE, CHAT_AVATAR.name());
		headers.set(DESTINATION_ID, gxsId.toString());
		stompSession.send(headers, new ChatMessage());
	}

	public void sendToChatRoom(long chatRoomId, ChatMessage message)
	{
		Objects.requireNonNull(stompSession);

		var headers = new StompHeaders();
		headers.setDestination(APP_PREFIX + CHAT_ROOT + CHAT_ROOM_DESTINATION);
		headers.set(MESSAGE_TYPE, message.isEmpty() ? CHAT_ROOM_TYPING_NOTIFICATION.name() : CHAT_ROOM_MESSAGE.name());
		headers.set(DESTINATION_ID, String.valueOf(chatRoomId));
		stompSession.send(headers, message);
	}

	public void sendBroadcast(ChatMessage message)
	{
		Objects.requireNonNull(stompSession);

		var headers = new StompHeaders();
		headers.setDestination(APP_PREFIX + CHAT_ROOT + CHAT_BROADCAST_DESTINATION);
		headers.set(MESSAGE_TYPE, CHAT_BROADCAST_MESSAGE.name());
		stompSession.send(headers, message);
	}

	private void performPendingSubscriptions(StompSession session)
	{
		log.debug("Performing subscriptions...");
		while (!pendingSubscriptions.isEmpty())
		{
			var pendingSubscription = pendingSubscriptions.removeFirst();

			var subscription = session.subscribe(pendingSubscription.getPath(), pendingSubscription.getStompFrameHandler());
			subscriptions.add(subscription);
		}
	}

	@EventListener
	public void onApplicationEvent(ContextClosedEvent ignored) // we don't use @PreDestroy because the tomcat context is closed before that
	{
		if (future != null)
		{
			try
			{
				subscriptions.forEach(StompSession.Subscription::unsubscribe); // if the connection is already closed (likely when running on the same host), we catch the MessageDeliveryException below as well as IllegalStateException
				future.get().disconnect();
			}
			catch (MessageDeliveryException | IllegalStateException | ExecutionException ignoredException)
			{
				// Nothing we can do
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
	}
}
