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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;

import java.util.stream.Stream;

public class CollectionUriFactory extends AbstractUriFactory
{
	private static final String AUTHORITY = "collection";

	private static final String PARAMETER_NAME = "name";
	private static final String PARAMETER_SIZE = "size";
	private static final String PARAMETER_RADIX = "radix";
	private static final String PARAMETER_FILES = "files";

	@Override
	public String getAuthority()
	{
		return AUTHORITY;
	}

	@Override
	public Content create(UriComponents uriComponents, String text, UriAction uriAction)
	{
		var name = uriComponents.getQueryParams().getFirst(PARAMETER_NAME);
		var size = uriComponents.getQueryParams().getFirst(PARAMETER_SIZE);
		var radix = uriComponents.getQueryParams().getFirst(PARAMETER_RADIX);
		var count = uriComponents.getQueryParams().getFirst(PARAMETER_FILES);

		if (Stream.of(name, size, radix, count).anyMatch(StringUtils::isBlank))
		{
			return ContentText.EMPTY;
		}

		var collectionUri = new CollectionUri(name, getLongArgument(size), radix, getIntArgument(count));

		//noinspection ConstantConditions
		return new ContentUri(radix, name + " (" + count + "files, " + FileUtils.byteCountToDisplaySize(Long.parseLong(size)) + ")", uri -> uriAction.openUri(collectionUri));
	}

	public static String generate(String name, int size, String radix, String files)
	{
		var uri = buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_NAME, name,
				PARAMETER_SIZE, String.valueOf(size),
				PARAMETER_RADIX, radix,
				PARAMETER_FILES, files);

		return "<a href=\"" + uri + "\">" + name + "</a>";
	}
}
