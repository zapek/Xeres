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

package io.xeres.ui.client;

import io.xeres.common.rest.ScrollDirection;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;

import static io.xeres.common.rest.ScrollRequestParameters.*;

final class WindowedRequest
{
	private WindowedRequest()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static URI buildRequest(UriBuilder uriBuilder, String path, long groupId, ScrollDirection direction, Instant lastInstant, Long lastId, int size)
	{
		return uriBuilder
				.path(path)
				.queryParam(PARAMETER_DIRECTION, direction.name().toLowerCase(Locale.ROOT))
				.queryParam(PARAMETER_LAST_INSTANT, lastInstant)
				.queryParam(PARAMETER_LAST_ID, lastId)
				.queryParam(PARAMETER_SIZE, size)
				.build(groupId);
	}
}
