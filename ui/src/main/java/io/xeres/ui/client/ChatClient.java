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

import io.xeres.common.dto.chat.ChatRoomContextDTO;
import io.xeres.common.id.LocationId;
import io.xeres.common.message.chat.ChatRoomContext;
import io.xeres.common.rest.chat.ChatRoomVisibility;
import io.xeres.common.rest.chat.CreateChatRoomRequest;
import io.xeres.common.rest.chat.InviteToChatRoomRequest;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.model.chat.ChatMapper;
import io.xeres.ui.model.location.Location;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

import static io.xeres.common.rest.PathConfig.CHAT_PATH;

@Component
public class ChatClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public ChatClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@PostConstruct
	private void init()
	{
		webClient = webClientBuilder
				.baseUrl(JavaFxApplication.getControlUrl() + CHAT_PATH)
				.build();
	}

	public Mono<Void> createChatRoom(String name, String topic, ChatRoomVisibility visibility, boolean signedIdentities)
	{
		var request = new CreateChatRoomRequest(name, topic, visibility, signedIdentities);

		return webClient.post()
				.uri("/rooms")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<Long> joinChatRoom(long id)
	{
		return webClient.put()
				.uri(uriBuilder -> uriBuilder
						.path("/rooms/{id}/subscription")
						.build(id))
				.retrieve()
				.bodyToMono(Long.class);
	}

	public Mono<Void> leaveChatRoom(long id)
	{
		return webClient.delete()
				.uri(uriBuilder -> uriBuilder
						.path("/rooms/{id}/subscription")
						.build(id))
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<ChatRoomContext> getChatRoomContext()
	{
		return webClient.get()
				.uri("/rooms")
				.retrieve()
				.bodyToMono(ChatRoomContextDTO.class)
				.map(ChatMapper::fromDTO);
	}

	public Mono<Void> inviteLocationsToChatRoom(long chatRoomId, Set<Location> locations)
	{
		var request = new InviteToChatRoomRequest(chatRoomId, locations.stream()
				.map(Location::getLocationId)
				.map(LocationId::toString)
				.collect(Collectors.toSet()));

		return webClient.post()
				.uri("/rooms/invite")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}
}
