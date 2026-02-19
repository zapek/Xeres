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

package io.xeres.ui.support.oembed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The original file is at <a href="https://oembed.com/providers.json">...</a>. Since entries are free to
 * be added using GitHub's PR, many of them are in there just for the sake of publicity and not
 * popularity. Since checking more than 300 regexp for each URL is expensive, we only keep the
 * most popular entries in the file.
 */
class OEmbedProvider
{
	private static final Logger log = LoggerFactory.getLogger(OEmbedProvider.class);

	private static final String OEMBED_DATABASE = "/oembed-providers.json";

	private Map<Pattern, String> providersMap;

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record OEmbedEntry(
			String providerName,
			String providerUrl,
			List<Endpoint> endpoints
	)
	{
		@JsonIgnoreProperties(ignoreUnknown = true)
		private record Endpoint(
				List<String> schemes,
				String url,
				boolean discovery
		)
		{
		}
	}

	OEmbedProvider(JsonMapper jsonMapper)
	{
		try
		{
			var loadedProviders = jsonMapper.readValue(Objects.requireNonNull(OEmbedProvider.class.getResourceAsStream(OEMBED_DATABASE)),
					new TypeReference<List<OEmbedEntry>>()
					{
					});

			log.debug("Loaded {} oEmbed providers", loadedProviders.size());

			providersMap = new HashMap<>();

			loadedProviders.forEach(provider -> provider.endpoints().forEach(endpoint -> endpoint.schemes().forEach(scheme -> providersMap.put(Pattern.compile(convertSimplePattern(scheme)), endpoint.url()))));
		}
		catch (JacksonException e)
		{
			log.error("Couldn't load oEmbed providers database", e);
			providersMap = Map.of();
		}
	}

	public String getOembedForUrl(String url)
	{
		return providersMap.entrySet().stream()
				.filter(provider -> provider.getKey().matcher(url).matches())
				.findFirst()
				.map(Map.Entry::getValue)
				.orElse("");
	}

	private static String convertSimplePattern(String pattern)
	{
		return pattern
				.replace(".", "\\.")
				.replace("?", "\\?")
				.replace("*", ".*");
	}
}
