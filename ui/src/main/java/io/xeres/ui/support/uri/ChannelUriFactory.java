/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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
import io.xeres.common.id.MsgId;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.markdown.UriAction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;

import java.util.stream.Stream;

import static io.xeres.ui.support.uri.ChannelUri.*;

public class ChannelUriFactory extends AbstractUriFactory
{
	@Override
	public String getAuthority()
	{
		return AUTHORITY;
	}

	@Override
	public Content createContent(UriComponents uriComponents, String text, UriAction uriAction)
	{
		var channelUri = createUri(uriComponents);
		if (channelUri == null)
		{
			return new ContentText("");
		}

		return new ContentUri(channelUri, StringUtils.isNotBlank(text) ? text : channelUri.name(), uriAction::openUri);
	}

	@Override
	ChannelUri createUri(UriComponents uriComponents)
	{
		var name = uriComponents.getQueryParams().getFirst(PARAMETER_NAME);
		var id = uriComponents.getQueryParams().getFirst(PARAMETER_GXS_ID);
		var msgId = uriComponents.getQueryParams().getFirst(PARAMETER_MSG_ID);

		if (Stream.of(name, id).anyMatch(StringUtils::isBlank))
		{
			return null;
		}

		return new ChannelUri(name, GxsId.fromString(id), StringUtils.isNotBlank(msgId) ? MsgId.fromString(msgId) : null);
	}
}
