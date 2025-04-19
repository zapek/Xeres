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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

import static io.xeres.common.properties.StartupProperties.Property.CONTROL_PASSWORD;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration
{
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, SettingsService settingsService) throws Exception
	{
		http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(authorize -> {
					authorize.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
					if (settingsService.isRemoteEnabled())
					{
						if (settingsService.hasRemotePassword() && StartupProperties.getBoolean(CONTROL_PASSWORD, true))
						{
							authorize.anyRequest().authenticated();
						}
						else
						{
							authorize.anyRequest().anonymous();
						}
					}
					else
					{
						if (settingsService.hasRemotePassword() && StartupProperties.getBoolean(CONTROL_PASSWORD, true))
						{
							authorize.anyRequest().access(new WebExpressionAuthorizationManager("isAuthenticated() && hasIpAddress('127.0.0.1')"));
						}
						else
						{
							authorize.anyRequest().access(new WebExpressionAuthorizationManager("isAnonymous() && hasIpAddress('127.0.0.1')"));
						}
							}
						}
				)
				.httpBasic(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(STATELESS));
		return http.build();
	}

	@Bean
	public UserDetailsService userDetailsService(SettingsService settingsService)
	{
		var userDetails = User.withUsername("user")
				.password("{noop}" + settingsService.getRemotePassword())
				.roles("USER")
				.build();

		return new InMemoryUserDetailsManager(userDetails);
	}
}
