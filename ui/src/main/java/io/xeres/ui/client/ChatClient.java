/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

import io.xeres.common.dto.chat.ChatBacklogDTO;
import io.xeres.common.dto.chat.ChatRoomBacklogDTO;
import io.xeres.common.dto.chat.ChatRoomContextDTO;
import io.xeres.common.dto.location.LocationDTO;
import io.xeres.common.events.StartupEvent;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.message.chat.ChatBacklog;
import io.xeres.common.message.chat.ChatRoomBacklog;
import io.xeres.common.message.chat.ChatRoomContext;
import io.xeres.common.rest.chat.ChatRoomVisibility;
import io.xeres.common.rest.chat.CreateChatRoomRequest;
import io.xeres.common.rest.chat.DistantChatRequest;
import io.xeres.common.rest.chat.InviteToChatRoomRequest;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.model.chat.ChatMapper;
import io.xeres.ui.model.location.Location;
import io.xeres.ui.model.location.LocationMapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
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

	@EventListener
	public void init(@SuppressWarnings("unused") StartupEvent event)
	{
		webClient = webClientBuilder
				.baseUrl(RemoteUtils.getControlUrl() + CHAT_PATH)
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

	public Mono<Location> createDistantChat(long identityId)
	{
		var request = new DistantChatRequest(identityId);

		return webClient.post()
				.uri("/distant-chats")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(LocationDTO.class)
				.map(LocationMapper::fromDTO);
	}

	public Mono<Void> closeDistantChat(long identityId)
	{
		return webClient.delete()
				.uri("/distant-chats/{id}", identityId)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<Void> joinChatRoom(long id)
	{
		return webClient.put()
				.uri("/rooms/{id}/subscription", id)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<Void> leaveChatRoom(long id)
	{
		return webClient.delete()
				.uri("/rooms/{id}/subscription", id)
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
				.map(Location::getLocationIdentifier)
				.map(LocationIdentifier::toString)
				.collect(Collectors.toSet()));

		return webClient.post()
				.uri("/rooms/invite")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Flux<ChatRoomBacklog> getChatRoomBacklog(long id)
	{
		return webClient.get()
				.uri("/rooms/{roomId}/messages", id)
				.retrieve()
				.bodyToFlux(ChatRoomBacklogDTO.class)
				.map(ChatMapper::fromDTO);
	}

	public Mono<Void> deleteChatRoomBacklog(long id)
	{
		return webClient.delete()
				.uri("/rooms/{roomId}/messages", id)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Flux<ChatBacklog> getChatBacklog(long id)
	{
		return webClient.get()
				.uri("/chats/{locationId}/messages", id)
				.retrieve()
				.bodyToFlux(ChatBacklogDTO.class)
				.map(ChatMapper::fromDTO);
	}

	public Mono<Void> deleteChatBacklog(long id)
	{
		return webClient.delete()
				.uri("/chats/{locationId}/messages", id)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Flux<ChatBacklog> getDistantChatBacklog(long id)
	{
		return webClient.get()
				.uri("/distant-chats/{identityId}/messages", id)
				.retrieve()
				.bodyToFlux(ChatBacklogDTO.class)
				.map(ChatMapper::fromDTO);
	}

	public Mono<Void> deleteDistantChatBacklog(long id)
	{
		return webClient.delete()
				.uri("/distant-chats/{identityId}/messages", id)
				.retrieve()
				.bodyToMono(Void.class);
	}
}
