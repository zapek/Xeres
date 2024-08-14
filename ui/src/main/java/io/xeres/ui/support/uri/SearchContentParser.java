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

public class SearchContentParser implements ContentParser
{
	private static final String PARAMETER_KEYWORDS = "keywords";

	private static final String AUTHORITY = "search";

	private String keywords;

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
	public Content parse(UriComponents uriComponents, String text, UriAction uriAction)
	{
		var keywordsParameters = uriComponents.getQueryParams().getFirst(PARAMETER_KEYWORDS);

		if (StringUtils.isBlank(keywordsParameters))
		{
			return ContentText.EMPTY;
		}

		keywords = keywordsParameters.trim();

		return new ContentUri(keywords, keywords, uri -> uriAction.openUri(this));
	}

	public static String generate(String name, String keywords)
	{
		var uri = ContentParser.buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_KEYWORDS, name);

		return "<a href=\"" + uri + "\">" + name + "</a>";
	}

	public String getKeywords()
	{
		return keywords;
	}
}
