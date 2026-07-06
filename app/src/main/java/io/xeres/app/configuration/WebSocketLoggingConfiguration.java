/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

@Configuration
public class WebSocketLoggingConfiguration implements SmartLifecycle
{
	private final WebSocketMessageBrokerStats webSocketMessageBrokerStats;

	private boolean running;

	public WebSocketLoggingConfiguration(WebSocketMessageBrokerStats webSocketMessageBrokerStats)
	{
		this.webSocketMessageBrokerStats = webSocketMessageBrokerStats;
	}

	@Override
	public void start()
	{
		running = true;

		// Avoids stats messages printed each 30 minutes
		webSocketMessageBrokerStats.setLoggingPeriod(0L);
	}

	@Override
	public void stop()
	{
		running = false;
	}

	@Override
	public boolean isRunning()
	{
		return running;
	}
}
