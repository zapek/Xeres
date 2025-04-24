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

import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.CompletableFuture;

public class SessionHandler extends StompSessionHandlerAdapter
{
	private static final Logger log = LoggerFactory.getLogger(SessionHandler.class);

	public interface OnConnected
	{
		void afterConnected(StompSession session);
	}

	private final WebSocketStompClient stompClient;
	private final String url;
	private final WebSocketHttpHeaders httpHeaders;
	private final OnConnected onConnected;

	private CompletableFuture<StompSession> future;


	SessionHandler(WebSocketStompClient stompClient, String url, WebSocketHttpHeaders httpHeaders, OnConnected onConnected)
	{
		this.stompClient = stompClient;
		this.url = url;
		this.httpHeaders = httpHeaders;
		this.onConnected = onConnected;
	}

	public void connect()
	{
		future = stompClient.connectAsync(url, httpHeaders, new StompHeaders(), this);
	}

	public CompletableFuture<StompSession> getFuture()
	{
		return future;
	}

	@Override
	public void afterConnected(StompSession session, StompHeaders connectedHeaders)
	{
		log.debug("Connected successfully to session {}, headers: {}", session, connectedHeaders);
		onConnected.afterConnected(session);
	}

	@Override
	public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception)
	{
		log.error("StompSessionHandler Exception for session {}, command {}, headers {} and payload {}", session, command, headers, payload, exception);
	}

	@Override
	public void handleTransportError(StompSession session, Throwable exception)
	{
		if (exception instanceof ConnectionLostException)
		{
			log.debug("Connection closed: {}", exception.getMessage());
			Platform.runLater(() -> UiUtils.alert(Alert.AlertType.ERROR, "WebSocket connection lost. Chat messages won't work anymore. Relaunch to fix."));
		}
		else
		{
			log.warn("StompSessionHandler Transport Exception for session {}", session, exception);
		}
	}
}
