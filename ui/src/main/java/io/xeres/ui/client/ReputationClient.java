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
import io.xeres.common.id.GxsId;
import io.xeres.common.reputation.Opinion;
import io.xeres.common.rest.reputation.ReputationRequest;
import io.xeres.common.rest.reputation.ReputationResponse;
import io.xeres.common.util.RemoteUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static io.xeres.common.rest.PathConfig.REPUTATION_PATH;

@Component
public class ReputationClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public ReputationClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(@SuppressWarnings("unused") StartupEvent event)
	{
		webClient = webClientBuilder.clone()
				.baseUrl(RemoteUtils.getControlUrl() + REPUTATION_PATH)
				.build();
	}

	public Mono<Void> setReputation(GxsId gxsId, Opinion opinion)
	{
		var reputationRequest = new ReputationRequest(gxsId, opinion);

		return webClient.post()
				.bodyValue(reputationRequest)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<ReputationResponse> getReputation(GxsId gxsId)
	{
		return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("")
						.queryParam("gxsId", gxsId)
						.build())
				.retrieve()
				.bodyToMono(ReputationResponse.class);
	}
}
