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
import io.xeres.ui.support.util.ClientUtils;
import io.xeres.ui.support.util.UriUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
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
				.defaultHeader(HttpHeaders.USER_AGENT, ClientUtils.GENERAL_USER_AGENT)
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
		if (!UriUtils.isSafeEnough(url))
		{
			return Mono.just(PreviewResponse.EMPTY);
		}
		var oEmbedUrl = oEmbedUrl(url);

		if (StringUtils.isEmpty(oEmbedUrl))
		{
			return getOpenGraph(url);
		}
		else
		{
			return getOEmbed(oEmbedUrl, url);
		}
	}

	public Mono<byte[]> getImage(String url)
	{
		return webClient.get()
				.uri(url)
				.accept(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG)
				.retrieve()
				.bodyToMono(byte[].class);
	}

	private String oEmbedUrl(String url)
	{
		return oembedService.getOembedForUrl(url);
	}


	/**
	 * Gets information about a URL using the <a href="https://ogp.me/">OpenGraph protocol</a>
	 *
	 * @param url the URL
	 * @return the information
	 */
	private Mono<PreviewResponse> getOpenGraph(String url)
	{
		return webClient.get()
				.uri(url)
				.accept(MediaType.TEXT_HTML)
				.header(HttpHeaders.RANGE, String.format("bytes=%d-%d", 0, HEAD_RANGE))
				.header(HttpHeaders.ACCEPT_ENCODING, "identity") // No compression
				.exchangeToMono(response -> {
					if (response.statusCode() != HttpStatus.PARTIAL_CONTENT)
					{
						log.debug("Server returned full content to our range request, truncating response...");
					}
					// Most servers don't support range requests for dynamic content so we need to truncate.
					return response.bodyToFlux(DataBuffer.class)
							.collect(() -> new SizeLimitingCollector(HEAD_RANGE),
									SizeLimitingCollector::add)
							.map(collector -> new String(collector.getResult(), StandardCharsets.UTF_8));
				})
				.publishOn(Schedulers.boundedElastic()) // Because we might block to fetch the possible oembed link
				.map(s -> toPreviewResponse(s, url));
	}

	/**
	 * Gets information about a URL using the <a href="https://oembed.com/">oEmbed protocol</a>
	 *
	 * @param oembedUrl the URL
	 * @return the information
	 */
	private Mono<PreviewResponse> getOEmbed(String oembedUrl, String url)
	{
		return webClient.get()
				.uri(_ -> UriComponentsBuilder.fromUriString(oembedUrl)
						.queryParam("format", "json")
						.queryParam("url", url)
						.build().toUri())
				.accept(MediaType.APPLICATION_JSON) // We don't want the XML variant...
				.retrieve()
				.bodyToMono(OEmbedResponse.class)
				.map(PreviewClient::toPreviewResponse);
	}

	private PreviewResponse toPreviewResponse(String content, String url)
	{
		// No need to check for <head> and </head>, Jsoup can parse partial tags fine
		var document = Jsoup.parse(content, url);
		var metaElements = document.select("meta");
		var ogs = metaElements.stream()
				.filter(element -> element.attr("property").startsWith("og:"))
				.collect(Collectors.toMap(element -> element.attr("property"), element -> element.attr("content")));
		var previewResponse = new PreviewResponse(
				ogs.getOrDefault("og:title", ""),
				ogs.getOrDefault("og:description", ""),
				ogs.getOrDefault("og:site_name", ""),
				ogs.getOrDefault("og:image", ""),
				NumberUtils.toInt(ogs.getOrDefault("og:image:width", "0")),
				NumberUtils.toInt(ogs.getOrDefault("og:image:height", "0")));

		if (!previewResponse.hasThumbnail())
		{
			// No thumbnail? Try to find an oEmbed link in the head
			var linkElements = document.select("link");
			var oembedLink = linkElements.stream()
					.filter(element -> element.attr("type").equals("application/json+oembed") && !element.attr("href").isBlank())
					.map(element -> element.attr("href"))
					.findFirst().orElse(null);

			if (oembedLink != null && UriUtils.isSafeEnough(oembedLink))
			{
				return getOEmbed(oembedLink, url).block();
			}
		}
		return previewResponse;
	}

	private static PreviewResponse toPreviewResponse(OEmbedResponse response)
	{
		return new PreviewResponse(
				HtmlUtils.htmlUnescape(StringUtils.defaultString(response.title())),
				"",
				HtmlUtils.htmlUnescape(StringUtils.defaultString(response.providerName())),
				HtmlUtils.htmlUnescape(StringUtils.defaultString(response.thumbnailUrl())),
				response.thumbnailWidth() != null ? response.thumbnailWidth() : 0,
				response.thumbnailHeight() != null ? response.thumbnailHeight() : 0
		);
	}
}
