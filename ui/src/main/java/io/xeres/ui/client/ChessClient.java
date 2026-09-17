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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static io.xeres.common.rest.PathConfig.CHESS_PATH;

@Component
public class ChessClient
{
	private final WebClient.Builder builder;
	private WebClient client;

	public ChessClient(WebClient.Builder builder)
	{
		this.builder = builder;
	}

	@EventListener
	public void init(StartupEvent unused)
	{
		client = builder.clone().baseUrl(RemoteUtils.getControlUrl() + CHESS_PATH).build();
	}

	public Mono<java.util.List<ChessGameDTO>> games()
	{
		if (client == null)
		{
			return Mono.just(java.util.List.of());
		}
		return client.get().uri("").retrieve().bodyToFlux(ChessGameDTO.class).collectList();
	}

	public Mono<java.util.List<io.xeres.common.dto.chess.ChessContactDTO>> contacts()
	{
		if (client == null)
		{
			return Mono.just(java.util.List.of());
		}
		return client.get().uri("/contacts").retrieve().bodyToFlux(io.xeres.common.dto.chess.ChessContactDTO.class).collectList();
	}

	public Mono<Void> addContact(String peer)
	{
		if (client == null)
		{
			return Mono.empty();
		}
		return client.post().uri("/contacts/{peer}", peer).retrieve().bodyToMono(Void.class);
	}

	public Mono<Void> removeContact(String peer)
	{
		if (client == null)
		{
			return Mono.empty();
		}
		return client.delete().uri("/contacts/{peer}", peer).retrieve().bodyToMono(Void.class);
	}

	public Mono<java.util.List<io.xeres.common.dto.chess.ChessLeaderboardEntryDTO>> leaderboard()
	{
		if (client == null)
		{
			return Mono.just(java.util.List.of());
		}
		return client.get().uri("/leaderboard").retrieve().bodyToFlux(io.xeres.common.dto.chess.ChessLeaderboardEntryDTO.class).collectList();
	}

	public Mono<io.xeres.common.dto.chess.ChessLeaderboardEntryDTO> rating(String peer)
	{
		if (client == null)
		{
			return Mono.empty();
		}
		return client.get().uri("/ratings/{peer}", peer).retrieve().bodyToMono(io.xeres.common.dto.chess.ChessLeaderboardEntryDTO.class);
	}

	public Mono<java.util.List<io.xeres.common.dto.chess.ChessHistorySummaryDTO>> history()
	{
		if (client == null)
		{
			return Mono.just(java.util.List.of());
		}
		return client.get().uri("/history").retrieve().bodyToFlux(io.xeres.common.dto.chess.ChessHistorySummaryDTO.class).collectList();
	}

	public Mono<ChessGameDTO> history(String id)
	{
		if (client == null)
		{
			return Mono.empty();
		}
		return client.get().uri("/history/{id}", id).retrieve().bodyToMono(ChessGameDTO.class);
	}

	public Mono<ChessGameDTO> invite(String peer)
	{
		if (client == null)
		{
			return Mono.empty();
		}
		return client.post().uri("/{peer}/invite", peer).retrieve().bodyToMono(ChessGameDTO.class);
	}

	public Mono<ChessGameDTO> action(String peer, String action)
	{
		if (client == null)
		{
			return Mono.empty();
		}
		return client.post().uri("/{peer}/actions", peer).bodyValue(new ChessActionRequest(action))
				.retrieve().bodyToMono(ChessGameDTO.class);
	}

	public Mono<Boolean> isBusy()
	{
		if (client == null)
		{
			return Mono.just(false);
		}
		return client.get().uri("/busy").retrieve().bodyToMono(Boolean.class);
	}

	public Mono<Void> setBusy(boolean busy)
	{
		if (client == null)
		{
			return Mono.empty();
		}
		return client.post().uri(uriBuilder -> uriBuilder.path("/busy").queryParam("busy", busy).build())
				.retrieve().bodyToMono(Void.class);
	}
}
