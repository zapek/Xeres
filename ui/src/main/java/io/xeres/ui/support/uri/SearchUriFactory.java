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

import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.markdown.UriAction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;

public class SearchUriFactory extends AbstractUriFactory
{
	private static final String AUTHORITY = "search";

	private static final String PARAMETER_KEYWORDS = "keywords";

	@Override
	public String getAuthority()
	{
		return AUTHORITY;
	}

	@Override
	public Content create(UriComponents uriComponents, String text, UriAction uriAction)
	{
		var keywords = uriComponents.getQueryParams().getFirst(PARAMETER_KEYWORDS);

		if (StringUtils.isBlank(keywords))
		{
			return ContentText.EMPTY;
		}

		var searchUri = new SearchUri(keywords.trim());

		return new ContentUri(keywords, keywords, uri -> uriAction.openUri(searchUri));
	}

	public static String generate(String name, String keywords)
	{
		var uri = buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_KEYWORDS, keywords);

		return "<a href=\"" + uri + "\">" + name + "</a>";
	}
}
