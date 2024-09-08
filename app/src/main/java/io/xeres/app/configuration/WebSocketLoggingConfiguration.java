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

package io.xeres.app.configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

@Configuration
public class WebSocketLoggingConfiguration
{
	private final WebSocketMessageBrokerStats webSocketMessageBrokerStats;

	public WebSocketLoggingConfiguration(WebSocketMessageBrokerStats webSocketMessageBrokerStats)
	{
		this.webSocketMessageBrokerStats = webSocketMessageBrokerStats;
	}

	@PostConstruct
	public void init()
	{
		// Avoids stats messages printed each 30 minutes
		webSocketMessageBrokerStats.setLoggingPeriod(0L);
	}
}
