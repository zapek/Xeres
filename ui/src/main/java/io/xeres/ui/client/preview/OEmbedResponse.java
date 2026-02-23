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

import com.fasterxml.jackson.annotation.JsonProperty;

record OEmbedResponse(
		String type,
		String version,
		String title,
		@JsonProperty("author_name")
		String authorName,
		@JsonProperty("author_url")
		String authorUrl,
		@JsonProperty("provider_name")
		String providerName,
		@JsonProperty("provider_url")
		String providerUrl,
		@JsonProperty("thumbnail_url")
		String thumbnailUrl,
		@JsonProperty("thumbnail_width")
		Integer thumbnailWidth,
		@JsonProperty("thumbnail_height")
		Integer thumbnailHeight
)
{
}
