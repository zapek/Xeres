/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public abstract class AbstractControllerTest
{
	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected MockMvc mvc;

	protected MockHttpServletRequestBuilder getJson(String uri)
	{
		return get(uri, APPLICATION_JSON);
	}

	protected MockHttpServletRequestBuilder get(String uri, MediaType mediaType)
	{
		return MockMvcRequestBuilders.get(uri)
				.accept(mediaType);
	}

	protected MockHttpServletRequestBuilder postJson(String uri, Object body)
	{
		var json = objectMapper.writeValueAsString(body);
		return post(uri)
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(json);
	}

	protected MockHttpServletRequestBuilder putJson(String uri, Object body)
	{
		var json = objectMapper.writeValueAsString(body);
		return put(uri)
				.contentType(APPLICATION_JSON)
				.accept(APPLICATION_JSON)
				.content(json);
	}

	protected MockHttpServletRequestBuilder patchJson(String uri, Object body)
	{
		var json = objectMapper.writeValueAsString(body);
		return patch(uri)
				.contentType("application/json-patch+json")
				.accept(APPLICATION_JSON)
				.content(json);
	}
}
