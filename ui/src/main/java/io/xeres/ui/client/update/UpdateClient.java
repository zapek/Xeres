/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.client.update;

import io.xeres.common.events.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

@Component
public class UpdateClient
{
	private static final Logger log = LoggerFactory.getLogger(UpdateClient.class);

	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public UpdateClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(StartupEvent event)
	{
		webClient = webClientBuilder
				.baseUrl("https://api.github.com/repos/zapek/Xeres")
				.defaultHeaders(HttpHeaders::clear) // Do not let GitHub know our user/password
				.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true))) // XXX: this is needed if we want to follow redirects! which github uses...
				.build();
	}

	public Mono<ReleaseResponse> getLatestVersion()
	{
		return webClient.get()
				.uri("/releases/latest")
				.retrieve()
				.bodyToMono(ReleaseResponse.class);
	}

	public Mono<Void> downloadFile(String url, Path destination)
	{
		var dataBufferFlux = webClient.get()
				.uri(url)
				.accept(MediaType.APPLICATION_OCTET_STREAM)
				.retrieve()
				.bodyToFlux(DataBuffer.class);

		log.debug("Downloading file {} to {}", url, destination);

		return DataBufferUtils.write(dataBufferFlux, destination, StandardOpenOption.WRITE);
	}

	public Mono<byte[]> downloadFile(String url)
	{
		return webClient.get()
				.uri(url)
				.accept(MediaType.APPLICATION_OCTET_STREAM)
				.retrieve()
				.bodyToMono(byte[].class);
	}

	public Flux<DataBuffer> downloadFileWithProgress(String url, Path destination, Consumer<UpdateProgress> progress)
	{
		var updateProgress = new UpdateProgress(destination, progress);

		var dataBufferFlux = webClient.get()
				.uri(url)
				.accept(MediaType.APPLICATION_OCTET_STREAM)
				.exchangeToFlux(response -> {
					long contentLength = response.headers().contentLength().orElse(-1);
					updateProgress.setContentLength(contentLength);
					return response.bodyToFlux(DataBuffer.class);
				});

		return DataBufferUtils.write(dataBufferFlux, updateProgress.getOutputStream())
				.doOnNext(DataBufferUtils::release);
	}
}
