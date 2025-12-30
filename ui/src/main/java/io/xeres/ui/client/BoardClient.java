/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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
import io.xeres.common.rest.board.CreateBoardGroupRequest;
import io.xeres.common.rest.board.CreateBoardMessageRequest;
import io.xeres.common.rest.board.UpdateBoardMessagesReadRequest;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.model.board.BoardGroup;
import io.xeres.ui.model.board.BoardMapper;
import io.xeres.ui.model.board.BoardMessage;
import io.xeres.ui.support.util.ClientUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Map;

import static io.xeres.common.rest.PathConfig.BOARDS_PATH;
import static io.xeres.ui.support.util.ClientUtils.fromFile;

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

	public Mono<Long> createBoardGroup(String name, String description)
	{
		var request = new CreateBoardGroupRequest(name, description);

		return webClient.post()
				.uri("/groups")
				.bodyValue(request)
				.exchangeToMono(ClientUtils::getCreatedId);
	}

	public Mono<Void> uploadBoardGroupImage(long id, File file)
	{
		return webClient.post()
				.uri("/groups/{id}/image", id)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(fromFile(file)))
				.retrieve()
				.bodyToMono(Void.class);
	}

	// XXX: delete boardImage too?

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

	public Mono<Void> createBoardMessage(long boardId, String title, String content, String link, long parentId, long originalId)
	{
		var request = new CreateBoardMessageRequest(boardId, title, content, link, parentId, originalId);

		return webClient.post()
				.uri("/messages")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<Void> uploadBoardMessageImage(long id, File file)
	{
		return webClient.post()
				.uri("/messages/{id}/image", id)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(fromFile(file)))
				.retrieve()
				.bodyToMono(Void.class);
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
