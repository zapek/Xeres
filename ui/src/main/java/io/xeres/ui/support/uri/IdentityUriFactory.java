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
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.markdown.UriAction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;

import java.util.stream.Stream;

import static io.xeres.ui.support.uri.IdentityUri.*;

public class IdentityUriFactory extends AbstractUriFactory
{
	@Override
	public String getAuthority()
	{
		return AUTHORITY;
	}

	@Override
	public Content createContent(UriComponents uriComponents, String text, UriAction uriAction)
	{
		var identityUri = createUri(uriComponents);
		if (identityUri == null)
		{
			return new ContentText("");
		}

		return new ContentUri(identityUri, StringUtils.isNotBlank(text) ? text : ("Identity (name=" + identityUri.name() + ", ID=" + identityUri.gxsId() + ")"), uriAction::openUri);
	}

	@Override
	IdentityUri createUri(UriComponents uriComponents)
	{
		var gxsId = uriComponents.getQueryParams().getFirst(PARAMETER_GXS_ID);
		var name = uriComponents.getQueryParams().getFirst(PARAMETER_NAME);
		var groupData = uriComponents.getQueryParams().getFirst(PARAMETER_GROUPDATA);

		if (Stream.of(gxsId, name).anyMatch(StringUtils::isBlank))
		{
			return null;
		}

		return new IdentityUri(name, GxsId.fromString(gxsId), groupData); // groupData contains the gxs group's data so that the peer can do something with it even if it doesn't have the group yet
	}
}
