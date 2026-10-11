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

package io.xeres.app.api.resolver;

import io.xeres.common.rest.ScrollDirection;
import io.xeres.common.rest.ScrollRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static io.xeres.common.rest.ScrollRequestParameters.*;

/// Allows to resolve ScrollRequest arguments passed to a Controller.
/// The arguments look like this:
/// ```json
/// {
///   "size": 5,
///   "direction": "forward",
///   "lastId": 3969,
///   "lastInstant": "2026-09-12T11:38:49Z"
/// }
/// ```
/// Just pass in the `lastInstant` of the last/first top/bottom of the previous result, as well
/// as its `id`, in case the instants collide.
public class ScrollRequestArgumentResolver implements HandlerMethodArgumentResolver
{
	public static final String KEY_PUBLISHED = "published";
	public static final String KEY_ID = "id";

	private static final int DEFAULT_WINDOW_SIZE = 50;

	@Override
	public boolean supportsParameter(MethodParameter parameter)
	{
		return ScrollRequest.class.equals(parameter.getParameterType());
	}

	@Override
	public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory)
	{
		var size = Integer.parseInt(Optional.ofNullable(webRequest.getParameter(PARAMETER_SIZE)).orElse(String.valueOf(DEFAULT_WINDOW_SIZE)));
		var direction = ScrollDirection.fromString(webRequest.getParameter(PARAMETER_DIRECTION)).orElse(ScrollDirection.FORWARD);
		var lastInstantParameter = webRequest.getParameter(PARAMETER_LAST_INSTANT);
		Instant lastInstant = lastInstantParameter != null ? Instant.parse(lastInstantParameter) : null;
		var lastIdParameter = webRequest.getParameter(PARAMETER_LAST_ID);
		Long id = lastIdParameter != null ? Long.valueOf(lastIdParameter) : null;

		ScrollPosition position;
		if (lastInstant == null || id == null)
		{
			position = ScrollPosition.keyset();
		}
		else
		{
			Map<String, Object> keys = Map.of(KEY_PUBLISHED, lastInstant, KEY_ID, id);
			position = direction == ScrollDirection.BACKWARD
					? ScrollPosition.backward(keys)
					: ScrollPosition.forward(keys);
		}

		// XXX: enable if you want to return from bottom to top
		//var sorting = direction == ScrollDirection.BACKWARD ? Sort.by(Order.asc(KEY_PUBLISHED), Order.asc(KEY_ID)) : Sort.by(Order.desc(KEY_PUBLISHED), Order.desc(KEY_ID));
		var sorting = Sort.by(Order.desc(KEY_PUBLISHED), Order.desc(KEY_ID));

		return new ScrollRequest(position, sorting, Limit.of(size));
	}
}
