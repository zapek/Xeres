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

import io.xeres.common.events.StartupEvent;
import io.xeres.common.rest.ScrollDirection;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.model.mail.MailGroup;
import io.xeres.ui.model.mail.MailMessage;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static io.xeres.common.rest.PathConfig.MAIL_PATH;

@Component
public class MailClient implements GxsGroupClient<MailGroup>, GxsMessageClient<MailMessage>
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
	public Flux<MailGroup> getGroups()
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
		return null;
	}

	@Override
	public Mono<WindowedResponse<MailMessage>> getMessages(long groupId, ScrollDirection direction, Instant lastInstant, Long lastId, int size)
	{
		return null;
	}
}
