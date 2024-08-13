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

import java.util.stream.Stream;

public class IdentityContentParser implements ContentParser
{
	private static final String PARAMETER_GXSID = "gxsid";
	private static final String PARAMETER_NAME = "name";
	private static final String PARAMETER_GROUPDATA = "groupdata";

	private static final String AUTHORITY = "identity";

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
		var gxsId = uriComponents.getQueryParams().getFirst(PARAMETER_GXSID);
		var name = uriComponents.getQueryParams().getFirst(PARAMETER_NAME);
		var groupData = uriComponents.getQueryParams().getFirst(PARAMETER_GROUPDATA);

		if (Stream.of(gxsId, name, groupData).anyMatch(StringUtils::isBlank))
		{
			return ContentText.EMPTY;
		}
		return new ContentUri(groupData, "Identity (name=" + name + ", ID=" + gxsId + ")", uri -> uriAction.openUri(this));
	}

	public static String generate(String gxsId, String name, String groupData)
	{
		var uri = ContentParser.buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_GXSID, gxsId,
				PARAMETER_NAME, name,
				PARAMETER_GROUPDATA, groupData);

		return "<a href=\"" + uri + "\">" + name + "</a>";
	}
}
