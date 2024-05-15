/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

import io.xeres.common.rest.file.FileSearchRequest;
import io.xeres.common.rest.file.FileSearchResponse;
import io.xeres.ui.JavaFxApplication;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static io.xeres.common.rest.PathConfig.FILES_PATH;

@Component
public class FileClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public FileClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@PostConstruct
	private void init()
	{
		webClient = webClientBuilder
				.baseUrl(JavaFxApplication.getControlUrl() + FILES_PATH)
				.build();
	}

	public Mono<FileSearchResponse> search(String name)
	{
		var request = new FileSearchRequest(name);

		return webClient.post()
				.uri("/search")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(FileSearchResponse.class);
	}
}
