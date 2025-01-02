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

package io.xeres.app.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import static io.xeres.common.message.MessagingConfiguration.MAXIMUM_MESSAGE_SIZE;

@Configuration
@EnableWebSocket
@ConditionalOnProperty(value = "spring.main.web-application-type", havingValue = "servlet")
public class WebSocketConfiguration implements WebSocketConfigurer
{
	// See https://stackoverflow.com/questions/21730566/how-to-increase-output-buffer-for-spring-sockjs-websocket-server-implementation
	@Bean
	public ServletServerContainerFactoryBean createServletServerContainerFactoryBean()
	{
		var container = new ServletServerContainerFactoryBean();
		container.setMaxTextMessageBufferSize(MAXIMUM_MESSAGE_SIZE);
		container.setMaxBinaryMessageBufferSize(MAXIMUM_MESSAGE_SIZE);
		return container;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry)
	{
		// No custom handlers, we use STOMP
	}
}
