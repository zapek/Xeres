/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.chat;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;

class ChatUserCell extends ListCell<ChatRoomUser>
{
	private static final int AVATAR_WIDTH = 32;
	private static final int AVATAR_HEIGHT = 32;

	private final GeneralClient generalClient;
	private final ImageCache imageCache;

	private static final ResourceBundle bundle = I18nUtils.getBundle();

	public ChatUserCell(GeneralClient generalClient, ImageCache imageCache)
	{
		super();
		this.generalClient = generalClient;
		this.imageCache = imageCache;
		TooltipUtils.install(this,
				() -> MessageFormat.format(bundle.getString("chat.room.user-info"), super.getItem().nickname(), super.getItem().gxsId()),
				() -> new ImageView(((ImageView) super.getGraphic()).getImage()));
	}

	@Override
	protected void updateItem(ChatRoomUser item, boolean empty)
	{
		super.updateItem(item, empty);
		setText(empty ? null : item.nickname());
		setGraphic(empty ? null : updateAvatar((AsyncImageView) getGraphic(), item));
	}

	private AsyncImageView updateAvatar(AsyncImageView asyncImageView, ChatRoomUser item)
	{
		if (asyncImageView == null)
		{
			asyncImageView = new AsyncImageView(
					url -> generalClient.getImage(url).block(),
					imageCache);
			asyncImageView.setFitWidth(AVATAR_WIDTH);
			asyncImageView.setFitHeight(AVATAR_HEIGHT);
		}

		asyncImageView.setUrl(getImageUrl(item));

		return asyncImageView;
	}

	private String getImageUrl(ChatRoomUser item)
	{
		if (item.identityId() != 0L)
		{
			return RemoteUtils.getControlUrl() + IDENTITIES_PATH + "/" + item.identityId() + "/image";
		}
		else
		{
			return RemoteUtils.getControlUrl() + IDENTITIES_PATH + "/image?gxsId=" + item.gxsId();
		}
	}
}
