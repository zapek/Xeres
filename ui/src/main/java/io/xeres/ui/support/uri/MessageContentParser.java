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
import org.springframework.web.util.UriComponents;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class MessageContentParser implements ContentParser
{
	public static final String PARAMETER_ID = "id";
	public static final String PARAMETER_SUBJECT = "subject";

	private static final String AUTHORITY = "message";

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
		var id = uriComponents.getQueryParams().getFirst(PARAMETER_ID); // warning: it can be of different type (gxsId, sslId, etc...)
		var subject = uriComponents.getQueryParams().getFirst(PARAMETER_SUBJECT);

		if (isBlank(id))
		{
			return ContentText.EMPTY;
		}

		return new ContentUri(id, id, s -> UiUtils.alert(Alert.AlertType.INFORMATION, "Messages are not supported yet."));
	}

	public static String generate(String id, String subject)
	{
		var uri = ContentParser.buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_ID, id,
				PARAMETER_SUBJECT, subject);

		return "<a href=\"" + uri + "\">" + subject + "</a>";
	}
}
