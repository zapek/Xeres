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

import io.xeres.common.id.GxsId;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.markdown.UriAction;
import org.springframework.web.util.UriComponents;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class MessageUriFactory extends AbstractUriFactory
{
	private static final String AUTHORITY = "message";

	private static final String PARAMETER_ID = "id";
	private static final String PARAMETER_SUBJECT = "subject";

	@Override
	public String getAuthority()
	{
		return AUTHORITY;
	}

	@Override
	public Content create(UriComponents uriComponents, String text, UriAction uriAction)
	{
		var id = uriComponents.getQueryParams().getFirst(PARAMETER_ID); // XXX: warning: it can be of different type (gxsId, locationId, etc...). We need to detect it first
		var subject = uriComponents.getQueryParams().getFirst(PARAMETER_SUBJECT);

		if (isBlank(id))
		{
			return ContentText.EMPTY;
		}

		var messageUri = new MessageUri(GxsId.fromString(id), subject);

		return new ContentUri(id, id, uri -> uriAction.openUri(messageUri));
	}

	public static String generate(String id, String subject)
	{
		var uri = buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_ID, id,
				PARAMETER_SUBJECT, subject);

		return "<a href=\"" + uri + "\">" + subject + "</a>";
	}
}
