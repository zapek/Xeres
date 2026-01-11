/*
 * Copyright (c) 2023-2026 by David Gerber - https://zapek.com
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
import io.xeres.common.events.StartupEvent;
import io.xeres.common.rest.forum.CreateForumMessageRequest;
import io.xeres.common.rest.forum.CreateOrUpdateForumGroupRequest;
import io.xeres.common.rest.forum.UpdateForumMessagesReadRequest;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.model.forum.ForumGroup;
import io.xeres.ui.model.forum.ForumMapper;
import io.xeres.ui.model.forum.ForumMessage;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static io.xeres.common.rest.PathConfig.FORUMS_PATH;

@Component
public class ForumClient implements GxsGroupClient<ForumGroup>
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public ForumClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(@SuppressWarnings("unused") StartupEvent event)
	{
		webClient = webClientBuilder
				.baseUrl(RemoteUtils.getControlUrl() + FORUMS_PATH)
				.build();
	}

	@Override
	public Flux<ForumGroup> getGroups()
	{
		return webClient.get()
				.uri("/groups")
				.retrieve()
				.bodyToFlux(ForumGroupDTO.class)
				.map(ForumMapper::fromDTO);
	}

	public Mono<Void> createForumGroup(String name, String description)
	{
		var request = new CreateOrUpdateForumGroupRequest(name, description);

		return webClient.post()
				.uri("/groups")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<Void> updateForumGroup(long groupId, String name, String description)
	{
		var request = new CreateOrUpdateForumGroupRequest(name, description);

		return webClient.put()
				.uri("/groups/{groupId}", groupId)
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<ForumGroup> getForumGroupById(long groupId)
	{
		return webClient.get()
				.uri("/groups/{groupId}", groupId)
				.retrieve()
				.bodyToMono(ForumGroupDTO.class)
				.map(ForumMapper::fromDTO);
	}

	@Override
	public Mono<Integer> getUnreadCount(long groupId)
	{
		return webClient.get()
				.uri("/groups/{groupId}/unread-count", groupId)
				.retrieve()
				.bodyToMono(Integer.class);
	}

	@Override
	public Mono<Void> subscribeToGroup(long groupId)
	{
		return webClient.put()
				.uri("/groups/{groupId}/subscription", groupId)
				.retrieve()
				.bodyToMono(Void.class);
	}

	@Override
	public Mono<Void> unsubscribeFromGroup(long groupId)
	{
		return webClient.delete()
				.uri("/groups/{groupId}/subscription", groupId)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Flux<ForumMessage> getForumMessages(long groupId)
	{
		return webClient.get()
				.uri("/groups/{groupId}/messages", groupId)
				.retrieve()
				.bodyToFlux(ForumMessageDTO.class)
				.map(ForumMapper::fromDTO);
	}

	public Mono<ForumMessage> getForumMessage(long messageId)
	{
		return webClient.get()
				.uri("/messages/{messageId}", messageId)
				.retrieve()
				.bodyToMono(ForumMessageDTO.class)
				.map(ForumMapper::fromDTO);
	}

	public Mono<Void> createForumMessage(long forumId, String title, String content, long parentId, long originalId)
	{
		var request = new CreateForumMessageRequest(forumId, title, content, parentId, originalId);

		return webClient.post()
				.uri("/messages")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<Void> updateForumMessagesRead(Map<Long, Boolean> messages)
	{
		var request = new UpdateForumMessagesReadRequest(messages);

		return webClient.patch()
				.uri("/messages")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}
}
