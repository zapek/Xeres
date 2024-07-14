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
import io.xeres.ui.support.markdown.LinkAction;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class FileContentParser implements ContentParser
{
	public static final String PARAMETER_NAME = "name";
	public static final String PARAMETER_SIZE = "size";
	public static final String PARAMETER_HASH = "hash";

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
	public Content parse(UriComponents uriComponents, String text, LinkAction linkAction)
	{
		var name = uriComponents.getQueryParams().getFirst(PARAMETER_NAME);
		var size = uriComponents.getQueryParams().getFirst(PARAMETER_SIZE);
		var hash = uriComponents.getQueryParams().getFirst(PARAMETER_HASH);

		if (Stream.of(name, size, hash).anyMatch(StringUtils::isBlank))
		{
			return ContentText.EMPTY;
		}

		return new ContentUri(hash, name + " (" + FileUtils.byteCountToDisplaySize(Long.parseLong(size)) + ")", uri -> {
			Map<String, String> parameters = new HashMap<>();
			parameters.put(PARAMETER_NAME, name);
			parameters.put(PARAMETER_SIZE, size);
			parameters.put(PARAMETER_HASH, hash);
			linkAction.openLink(this, parameters);
		});
	}

	public static String generate(String name, long size, Sha1Sum hash)
	{
		var uri = ContentParser.buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_NAME, name,
				PARAMETER_SIZE, String.valueOf(size),
				PARAMETER_HASH, hash.toString());

		return "<a href=\"" + uri + "\">" + name + "</a>";
	}
}
