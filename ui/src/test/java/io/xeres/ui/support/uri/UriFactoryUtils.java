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

package io.xeres.ui.support.uri;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;

public final class UriFactoryUtils
{
	private UriFactoryUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static UriComponents createUriComponentsFromUri(String url)
	{
		URI uri;
		try
		{
			uri = new URI(url);
		}
		catch (URISyntaxException e)
		{
			throw new RuntimeException(e);
		}
		return UriComponentsBuilder.fromPath(uri.getPath())
				.query(uri.getQuery())
				.build();
	}
}
