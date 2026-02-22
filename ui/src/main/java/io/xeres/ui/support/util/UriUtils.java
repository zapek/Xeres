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

package io.xeres.ui.support.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.InvalidUrlException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.regex.Pattern;

public final class UriUtils
{
	private UriUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Matches at least one alpha and one dot in the string.
	 */
	private static final Pattern ALPHA_CHARACTER = Pattern.compile("^(?=.*[a-z])(?=.*\\.).*$");

	/**
	 * For a URL to be safe, it must:
	 * <ul>
	 * <li>have a https scheme
	 * <li>do not specify a port
	 * <li>have a host with at least a dot and an alphabetical lowercase character
	 * </ul>
	 *
	 * @param url the url to check
	 * @return true if probably safe
	 */
	public static boolean isSafeEnough(String url)
	{
		if (StringUtils.isBlank(url))
		{
			return false;
		}
		try
		{
			var uriComponents = UriComponentsBuilder.fromUriString(url)
					.build();
			var host = uriComponents.getHost();
			var port = uriComponents.getPort();
			if (port != -1)
			{
				return false;
			}
			if ("https".equals(uriComponents.getScheme()) && StringUtils.isNotBlank(host))
			{
				return ALPHA_CHARACTER.matcher(host).matches();
			}
		}
		catch (InvalidUrlException _)
		{
			// Do nothing, we return false
		}
		return false;
	}
}
