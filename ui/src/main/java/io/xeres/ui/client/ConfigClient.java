/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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
import io.xeres.common.location.Availability;
import io.xeres.common.rest.config.*;
import io.xeres.common.util.RemoteUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Set;

import static io.xeres.common.rest.PathConfig.CONFIG_PATH;
import static io.xeres.ui.support.util.ClientUtils.fromFile;

@Component
public class ConfigClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public ConfigClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(StartupEvent event)
	{
		webClient = webClientBuilder
				.baseUrl(RemoteUtils.getControlUrl() + CONFIG_PATH)
				.build();
	}

	public Mono<Void> createProfile(String name)
	{
		var profileRequest = new OwnProfileRequest(name);

		return webClient.post()
				.uri("/profile")
				.bodyValue(profileRequest)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<Void> createLocation(String name)
	{
		var locationRequest = new OwnLocationRequest(name);

		return webClient.post()
				.uri("/location")
				.bodyValue(locationRequest)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<Void> createIdentity(String name, boolean anonymous)
	{
		var identityRequest = new OwnIdentityRequest(name, anonymous);

		return webClient.post()
				.uri("/identity")
				.bodyValue(identityRequest)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<Void> changeAvailability(Availability availability)
	{
		return webClient.put()
				.uri("/location/availability")
				.bodyValue(availability)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<IpAddressResponse> getExternalIpAddress()
	{
		return webClient.get()
				.uri("/external-ip")
				.retrieve()
				.bodyToMono(IpAddressResponse.class);
	}

	public Mono<IpAddressResponse> getInternalIpAddress()
	{
		return webClient.get()
				.uri("/internal-ip")
				.retrieve()
				.bodyToMono(IpAddressResponse.class);
	}

	public Mono<HostnameResponse> getHostname()
	{
		return webClient.get()
				.uri("/hostname")
				.retrieve()
				.bodyToMono(HostnameResponse.class);
	}

	public Mono<UsernameResponse> getUsername()
	{
		return webClient.get()
				.uri("/username")
				.retrieve()
				.bodyToMono(UsernameResponse.class);
	}

	public Mono<Set<String>> getCapabilities()
	{
		return webClient.get()
				.uri("/capabilities")
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<>()
				{
				});
	}

	public Flux<DataBuffer> getBackup()
	{
		return webClient.get()
				.uri("/export")
				.retrieve()
				.bodyToFlux(DataBuffer.class);
	}

	public Mono<Void> sendBackup(File file)
	{
		return webClient.post()
				.uri("/import")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(fromFile(file)))
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<Void> sendRsKeyring(File file, String locationName, String password)
	{
		return webClient.post()
				.uri(uriBuilder -> uriBuilder
						.path("/import-profile-from-rs")
						.queryParam("locationName", locationName)
						.queryParam("password", password)
						.build())
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(fromFile(file)))
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<ImportRsFriendsResponse> sendRsFriends(File file)
	{
		return webClient.post()
				.uri("/import-friends-from-rs")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(fromFile(file)))
				.retrieve()
				.bodyToMono(ImportRsFriendsResponse.class);
	}

	public Mono<Boolean> verifyUpdate(String filePath, byte[] signature)
	{
		var request = new VerifyUpdateRequest(filePath, signature);

		return webClient.post()
				.uri("/verify-update")
				.bodyValue(request)
				.retrieve()
				.bodyToMono(Boolean.class);
	}
}
