/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

import java.util.List;

/**
 * Paginated response.
 *
 * @param content          the page content as a {@link List}
 * @param totalElements    the total amount of elements
 * @param totalPages       the number of total pages
 * @param empty            if the page has no content at all
 * @param first            if the page is the first one
 * @param last             if the page is the last one
 * @param number           the number of the current page. Is always non-negative.
 * @param size             the size of the page
 * @param numberOfElements the number of elements currently on this page
 * @param <T>              the element's type
 */
public record PaginatedResponse<T>(
		List<T> content,
		int totalElements,
		int totalPages,
		boolean empty,
		boolean first,
		boolean last,
		int number,
		int size,
		int numberOfElements
)
{
}
