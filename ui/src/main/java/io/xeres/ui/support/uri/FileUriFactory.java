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

import io.xeres.common.id.Sha1Sum;
import io.xeres.common.util.ByteUnitUtils;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.markdown.UriAction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;

import java.util.stream.Stream;

import static io.xeres.ui.support.uri.FileUri.*;

public class FileUriFactory extends AbstractUriFactory
{
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
		var hash = uriComponents.getQueryParams().getFirst(PARAMETER_HASH);

		if (Stream.of(name, size, hash).anyMatch(StringUtils::isBlank))
		{
			return ContentText.EMPTY;
		}

		var fileUri = new FileUri(name, getLongArgument(size), getHashArgument(hash));

		return new ContentUri(fileUri.toString(), StringUtils.isNotBlank(text) ? text : (fileUri.name() + " (" + ByteUnitUtils.fromBytes(fileUri.size()) + ")"), uri -> uriAction.openUri(fileUri));
	}

	public static String generate(String name, long size, Sha1Sum hash)
	{
		var fileUri = new FileUri(name, size, hash);

		return "<a href=\"" + fileUri + "\">" + name + " (" + ByteUnitUtils.fromBytes(size) + ")" + "</a>";
	}
}
