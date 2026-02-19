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

package io.xeres.ui.client.preview;

import io.xeres.common.events.StartupEvent;
import io.xeres.ui.support.oembed.OEmbedService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.stream.Collectors;

@Component
public class PreviewClient
{
	private static final Logger log = LoggerFactory.getLogger(PreviewClient.class);

	private static final int HEAD_RANGE = 32768;

	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;
	private final OEmbedService oembedService;

	public PreviewClient(WebClient.Builder webClientBuilder, OEmbedService oembedService)
	{
		this.webClientBuilder = webClientBuilder;
		this.oembedService = oembedService;
	}

	@EventListener
	public void init(@SuppressWarnings("unused") StartupEvent event)
	{
		webClient = webClientBuilder.clone()
				.defaultHeaders(HttpHeaders::clear) // Do not let remote sites know our credentials
				.clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true))) // Follow redirects
				.build();
	}

	/**
	 * Gets information about a URL.
	 *
	 * @param url the URL
	 * @return the information
	 */
	public Mono<PreviewResponse> getPreview(String url)
	{
		var oEmbedUrl = oEmbedUrl(url);

		if (StringUtils.isEmpty(oEmbedUrl))
		{
			return getOpenGraph(url);
		}
		else
		{
			return getOEmbed(url);
		}
	}

	private String oEmbedUrl(String url)
	{
		return oembedService.getOembedForUrl(url);
	}


	/**
	 * Gets information about a URL using the {@link <a href="https://ogp.me/">OpenGraph protocol</a>}
	 *
	 * @param url the URL
	 * @return the information
	 */
	private Mono<PreviewResponse> getOpenGraph(String url)
	{
		return webClient.get()
				.uri(url)
				.accept(MediaType.TEXT_HTML)
				.header("Range", String.format("bytes=%d-%d", 0, HEAD_RANGE))
				.header("Accept-Encoding", "identity") // No compression
				.exchangeToMono(response -> {
					if (response.statusCode().is2xxSuccessful())
					{
						return response.bodyToMono(String.class);
					}
					return Mono.error(new RuntimeException("Server doesn't support range requests"));
				})
				.map(s -> toPreviewResponse(s, url));
	}

	/**
	 * Gets information about a URL using the {@link <a href="https://oembed.com/">oEmbed protocol</a>}
	 *
	 * @param url the URL
	 * @return the information
	 */
	private Mono<PreviewResponse> getOEmbed(String url)
	{
		return webClient.get()
				.uri(url)
				.accept(MediaType.APPLICATION_JSON) // We don't want the XML variant...
				.retrieve()
				.bodyToMono(OEmbedResponse.class)
				.map(PreviewClient::toPreviewResponse);
	}

	private static PreviewResponse toPreviewResponse(String content, String url)
	{
		int headEnd = content.indexOf("</head>");

		if (headEnd != -1)
		{
			var document = Jsoup.parse(content.substring(0, headEnd + 7), url);
			var metaElements = document.select("meta");
			var ogs = metaElements.stream()
					.filter(element -> element.attr("property").startsWith("og:"))
					.collect(Collectors.toMap(element -> element.attr("property"), element -> element.attr("content")));
			return new PreviewResponse(
					ogs.getOrDefault("og:title", ""),
					ogs.getOrDefault("og:description", ""),
					ogs.getOrDefault("og:site_name", ""),
					ogs.getOrDefault("og:image", ""),
					NumberUtils.toInt(ogs.getOrDefault("og:image:width", "0")),
					NumberUtils.toInt(ogs.getOrDefault("og:image:height", "0")));
		}
		return PreviewResponse.EMPTY;
	}

	private static PreviewResponse toPreviewResponse(OEmbedResponse response)
	{
		return new PreviewResponse(
				response.title(),
				"",
				response.providerName(),
				response.thumbnailUrl(),
				response.thumbnailWidth(),
				response.thumbnailHeight()
		);
	}
}
