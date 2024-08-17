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

package io.xeres.ui.client.message;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.*;

public class SessionHandler extends StompSessionHandlerAdapter
{
	private static final Logger log = LoggerFactory.getLogger(SessionHandler.class);

	public interface OnConnected
	{
		void connect(StompSession session);
	}

	private final OnConnected onConnected;

	SessionHandler(OnConnected onConnected)
	{
		this.onConnected = onConnected;
	}

	@Override
	public void afterConnected(@Nonnull StompSession session, @Nonnull StompHeaders connectedHeaders)
	{
		log.debug("Connected successfully to session {}, headers: {}", session, connectedHeaders);
		onConnected.connect(session);
	}

	@Override
	public void handleException(@Nonnull StompSession session, StompCommand command, @Nonnull StompHeaders headers, @Nonnull byte[] payload, @Nonnull Throwable exception)
	{
		log.error("StompSessionHandler Exception for session {}, command {}, headers {} and payload {}", session, command, headers, payload, exception);
	}

	@Override
	public void handleTransportError(@Nonnull StompSession session, @Nonnull Throwable exception)
	{
		if (exception instanceof ConnectionLostException)
		{
			log.debug("Connection closed");
		}
		else
		{
			log.warn("StompSessionHandler Transport Exception for session {}", session, exception);
		}
	}
}
