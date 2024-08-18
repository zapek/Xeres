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
import io.xeres.common.id.MessageId;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.markdown.UriAction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;

import java.util.stream.Stream;

public class ChannelUriFactory extends AbstractUriFactory
{
	private static final String AUTHORITY = "channel";

	private static final String PARAMETER_NAME = "name";
	private static final String PARAMETER_ID = "id";
	private static final String PARAMETER_MSGID = "msgid";

	@Override
	public String getAuthority()
	{
		return AUTHORITY;
	}

	@Override
	public Content create(UriComponents uriComponents, String text, UriAction uriAction)
	{
		var name = uriComponents.getQueryParams().getFirst(PARAMETER_NAME);
		var id = uriComponents.getQueryParams().getFirst(PARAMETER_ID);
		var msgId = uriComponents.getQueryParams().getFirst(PARAMETER_MSGID);

		if (Stream.of(name, id).anyMatch(StringUtils::isBlank))
		{
			return ContentText.EMPTY;
		}

		var channelUri = new ChannelUri(name, GxsId.fromString(id), StringUtils.isNotBlank(msgId) ? MessageId.fromString(msgId) : null);

		return new ContentUri(msgId != null ? msgId : id, name, uri -> uriAction.openUri(channelUri));
	}

	public static String generate(String name, String id, String msgId)
	{
		var uri = buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_NAME, name,
				PARAMETER_ID, id,
				PARAMETER_MSGID, msgId);

		return "<a href=\"" + uri + "\">" + name + "</a>";
	}
}
