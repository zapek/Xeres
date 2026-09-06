/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

import io.xeres.common.dto.chess.ChessGameDTO;
import io.xeres.common.events.StartupEvent;
import io.xeres.common.rest.chess.ChessActionRequest;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.event.ChessGamesEvent;
import jakarta.annotation.PreDestroy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static io.xeres.common.rest.PathConfig.CHESS_PATH;

@Component
public class ChessClient
{
	private final WebClient.Builder builder;
	private final ApplicationEventPublisher publisher;
	private WebClient client;
	private Disposable updates;

	public ChessClient(WebClient.Builder builder, ApplicationEventPublisher publisher)
	{
		this.builder = builder;
		this.publisher = publisher;
	}

	@EventListener
	public void init(StartupEvent unused)
	{
		close();
		client = builder.clone().baseUrl(RemoteUtils.getControlUrl() + CHESS_PATH).build();
		updates = Flux.interval(Duration.ofSeconds(2))
				.onBackpressureDrop()
				.concatMap(_ -> client.get().retrieve().bodyToFlux(ChessGameDTO.class).collectList()
						.timeout(Duration.ofSeconds(10)).onErrorResume(_ -> Mono.empty()))
				.subscribe(games -> publisher.publishEvent(new ChessGamesEvent(games)));
	}

	public Mono<ChessGameDTO> invite(String peer)
	{
		return client.post().uri("/{peer}/invite", peer).retrieve().bodyToMono(ChessGameDTO.class);
	}

	public Mono<ChessGameDTO> action(String peer, String action)
	{
		return client.post().uri("/{peer}/actions", peer).bodyValue(new ChessActionRequest(action))
				.retrieve().bodyToMono(ChessGameDTO.class);
	}

	@PreDestroy
	public void close()
	{
		if (updates != null)
		{
			updates.dispose();
		}
	}
}
