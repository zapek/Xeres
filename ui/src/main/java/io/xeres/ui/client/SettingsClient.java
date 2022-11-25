/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.diff.JsonDiff;
import io.xeres.common.dto.settings.SettingsDTO;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.model.settings.Settings;
import io.xeres.ui.model.settings.SettingsMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static io.xeres.common.rest.PathConfig.SETTINGS_PATH;

@Component
public class SettingsClient
{
	private final WebClient.Builder webClientBuilder;
	private final ObjectMapper objectMapper;

	private WebClient webClient;

	public SettingsClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper)
	{
		this.webClientBuilder = webClientBuilder;
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	private void init()
	{
		webClient = webClientBuilder
				.baseUrl(JavaFxApplication.getControlUrl() + SETTINGS_PATH)
				.build();
	}

	public Mono<Settings> getSettings()
	{
		return webClient.get()
				.uri("")
				.retrieve()
				.bodyToMono(SettingsDTO.class)
				.map(SettingsMapper::fromDTO);
	}

	public Mono<Settings> patchSettings(Settings originalSettings, Settings newSettings)
	{
		var target = objectMapper.convertValue(newSettings, JsonNode.class);
		var source = objectMapper.convertValue(originalSettings, JsonNode.class);
		var patch = JsonDiff.asJsonPatch(source, target);

		return webClient.patch()
				.uri("")
				.contentType(MediaType.valueOf("application/json-patch+json"))
				.bodyValue(patch)
				.retrieve()
				.bodyToMono(SettingsDTO.class)
				.map(SettingsMapper::fromDTO);
	}
}
