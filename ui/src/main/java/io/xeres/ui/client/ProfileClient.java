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

import io.xeres.common.dto.profile.ProfileDTO;
import io.xeres.common.events.StartupEvent;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.pgp.Trust;
import io.xeres.common.rest.contact.Contact;
import io.xeres.common.rest.profile.ProfileKeyAttributes;
import io.xeres.common.rest.profile.RsIdRequest;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.model.profile.Profile;
import io.xeres.ui.model.profile.ProfileMapper;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static io.xeres.common.dto.profile.ProfileConstants.OWN_PROFILE_ID;
import static io.xeres.common.rest.PathConfig.PROFILES_PATH;

@Component
public class ProfileClient
{
	private final WebClient.Builder webClientBuilder;

	private WebClient webClient;

	public ProfileClient(WebClient.Builder webClientBuilder)
	{
		this.webClientBuilder = webClientBuilder;
	}

	@EventListener
	public void init(StartupEvent event)
	{
		webClient = webClientBuilder
				.baseUrl(RemoteUtils.getControlUrl() + PROFILES_PATH)
				.build();
	}

	public Mono<Void> create(String rsId, int connectionIndex, Trust trust)
	{
		var rsIdRequest = new RsIdRequest(rsId);

		return webClient.post()
				.uri(uriBuilder -> uriBuilder
						.path("")
						.queryParam("connectionIndex", connectionIndex)
						.queryParam("trust", trust.name())
						.build())
				.bodyValue(rsIdRequest)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Flux<Profile> findAll()
	{
		return webClient.get()
				.uri("")
				.retrieve()
				.bodyToFlux(ProfileDTO.class)
				.map(ProfileMapper::fromDTO);
	}

	public Mono<Profile> getOwn()
	{
		return findById(OWN_PROFILE_ID);
	}

	public Mono<Profile> checkRsId(String rsId)
	{
		var rsIdRequest = new RsIdRequest(rsId);

		return webClient.post()
				.uri("/check")
				.bodyValue(rsIdRequest)
				.retrieve()
				.bodyToMono(ProfileDTO.class)
				.map(ProfileMapper::fromDeepDTO);
	}

	public Mono<Profile> findById(long id)
	{
		return webClient.get()
				.uri("/{id}", id)
				.retrieve()
				.bodyToMono(ProfileDTO.class)
				.map(ProfileMapper::fromDeepDTO);
	}

	public Mono<ProfileKeyAttributes> findProfileKeyAttributes(long id)
	{
		return webClient.get()
				.uri("/{id}/key-attributes", id)
				.retrieve()
				.bodyToMono(ProfileKeyAttributes.class);
	}

	public Flux<Contact> findContactsForProfile(long id)
	{
		return webClient.get()
				.uri("/{id}/contacts", id)
				.retrieve()
				.bodyToFlux(Contact.class);
	}

	public Flux<Profile> findByLocationIdentifier(LocationIdentifier locationIdentifier, boolean withLocations)
	{
		return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("")
						.queryParam("locationIdentifier", locationIdentifier.toString())
						.queryParam("withLocations", withLocations)
						.build())
				.retrieve()
				.bodyToFlux(ProfileDTO.class)
				.map(ProfileMapper::fromDeepDTO);
	}

	public Flux<Profile> findByPgpIdentifier(long pgpIdentifier, boolean withLocations)
	{
		return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("")
						.queryParam("pgpIdentifier", Long.toUnsignedString(pgpIdentifier, 16))
						.queryParam("withLocations", withLocations)
						.build())
				.retrieve()
				.bodyToFlux(ProfileDTO.class)
				.map(ProfileMapper::fromDeepDTO);
	}

	public Mono<Void> setTrust(long id, Trust trust)
	{
		return webClient.put()
				.uri("/{id}/trust", id)
				.bodyValue(trust)
				.retrieve()
				.bodyToMono(Void.class);
	}

	public Mono<Void> delete(long id)
	{
		return webClient.delete()
				.uri("/{id}", id)
				.retrieve()
				.bodyToMono(Void.class);
	}
}
