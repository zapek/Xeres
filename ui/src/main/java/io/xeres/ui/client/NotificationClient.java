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

package io.xeres.ui.client;

import io.xeres.common.events.StartupEvent;
import io.xeres.common.rest.notification.availability.AvailabilityNotification;
import io.xeres.common.rest.notification.contact.ContactNotification;
import io.xeres.common.rest.notification.file.FileNotification;
import io.xeres.common.rest.notification.file.FileSearchNotification;
import io.xeres.common.rest.notification.forum.ForumNotification;
import io.xeres.common.rest.notification.status.StatusNotification;
import io.xeres.common.util.RemoteUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

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

	@EventListener
	public void init(StartupEvent event)
	{
		webClient = webClientBuilder
				.baseUrl(RemoteUtils.getControlUrl() + NOTIFICATIONS_PATH)
				.build();
	}

	public Flux<ServerSentEvent<StatusNotification>> getStatusNotifications()
	{
		return webClient.get()
				.uri("/status")
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<>()
				{
				});
	}

	public Flux<ServerSentEvent<ForumNotification>> getForumNotifications()
	{
		return webClient.get()
				.uri("/forum")
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<>()
				{
				});
	}

	public Flux<ServerSentEvent<FileNotification>> getFileNotifications()
	{
		return webClient.get()
				.uri("/file")
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<>()
				{
				});
	}

	public Flux<ServerSentEvent<FileSearchNotification>> getFileSearchNotifications()
	{
		return webClient.get()
				.uri("/fileSearch")
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<>()
				{
				});
	}

	public Flux<ServerSentEvent<ContactNotification>> getContactNotifications()
	{
		return webClient.get()
				.uri("/contact")
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<>()
				{
				});
	}

	public Flux<ServerSentEvent<AvailabilityNotification>> getConnectionNotifications()
	{
		return webClient.get()
				.uri("/availability")
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<>()
				{
				});
	}
}
