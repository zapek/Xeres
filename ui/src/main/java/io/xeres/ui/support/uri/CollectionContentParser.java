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
import io.xeres.ui.support.markdown.LinkAction;
import io.xeres.ui.support.util.UiUtils;
import javafx.scene.control.Alert;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;

import java.util.stream.Stream;

public class CollectionContentParser implements ContentParser
{
	public static final String PARAMETER_NAME = "name";
	public static final String PARAMETER_SIZE = "size";
	public static final String PARAMETER_RADIX = "radix";
	public static final String PARAMETER_FILES = "files";

	private static final String AUTHORITY = "collection";

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
		var radix = uriComponents.getQueryParams().getFirst(PARAMETER_RADIX);
		var count = uriComponents.getQueryParams().getFirst(PARAMETER_FILES);

		if (Stream.of(name, size, radix, count).anyMatch(StringUtils::isBlank))
		{
			return ContentText.EMPTY;
		}

		//noinspection ConstantConditions
		return new ContentUri(radix, name + " (" + count + "files, " + FileUtils.byteCountToDisplaySize(Long.parseLong(size)) + ")", s -> UiUtils.alert(Alert.AlertType.INFORMATION, "Browsing collections is not supported yet."));
	}

	public static String generate(String name, int size, String radix, String files)
	{
		var uri = ContentParser.buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_NAME, name,
				PARAMETER_SIZE, String.valueOf(size),
				PARAMETER_RADIX, radix,
				PARAMETER_FILES, files);

		return "<a href=\"" + uri + "\">" + name + "</a>";
	}
}
