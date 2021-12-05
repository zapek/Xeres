/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

import io.xeres.common.dto.identity.IdentityDTO;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.model.identity.Identity;
import io.xeres.ui.model.identity.IdentityMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

import static io.xeres.common.rest.PathConfig.IDENTITY_PATH;

@Component
public class IdentityClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public IdentityClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@PostConstruct
	private void init()
	{
		webClient = webClientBuilder
				.baseUrl(JavaFxApplication.getControlUrl() + IDENTITY_PATH)
				.build();
	}

	public Flux<Identity> getIdentities()
	{
		return webClient.get()
				.uri("/")
				.retrieve()
				.bodyToFlux(IdentityDTO.class)
				.map(IdentityMapper::fromDTO);
	}

	public Mono<Identity> findById(long id)
	{
		return webClient.get()
				.uri("/{id}", id)
				.retrieve()
				.bodyToMono(IdentityDTO.class)
				.map(IdentityMapper::fromDTO);
	}
}
