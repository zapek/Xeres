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

import io.xeres.common.dto.board.BoardGroupDTO;
import io.xeres.common.dto.board.BoardMessageDTO;
import io.xeres.common.events.StartupEvent;
import io.xeres.common.rest.board.UpdateBoardMessagesReadRequest;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.model.board.BoardGroup;
import io.xeres.ui.model.board.BoardMapper;
import io.xeres.ui.model.board.BoardMessage;
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

import static io.xeres.common.rest.PathConfig.BOARDS_PATH;

@Component
public class BoardClient implements GxsGroupClient<BoardGroup>, GxsMessageClient<BoardMessage>
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public BoardClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(@SuppressWarnings("unused") StartupEvent event)
	{
		webClient = webClientBuilder
				.baseUrl(RemoteUtils.getControlUrl() + BOARDS_PATH)
				.build();
	}

	@Override
	public Flux<BoardGroup> getGroups()
	{
		return webClient.get()
				.uri("/groups")
				.retrieve()
				.bodyToFlux(BoardGroupDTO.class)
				.map(BoardMapper::fromDTO);
	}

	public Mono<Long> createBoardGroup(String name, String description, File image)
	{
		var builder = ClientUtils.createGroupBuilder(name, description, image);

		return webClient.post()
				.uri("/groups")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(builder.build()))
				.exchangeToMono(ClientUtils::getCreatedId);
	}

	public Mono<Void> updateBoardGroup(long groupId, String name, String description, File image, boolean updateImage)
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

	public Mono<BoardGroup> getBoardGroupById(long groupId)
	{
		return webClient.get()
				.uri("/groups/{groupId}", groupId)
				.retrieve()
				.bodyToMono(BoardGroupDTO.class)
				.map(BoardMapper::fromDTO);
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
	public Mono<PaginatedResponse<BoardMessage>> getMessages(long groupId, int page, int size)
	{
		return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/groups/{groupId}/messages")
						.queryParam("page", page)
						.queryParam("size", size)
						.queryParam("sort", "published,desc")
						.build(groupId))
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<PaginatedResponse<BoardMessageDTO>>()
				{
				})
				.map(BoardMapper::fromDTO);
	}

	public Mono<BoardMessage> getBoardMessage(long messageId)
	{
		return webClient.get()
				.uri("/messages/{messageId}", messageId)
				.retrieve()
				.bodyToMono(BoardMessageDTO.class)
				.map(BoardMapper::fromDTO);
	}

	public Mono<Long> createBoardMessage(long boardId, String title, String content, String link, File image)
	{
		var builder = new MultipartBodyBuilder();
		if (boardId == 0L)
		{
			throw new IllegalArgumentException("BoardId is required");
		}
		builder.part("boardId", boardId);
		if (StringUtils.isBlank(title))
		{
			throw new IllegalArgumentException("Title is required");
		}
		builder.part("title", title);
		if (StringUtils.isNotBlank(content))
		{
			builder.part("content", content);
		}
		if (StringUtils.isNotBlank(link))
		{
			builder.part("link", link);
		}
		if (image != null)
		{
			builder.part("image", new FileSystemResource(image));
		}

		return webClient.post()
				.uri("/messages")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(builder.build()))
				.exchangeToMono(ClientUtils::getCreatedId);

	}

	public Mono<Void> updateBoardMessagesRead(Map<Long, Boolean> messages)
	{
		var request = new UpdateBoardMessagesReadRequest(messages);

		return webClient.patch()
				.uri("/messages")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}
}
