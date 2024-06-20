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

import io.xeres.common.dto.location.LocationDTO;
import io.xeres.common.events.StartupEvent;
import io.xeres.common.rest.location.RSIdResponse;
import io.xeres.common.rsid.Type;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.model.location.Location;
import io.xeres.ui.model.location.LocationMapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static io.xeres.common.rest.PathConfig.LOCATIONS_PATH;

@Component
public class LocationClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public LocationClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(StartupEvent event)
	{
		webClient = webClientBuilder
				.baseUrl(JavaFxApplication.getControlUrl() + LOCATIONS_PATH)
				.build();
	}

	public Mono<Location> findById(long id)
	{
		return webClient.get()
				.uri("/{id}", id)
				.retrieve()
				.bodyToMono(LocationDTO.class)
				.map(LocationMapper::fromDTO);
	}

	public Mono<RSIdResponse> getRSId(long id, Type type)
	{
		return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/{id}/rsId")
						.queryParam("type", type)
						.build(id))
				.retrieve()
				.bodyToMono(RSIdResponse.class);
	}
}
