/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

import org.apache.commons.collections4.ListUtils;

import java.util.List;

/**
 * Paginated response.
 *
 * @param content          the page content as a {@link List}
 * @param page             the page values
 * @param <T>              the element's type
 */
public record PaginatedResponse<T>(
		List<T> content,
		PaginatedPage page
)
{
	/**
	 * The paginated response page values.
	 *
	 * @param totalElements the total amount of elements
	 * @param totalPages    the number of total pages
	 * @param number        the number of the current page. Is always non-negative.
	 * @param size          the size of the page
	 */
	public record PaginatedPage(int totalElements,
	                            int totalPages,
	                            int number,
	                            int size)
	{
	}

	/**
	 * Checks if the page has any content at all.
	 *
	 * @return true if the page has no content at all
	 */
	public boolean empty()
	{
		return ListUtils.emptyIfNull(content).isEmpty();
	}

	/**
	 * Checks if the page is the first one.
	 *
	 * @return true if the page is the first one
	 */
	public boolean first()
	{
		return page.number == 0;
	}

	/**
	 * Checks if the page is the last one.
	 *
	 * @return true if the page is the last one
	 */
	public boolean last()
	{
		return page.number == page.totalPages;
	}

	/**
	 * Gets the number of elements in the page.
	 *
	 * @return the number of elements in the current page.
	 */
	public int numberOfElements()
	{
		return ListUtils.emptyIfNull(content).size();
	}
}
