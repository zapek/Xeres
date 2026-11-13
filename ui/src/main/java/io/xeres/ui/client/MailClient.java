/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

import io.xeres.common.dto.FileDTO;
import io.xeres.common.dto.mail.MailMessageDTO;
import io.xeres.common.events.StartupEvent;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.mail.MailType;
import io.xeres.common.rest.ScrollDirection;
import io.xeres.common.rest.mail.CreateMailMessageRequest;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.controller.mail.MailFolder;
import io.xeres.ui.model.mail.MailMapper;
import io.xeres.ui.model.mail.MailMessage;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static io.xeres.common.rest.PathConfig.MAIL_PATH;
import static io.xeres.common.rest.ScrollRequestParameters.*;

@Component
public class MailClient implements GxsGroupClient<MailFolder>, GxsMessageClient<MailMessage>
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public MailClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(@SuppressWarnings("unused") StartupEvent event)
	{
		webClient = webClientBuilder.clone()
				.baseUrl(RemoteUtils.getControlUrl() + MAIL_PATH)
				.build();
	}

	@Override
	public Flux<MailFolder> getGroups()
	{
		return null; // Not used
	}

	@Override
	public Mono<Integer> getUnreadCount(long groupId)
	{
		return null;
	}

	@Override
	public Mono<Void> subscribeToGroup(long groupId)
	{
		return null; // Not used
	}

	@Override
	public Mono<Void> unsubscribeFromGroup(long groupId)
	{
		return null; // Not used
	}

	@Override
	public Mono<Void> setGroupMessagesReadState(long groupId, boolean read)
	{
		return null;
	}

	@Override
	public Mono<PaginatedResponse<MailMessage>> getMessages(long groupId, int page, int size)
	{
		return webClient.get()
				.uri(uriBuilder -> uriBuilder.path("/folders/{mailType}/messages")
						.queryParam("page", page)
						.queryParam("size", size)
						.queryParam("sort", "published,desc")
						.build(MailType.fromGroupId(groupId)))
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<PaginatedResponse<MailMessageDTO>>()
				{
				})
				.map(MailMapper::fromDTO);
	}

	@Override
	public Mono<WindowedResponse<MailMessage>> getMessages(long groupId, ScrollDirection direction, Instant lastInstant, Long lastId, int size)
	{
		return webClient.get()
				.uri(uriBuilder -> uriBuilder.path("/folders/{mailType}/messages")
						.queryParam(PARAMETER_DIRECTION, direction.name().toLowerCase(Locale.ROOT))
						.queryParam(PARAMETER_LAST_INSTANT, lastInstant)
						.queryParam(PARAMETER_LAST_ID, lastId)
						.queryParam(PARAMETER_SIZE, size)
						.build(MailType.fromGroupId(groupId)))
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<WindowedResponse<MailMessageDTO>>()
				{
				})
				.map(MailMapper::fromDTO);
	}

	public Mono<MailMessage> getMessage(long messageId)
	{
		return webClient.get()
				.uri("/messages/{messageId}", messageId)
				.retrieve()
				.bodyToMono(MailMessageDTO.class)
				.map(MailMapper::fromDTO);

	}

	// XXX: not sure about the FileDTO...
	public Mono<Void> createMailMessage(Set<LocationIdentifier> locationsTo, Set<LocationIdentifier> locationsCc, Set<LocationIdentifier> locationsBcc, Set<GxsId> identitiesTo, Set<GxsId> identitiesCc, Set<GxsId> identitiesBcc, String subject, String content, List<FileDTO> files)
	{
		var request = new CreateMailMessageRequest(locationsTo, locationsCc, locationsBcc, identitiesTo, identitiesCc, identitiesBcc, subject, content, files);

		return webClient.post()
				.uri("/messages")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}
}
