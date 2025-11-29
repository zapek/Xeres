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

import io.xeres.common.dto.channel.ChannelGroupDTO;
import io.xeres.common.events.StartupEvent;
import io.xeres.common.rest.channel.CreateChannelGroupRequest;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.model.channel.ChannelGroup;
import io.xeres.ui.model.channel.ChannelMapper;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;

import static io.xeres.common.rest.PathConfig.CHANNELS_PATH;
import static io.xeres.ui.support.util.ClientUtils.fromFile;

@Component
public class ChannelClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public ChannelClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(@SuppressWarnings("unused") StartupEvent event)
	{
		webClient = webClientBuilder
				.baseUrl(RemoteUtils.getControlUrl() + CHANNELS_PATH)
				.build();
	}

	public Flux<ChannelGroup> getChannelGroups()
	{
		return webClient.get()
				.uri("/groups")
				.retrieve()
				.bodyToFlux(ChannelGroupDTO.class)
				.map(ChannelMapper::fromDTO);
	}

	public Mono<Void> createChannelGroup(String name, String description)
	{
		var request = new CreateChannelGroupRequest(name, description);

		return webClient.post()
				.uri("/groups")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<Void> uploadChannelGroupImage(long id, File file)
	{
		return webClient.post()
				.uri("/groups/{id}/image", id)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(fromFile(file)))
				.retrieve()
				.bodyToMono(Void.class);
	}

	// XXX: delete ChannelImage too?
}
