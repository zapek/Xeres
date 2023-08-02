/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

import io.xeres.common.dto.forum.ForumGroupDTO;
import io.xeres.common.dto.forum.ForumMessageDTO;
import io.xeres.common.message.forum.ForumGroup;
import io.xeres.common.message.forum.ForumMessage;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.model.forum.ForumMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static io.xeres.common.rest.PathConfig.FORUMS_PATH;

@Component
public class ForumClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public ForumClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@PostConstruct
	private void init()
	{
		webClient = webClientBuilder
				.baseUrl(JavaFxApplication.getControlUrl() + FORUMS_PATH)
				.build();
	}

	public Flux<ForumGroup> getForumGroups()
	{
		return webClient.get()
				.uri("/groups")
				.retrieve()
				.bodyToFlux(ForumGroupDTO.class)
				.map(ForumMapper::fromDTO);
	}

	public Mono<ForumGroup> getForumGroupById(long groupId)
	{
		return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/groups/{groupId}")
						.build(groupId))
				.retrieve()
				.bodyToMono(ForumGroupDTO.class)
				.map(ForumMapper::fromDTO);
	}

	public Mono<Long> subscribeToForumGroup(long groupId)
	{
		return webClient.put()
				.uri(uriBuilder -> uriBuilder
						.path("/groups/{groupId}/subscription")
						.build(groupId))
				.retrieve()
				.bodyToMono(Long.class);
	}

	public Mono<Void> unsubscribeFromForumGroup(long groupId)
	{
		return webClient.delete()
				.uri(uriBuilder -> uriBuilder
						.path("/groups/{groupId}/subscription")
						.build(groupId))
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Flux<ForumMessage> getForumMessages(long groupId)
	{
		return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/groups/{groupId}/messages")
						.build(groupId))
				.retrieve()
				.bodyToFlux(ForumMessageDTO.class)
				.map(ForumMapper::fromDTO);
	}

	public Mono<ForumMessage> getForumMessage(long messageId)
	{
		return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/messages/{messageId}")
						.build(messageId))
				.retrieve()
				.bodyToMono(ForumMessageDTO.class)
				.map(ForumMapper::fromDTO);
	}
}
