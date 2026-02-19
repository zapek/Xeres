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

import io.xeres.common.events.StartupEvent;
import io.xeres.common.rest.statistics.DataCounterStatisticsResponse;
import io.xeres.common.rest.statistics.RttStatisticsResponse;
import io.xeres.common.rest.statistics.TurtleStatisticsResponse;
import io.xeres.common.util.RemoteUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static io.xeres.common.rest.PathConfig.STATISTICS_PATH;

@Component
public class StatisticsClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public StatisticsClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(@SuppressWarnings("unused") StartupEvent event)
	{
		webClient = webClientBuilder.clone()
				.baseUrl(RemoteUtils.getControlUrl() + STATISTICS_PATH)
				.build();
	}

	public Mono<TurtleStatisticsResponse> getTurtleStatistics()
	{
		return webClient.get()
				.uri("/turtle")
				.retrieve()
				.bodyToMono(TurtleStatisticsResponse.class);
	}

	public Mono<RttStatisticsResponse> getRttStatistics()
	{
		return webClient.get()
				.uri("/rtt")
				.retrieve()
				.bodyToMono(RttStatisticsResponse.class);
	}

	public Mono<DataCounterStatisticsResponse> getDataCounterStatistics()
	{
		return webClient.get()
				.uri("/data-counter")
				.retrieve()
				.bodyToMono(DataCounterStatisticsResponse.class);
	}
}
