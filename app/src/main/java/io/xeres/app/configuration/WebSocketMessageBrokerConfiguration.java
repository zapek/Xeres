/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import static io.xeres.common.rest.PathConfig.CHAT_PATH;

/**
 * Configuration of the WebSocket. This is used for anything that requires a persistent connection from
 * the UI client to the server because of a bidirectional data stream (for example, chat windows).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketMessageBrokerConfiguration implements WebSocketMessageBrokerConfigurer
{
	private static final Logger log = LoggerFactory.getLogger(WebSocketMessageBrokerConfiguration.class);

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry)
	{
		registry.addEndpoint("/ws");
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry)
	{
		registry.setApplicationDestinationPrefixes("/app"); // this is for @Controller annotated endpoints
		registry.enableSimpleBroker(CHAT_PATH); // this is for the broker (subscriptions, ...)
	}

	@EventListener
	public void handleSessionSubscribeEvent(SessionSubscribeEvent event)
	{
		log.debug("Subscription from {}", event);
	}

	@EventListener
	public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event)
	{
		log.debug("Unsubscription from {}", event);
	}

	@EventListener
	public void handleSessionDisconnectEvent(SessionDisconnectEvent event)
	{
		log.debug("Disconnection from {}", event);
	}

	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registry)
	{
		registry.setMessageSizeLimit(1024 * 1024); // 1 MB XXX: adjust maybe, see also the client
		registry.setSendBufferSizeLimit(1024 * 1024);
	}
}
