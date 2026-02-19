/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

import io.xeres.common.dto.channel.ChannelGroupDTO;
import io.xeres.common.dto.channel.ChannelMessageDTO;
import io.xeres.common.events.StartupEvent;
import io.xeres.common.rest.channel.UpdateChannelMessagesReadRequest;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.model.channel.ChannelGroup;
import io.xeres.ui.model.channel.ChannelMapper;
import io.xeres.ui.model.channel.ChannelMessage;
import io.xeres.ui.support.util.ClientUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Map;

import static io.xeres.common.rest.PathConfig.CHANNELS_PATH;

@Component
public class ChannelClient implements GxsGroupClient<ChannelGroup>, GxsMessageClient<ChannelMessage>
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public ChannelClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(@SuppressWarnings("unused") StartupEvent event)
	{
		webClient = webClientBuilder.clone()
				.baseUrl(RemoteUtils.getControlUrl() + CHANNELS_PATH)
				.build();
	}

	@Override
	public Flux<ChannelGroup> getGroups()
	{
		return webClient.get()
				.uri("/groups")
				.retrieve()
				.bodyToFlux(ChannelGroupDTO.class)
				.map(ChannelMapper::fromDTO);
	}

	public Mono<Long> createChannelGroup(String name, String description, File image)
	{
		var builder = ClientUtils.createGroupBuilder(name, description, image);

		return webClient.post()
				.uri("/groups")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(builder.build()))
				.exchangeToMono(ClientUtils::getCreatedId);
	}

	public Mono<Void> updateChannelGroup(long groupId, String name, String description, File image, boolean updateImage)
	{
		var builder = ClientUtils.createGroupBuilder(name, description, image);
		if (updateImage)
		{
			builder.part("updateImage", true);
		}

		return webClient.put()
				.uri("/groups/{groupId}", groupId)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(builder.build()))
				.retrieve()
				.bodyToMono(Void.class);
	}


	public Mono<ChannelGroup> getChannelGroupById(long groupId)
	{
		return webClient.get()
				.uri("/groups/{groupId}", groupId)
				.retrieve()
				.bodyToMono(ChannelGroupDTO.class)
				.map(ChannelMapper::fromDTO);
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

	@Override
	public Mono<Void> markAllMessagesAsRead(long groupId, boolean read)
	{
		return webClient.put()
				.uri(uriBuilder -> uriBuilder
						.path("/groups/{groupId}/read")
						.queryParam("read", read)
						.build(groupId))
				.retrieve()
				.bodyToMono(Void.class);
	}

	@Override
	public Mono<PaginatedResponse<ChannelMessage>> getMessages(long groupId, int page, int size)
	{
		return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/groups/{groupId}/messages")
						.queryParam("page", page)
						.queryParam("size", size)
						.queryParam("sort", "published,desc")
						.build(groupId))
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<PaginatedResponse<ChannelMessageDTO>>()
				{
				})
				.map(ChannelMapper::fromDTO);
	}

	public Mono<ChannelMessage> getChannelMessage(long messageId)
	{
		return webClient.get()
				.uri("/messages/{messageId}", messageId)
				.retrieve()
				.bodyToMono(ChannelMessageDTO.class)
				.map(ChannelMapper::fromDTO);
	}

	public Mono<Long> createChannelMessage(long channelId, String title, String content, File image, long originalId)
	{
		var builder = new MultipartBodyBuilder();
		if (channelId == 0L)
		{
			throw new IllegalArgumentException("ChannelId is required");
		}
		builder.part("channelId", channelId);
		if (StringUtils.isBlank(title))
		{
			throw new IllegalArgumentException("Title is required");
		}
		builder.part("title", title);
		if (StringUtils.isNotBlank(content))
		{
			builder.part("content", content);
		}
		if (image != null)
		{
			builder.part("image", new FileSystemResource(image));
		}
		if (originalId != 0L)
		{
			builder.part("originalId", originalId);
		}

		return webClient.post()
				.uri("/messages")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(builder.build()))
				.exchangeToMono(ClientUtils::getCreatedId);
	}

	public Mono<Void> updateChannelMessagesRead(Map<Long, Boolean> messages)
	{
		var request = new UpdateChannelMessagesReadRequest(messages);

		return webClient.patch()
				.uri("/messages")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}
}
