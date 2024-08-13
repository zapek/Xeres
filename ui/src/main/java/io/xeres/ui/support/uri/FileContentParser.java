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
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.markdown.UriAction;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;

import java.util.stream.Stream;

public class FileContentParser implements ContentParser
{
	private static final String PARAMETER_NAME = "name";
	private static final String PARAMETER_SIZE = "size";
	private static final String PARAMETER_HASH = "hash";

	private String name;
	private long size;
	private Sha1Sum hash;

	private static final String AUTHORITY = "file";

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
		var nameParameter = uriComponents.getQueryParams().getFirst(PARAMETER_NAME);
		var sizeParameter = uriComponents.getQueryParams().getFirst(PARAMETER_SIZE);
		var hashParameter = uriComponents.getQueryParams().getFirst(PARAMETER_HASH);

		if (Stream.of(nameParameter, sizeParameter, hashParameter).anyMatch(StringUtils::isBlank))
		{
			return ContentText.EMPTY;
		}

		name = nameParameter;
		size = getLong(sizeParameter);
		hash = getHash(hashParameter);

		return new ContentUri(hash.toString(), name + " (" + FileUtils.byteCountToDisplaySize(size) + ")", uri -> uriAction.openUri(this));
	}

	public static String generate(String name, long size, Sha1Sum hash)
	{
		var uri = ContentParser.buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_NAME, name,
				PARAMETER_SIZE, String.valueOf(size),
				PARAMETER_HASH, hash.toString());

		return "<a href=\"" + uri + "\">" + name + "</a>";
	}

	public String getName()
	{
		return name;
	}

	public long getSize()
	{
		return size;
	}

	public Sha1Sum getHash()
	{
		return hash;
	}
}
