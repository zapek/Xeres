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

public class ForumContentParser implements ContentParser
{
	private static final String AUTHORITY = "forum";

	private static final String PARAMETER_NAME = "name";
	private static final String PARAMETER_ID = "id";
	private static final String PARAMETER_MSGID = "msgid";

	private String name;
	private GxsId id;
	private MessageId msgId;

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
		var idParameter = uriComponents.getQueryParams().getFirst(PARAMETER_ID);
		var msgIdParameter = uriComponents.getQueryParams().getFirst(PARAMETER_MSGID);

		if (Stream.of(nameParameter, idParameter).anyMatch(StringUtils::isBlank))
		{
			return ContentText.EMPTY;
		}

		name = nameParameter;
		id = GxsId.fromString(idParameter);
		if (StringUtils.isNotBlank(msgIdParameter))
		{
			msgId = MessageId.fromString(msgIdParameter);
		}

		return new ContentUri(msgIdParameter, nameParameter, uri -> uriAction.openUri(this));
	}

	public static String generate(String name, GxsId id, MessageId msgid)
	{
		var uri = ContentParser.buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_NAME, name,
				PARAMETER_ID, id.toString(),
				PARAMETER_MSGID, msgid.toString());

		return "<a href=\"" + uri + "\">" + name + "</a>";
	}

	public static String generate(String name, GxsId id)
	{
		var uri = ContentParser.buildUri(PROTOCOL_RETROSHARE, AUTHORITY,
				PARAMETER_NAME, name,
				PARAMETER_ID, id.toString());

		return "<a href=\"" + uri + "\">" + name + "</a>";
	}

	public String getName()
	{
		return name;
	}

	public GxsId getId()
	{
		return id;
	}

	public MessageId getMsgId()
	{
		return msgId;
	}
}
