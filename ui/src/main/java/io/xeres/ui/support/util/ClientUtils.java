/*
 * Copyright (c) 2023-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.File;

public final class ClientUtils
{
	private ClientUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static MultiValueMap<String, HttpEntity<?>> fromFile(File file)
	{
		var builder = new MultipartBodyBuilder();
		builder.part("file", new FileSystemResource(file));
		return builder.build();
	}

	/**
	 * Gets the ID from a client's POST creation request by using the Location: header.
	 *
	 * @param response the response of the WebClient
	 * @return the ID
	 */
	public static Mono<Long> getCreatedId(ClientResponse response)
	{
		if (response.statusCode().is2xxSuccessful())
		{
			var location = response.headers().asHttpHeaders().getLocation();

			if (location != null)
			{
				var uriComponents = UriComponentsBuilder.fromUri(location).build();
				String lastPathSegment = uriComponents.getPathSegments().getLast();

				try
				{
					return Mono.just(Long.parseLong(lastPathSegment));
				}
				catch (NumberFormatException e)
				{
					return Mono.error(new IllegalArgumentException("Failed to parse ID from location header: " + location, e));
				}
			}
			return Mono.error(new IllegalArgumentException("Location header not found in response"));
		}
		else
		{
			return Mono.error(new IllegalStateException("Failed to create resource, status: " + response.statusCode()));
		}
	}

	public static MultipartBodyBuilder createGroupBuilder(String name, String description, File image)
	{
		var builder = new MultipartBodyBuilder();
		if (StringUtils.isBlank(name))
		{
			throw new IllegalArgumentException("Name is required");
		}
		builder.part("name", name);
		if (StringUtils.isBlank(description))
		{
			throw new IllegalArgumentException("Description is required");
		}
		builder.part("description", description);
		if (image != null)
		{
			builder.part("image", new FileSystemResource(image));
		}
		return builder;
	}
}
