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

import io.xeres.common.AppName;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.markdown.LinkAction;
import org.springframework.web.util.UriComponents;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CertificateContentParser implements ContentParser
{
	public static final String PARAMETER_RADIX = "radix";
	public static final String PARAMETER_NAME = "name";
	public static final String PARAMETER_LOCATION = "location";

	private static final String AUTHORITY = "certificate";

	@Override
	public String getProtocol()
	{
		return PROTOCOL_RETROSHARE;
	}

	@Override
	public String getAuthority()
	{
		return AUTHORITY;
	}

	@Override
	public Content parse(UriComponents uriComponents, String text, LinkAction linkAction)
	{
		var radix = uriComponents.getQueryParams().getFirst(PARAMETER_RADIX);
		var name = uriComponents.getQueryParams().getFirst(PARAMETER_NAME);
		var location = uriComponents.getQueryParams().getFirst(PARAMETER_LOCATION);

		if (isBlank(radix))
		{
			return ContentText.EMPTY;
		}

		return new ContentUri(defaultString(radix), text, JavaFxApplication::addPeer);
	}

	public static String generate(String radix, String name, String location)
	{
		var uri = ContentParser.buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_RADIX, radix,
				PARAMETER_NAME, name,
				PARAMETER_LOCATION, location);

		return "<a href=\"" + uri + "\">" + AppName.NAME + " Certificate (" + name + ", @" + location + ")</a>";
	}
}
