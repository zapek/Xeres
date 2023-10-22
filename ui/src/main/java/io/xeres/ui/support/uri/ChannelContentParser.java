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
import io.xeres.ui.support.util.UiUtils;
import javafx.scene.control.Alert;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;

import java.util.stream.Stream;

public class ChannelContentParser implements ContentParser
{
	@Override
	public String getProtocol()
	{
		return PROTOCOL_RETROSHARE;
	}

	@Override
	public String getAuthority()
	{
		return "channel";
	}

	@Override
	public Content parse(UriComponents uriComponents, String text)
	{
		var name = uriComponents.getQueryParams().getFirst("name");
		var channelId = uriComponents.getQueryParams().getFirst("id");
		var msgId = uriComponents.getQueryParams().getFirst("msgid");

		if (Stream.of(name, channelId).anyMatch(StringUtils::isBlank))
		{
			return ContentText.EMPTY;
		}

		return new ContentUri(msgId, name, s -> UiUtils.alert(Alert.AlertType.INFORMATION, "Channels are not supported yet."));
	}
}
