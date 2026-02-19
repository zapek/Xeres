/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

import io.xeres.common.dto.share.ShareDTO;
import io.xeres.common.events.StartupEvent;
import io.xeres.common.rest.share.TemporaryShareRequest;
import io.xeres.common.rest.share.TemporaryShareResponse;
import io.xeres.common.rest.share.UpdateShareRequest;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.model.share.Share;
import io.xeres.ui.model.share.ShareMapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static io.xeres.common.rest.PathConfig.SHARES_PATH;

@Component
public class ShareClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;


	public ShareClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(@SuppressWarnings("unused") StartupEvent event)
	{
		webClient = webClientBuilder.clone()
				.baseUrl(RemoteUtils.getControlUrl() + SHARES_PATH)
				.build();
	}

	public Flux<Share> findAll()
	{
		return webClient.get()
				.uri("")
				.retrieve()
				.bodyToFlux(ShareDTO.class)
				.map(ShareMapper::fromDTO);
	}

	public Mono<Void> createAndUpdate(List<Share> shares)
	{
		var request = new UpdateShareRequest(ShareMapper.toDTOs(shares));

		return webClient.post()
				.uri("")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<TemporaryShareResponse> createTemporaryShare(String filePath)
	{
		var request = new TemporaryShareRequest(filePath);

		return webClient.post()
				.uri("/temporary")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(TemporaryShareResponse.class);
	}
}
