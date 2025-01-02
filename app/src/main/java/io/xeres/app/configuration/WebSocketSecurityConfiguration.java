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

import io.xeres.app.service.SettingsService;
import io.xeres.common.properties.StartupProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

import static io.xeres.common.properties.StartupProperties.Property.CONTROL_PASSWORD;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfiguration
{
	@Bean
	AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages, SettingsService settingsService)
	{
		if (settingsService.hasRemotePassword() && StartupProperties.getBoolean(CONTROL_PASSWORD, true))
		{
			return AuthorityAuthorizationManager.hasRole("USER");
		}
		else
		{
			return AuthorityAuthorizationManager.hasRole("ANONYMOUS");
		}
	}
}
