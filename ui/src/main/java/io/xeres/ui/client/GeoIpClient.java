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

import io.xeres.common.events.StartupEvent;
import io.xeres.common.rest.geoip.CountryResponse;
import io.xeres.common.util.RemoteUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static io.xeres.common.rest.PathConfig.GEOIP_PATH;

@Component
public class GeoIpClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public GeoIpClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(@SuppressWarnings("unused") StartupEvent event)
	{
		webClient = webClientBuilder.clone()
				.baseUrl(RemoteUtils.getControlUrl() + GEOIP_PATH)
				.build();
	}

	public Mono<CountryResponse> getIsoCountry(String ip)
	{
		return webClient.get()
				.uri("/{ip}", ip)
				.retrieve()
				.bodyToMono(CountryResponse.class);
	}
}
