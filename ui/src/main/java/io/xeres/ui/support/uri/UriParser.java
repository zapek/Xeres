/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

public final class UriParser
{
	private static final Map<String, Map<String, ContentParser>> contentParsers = new HashMap<>();

	static
	{
		addContentParser(new BoardsContentParser());
		addContentParser(new CertificateContentParser());
		addContentParser(new ChannelContentParser());
		addContentParser(new ChatRoomContentParser());
		addContentParser(new FileContentParser());
		addContentParser(new ForumContentParser());
		addContentParser(new IdentityContentParser());
		addContentParser(new MessageContentParser());
		addContentParser(new ProfileContentParser());
		addContentParser(new SearchContentParser());
		addContentParser(new CollectionContentParser());
	}

	private UriParser()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static void addContentParser(ContentParser contentParser)
	{
		var map = contentParsers.getOrDefault(contentParser.getProtocol(), new HashMap<>());
		map.put(contentParser.getAuthority(), contentParser);
		contentParsers.put(contentParser.getProtocol(), map);
	}

	public static Content parse(String href, String text)
	{
		if (isBlank(href))
		{
			return new ContentText(text);
		}

		try
		{
			var uri = new URI(href);
			var contentParserMap = contentParsers.get(uri.getScheme());
			var contentParser = contentParserMap.get(uri.getAuthority());

			if (contentParser != null)
			{
				return contentParser.parse(uri, text);
			}
			return new ContentUri(uri.toString());
		}
		catch (URISyntaxException e)
		{
			return ContentText.EMPTY;
		}
	}
}
