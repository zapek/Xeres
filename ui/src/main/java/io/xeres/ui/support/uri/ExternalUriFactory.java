/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.markdown.UriAction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class ExternalUriFactory extends AbstractUriFactory
{
	@Override
	public String getAuthority()
	{
		return null;
	}

	@Override
	Content create(UriComponents uriComponents, String text, UriAction uriAction)
	{
		var externalUri = new ExternalUri(URLDecoder.decode(uriComponents.toUriString(), StandardCharsets.UTF_8));

		return new ContentUri(externalUri, StringUtils.isNotBlank(text) ? text : externalUri.toUriString(), uriAction::openUri);
	}
}
