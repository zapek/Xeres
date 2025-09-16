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

import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.markdown.UriAction;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isBlank;

public final class UriFactory
{
	private static final Map<String, Map<String, AbstractUriFactory>> contentParsers = new HashMap<>();

	static
	{
		addContentParser(new BoardsUriFactory());
		addContentParser(new CertificateUriFactory());
		addContentParser(new ChannelUriFactory());
		addContentParser(new ChatRoomUriFactory());
		addContentParser(new FileUriFactory());
		addContentParser(new ForumUriFactory());
		addContentParser(new IdentityUriFactory());
		addContentParser(new MessageUriFactory());
		addContentParser(new ProfileUriFactory());
		addContentParser(new SearchUriFactory());
		addContentParser(new CollectionUriFactory());
	}

	private static final ExternalUriFactory externalUriFactory = new ExternalUriFactory();

	private UriFactory()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static void addContentParser(AbstractUriFactory contentParser)
	{
		var map = contentParsers.getOrDefault(contentParser.getProtocol(), new HashMap<>());
		var authority = contentParser.getAuthority();
		Objects.requireNonNull(authority, "Authority cannot be null for " + contentParser.getClass().getSimpleName() + ", or it's not supposed to be used as a content parser");
		map.put(authority, contentParser);
		contentParsers.put(contentParser.getProtocol(), map);
	}

	public static Content createContent(String href, String text, UriAction uriAction)
	{
		if (isBlank(href))
		{
			return new ContentText(text);
		}

		try
		{
			var uri = new URI(href);
			var contentParserMap = contentParsers.get(uri.getScheme());
			if (contentParserMap != null)
			{
				var contentParser = contentParserMap.get(uri.getAuthority());

				if (contentParser != null)
				{
					var uriComponents = UriComponentsBuilder.fromPath(uri.getPath())
							.query(uri.getQuery())
							.build();

					return contentParser.create(uriComponents, text, uriAction);
				}
			}
			return externalUriFactory.create(UriComponentsBuilder.fromUri(uri).build(), text, uriAction);
		}
		catch (URISyntaxException _)
		{
			return new ContentText("");
		}
	}
}
