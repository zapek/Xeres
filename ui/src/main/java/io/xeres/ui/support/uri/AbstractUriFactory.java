/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

import io.xeres.common.id.Sha1Sum;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.markdown.UriAction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriUtils;

import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractUriFactory
{
	protected static final String PROTOCOL_RETROSHARE = "retroshare";

	public abstract String getAuthority();

	abstract Content create(UriComponents uriComponents, String text, UriAction uriAction);

	public String getProtocol()
	{
		return PROTOCOL_RETROSHARE;
	}

	protected static String buildUri(String protocol, String authority, String... args)
	{
		var sb = new StringBuilder(protocol);
		var firstArg = true;

		if (args.length % 2 != 0)
		{
			throw new IllegalArgumentException("Wrong number of arguments: must be name and value pairs");
		}
		sb.append("://");
		sb.append(authority);

		for (var i = 0; i < args.length; i += 2)
		{
			if (StringUtils.isNotBlank(args[i + 1]))
			{
				if (firstArg)
				{
					sb.append("?");
					firstArg = false;
				}
				else
				{
					sb.append("&");
				}
				sb.append(args[i]);
				sb.append("=");
				sb.append(UriUtils.encodeQueryParam(args[i + 1], UTF_8));
			}
		}
		return sb.toString();
	}

	protected static long getLongArgument(String s)
	{
		try
		{
			return Long.parseUnsignedLong(s.toLowerCase(Locale.ROOT), 16);
		}
		catch (NumberFormatException e)
		{
			return 0L;
		}
	}

	protected static int getIntArgument(String s)
	{
		try
		{
			return Integer.parseInt(s.toLowerCase(Locale.ROOT), 16);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	protected static Sha1Sum getHashArgument(String s)
	{
		return Sha1Sum.fromString(s);
	}
}
