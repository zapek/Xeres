/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.markdown.UriAction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;

import static io.xeres.ui.support.uri.CertificateUri.*;

public class CertificateUriFactory extends AbstractUriFactory
{
	@Override
	public String getAuthority()
	{
		return AUTHORITY;
	}

	@Override
	public Content create(UriComponents uriComponents, String text, UriAction uriAction)
	{
		var radix = uriComponents.getQueryParams().getFirst(PARAMETER_RADIX);
		var name = uriComponents.getQueryParams().getFirst(PARAMETER_NAME);
		var location = uriComponents.getQueryParams().getFirst(PARAMETER_LOCATION);

		if (StringUtils.isBlank(radix))
		{
			return ContentText.EMPTY;
		}

		var certificateUri = new CertificateUri(radix, name, location);

		return new ContentUri(certificateUri.toString(), StringUtils.isNotBlank(text) ? text : generateName(name, location), uri -> uriAction.openUri(certificateUri));
	}

	private static String generateName(String name, String location)
	{
		var sb = new StringBuilder(AppName.NAME);
		sb.append(" Certificate (");

		if (StringUtils.isNotBlank(name))
		{
			sb.append(name);
		}
		else
		{
			sb.append("unknown");
		}
		if (StringUtils.isNotBlank(location))
		{
			sb.append(", @");
			sb.append(location);
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Generates the certificate in a friendly way, since otherwise this would generate big URLs.
	 *
	 * @param radix    the encoded certificate in base64
	 * @param name     the name
	 * @param location the location
	 * @return a link URL
	 */
	public static String generate(String radix, String name, String location)
	{
		var certificateUri = new CertificateUri(radix, name, location);
		return "<a href=\"" + certificateUri + "\">" + AppName.NAME + " Certificate (" + name + ", @" + location + ")</a>";
	}
}
