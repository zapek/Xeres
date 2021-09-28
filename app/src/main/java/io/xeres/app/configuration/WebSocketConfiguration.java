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

package io.xeres.app.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import static io.xeres.common.rest.PathConfig.CHAT_PATH;

/**
 * Configuration of the WebSocket. This is used for anything that requires a persistent connection from
 * the UI client to the server because of a bidirectional data stream (ie. chat windows).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer
{
	private static final Logger log = LoggerFactory.getLogger(WebSocketConfiguration.class);

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

	// XXX: the following is useful for debugging... remove them once I'm done and if I don't need it (they could still be useful to DETECT if a client fails to subscribe to websockets)
	@EventListener
	public void handleSessionSubscribeEvent(SessionSubscribeEvent event)
	{
		log.debug("Subscription from {}", event);
	}

	@EventListener
	public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event)
	{
		log.debug("Unsubscription from {}", event); // XXX: seems to not be called?!
	}

	@EventListener
	public void handleSessionDisconnectEvent(SessionDisconnectEvent event)
	{
		log.debug("Disconnection from {}", event);
	}
}
