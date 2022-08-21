/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.ui.client;

import io.xeres.common.rest.notification.StatusNotificationResponse;
import io.xeres.ui.JavaFxApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;

import static io.xeres.common.rest.PathConfig.NOTIFICATIONS_PATH;

@Component
public class NotificationClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public NotificationClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@PostConstruct
	private void init()
	{
		webClient = webClientBuilder
				.baseUrl(JavaFxApplication.getControlUrl() + NOTIFICATIONS_PATH)
				.build();
	}

	public Flux<ServerSentEvent<StatusNotificationResponse>> getNotifications()
	{
		return webClient.get()
				.uri("/status")
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<>()
				{
				});
	}
}
