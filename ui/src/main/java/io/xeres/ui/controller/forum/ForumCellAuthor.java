/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.forum;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.message.forum.ForumMessage;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.ImageView;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;

class ForumCellAuthor extends TreeTableCell<ForumMessage, ForumMessage>
{
	private static final int AUTHOR_WIDTH = 24;
	private static final int AUTHOR_HEIGHT = 24;

	private final GeneralClient generalClient;
	private final ImageCache imageCache;
	private final ResourceBundle bundle = I18nUtils.getBundle();

	public ForumCellAuthor(GeneralClient generalClient, ImageCache imageCache)
	{
		super();
		this.generalClient = generalClient;
		this.imageCache = imageCache;
		TooltipUtils.install(this,
				() -> MessageFormat.format(bundle.getString("chat.room.user-info"), super.getItem().getAuthorName(), super.getItem().getAuthorId()),
				() -> new ImageView(((ImageView) super.getGraphic()).getImage()));
	}

	@Override
	protected void updateItem(ForumMessage item, boolean empty)
	{
		super.updateItem(item, empty);
		setText(empty ? null : getAuthorName(item));
		setGraphic(empty ? null : updateAuthor((AsyncImageView) getGraphic(), item));
	}

	private static String getAuthorName(ForumMessage item)
	{
		return item.getAuthorName() != null ? item.getAuthorName() : item.getGxsId().toString();
	}

	private AsyncImageView updateAuthor(AsyncImageView asyncImageView, ForumMessage message)
	{
		if (asyncImageView == null)
		{
			asyncImageView = new AsyncImageView(
					url -> generalClient.getImage(url).block(),
					null,
					imageCache);
			asyncImageView.setFitWidth(AUTHOR_WIDTH);
			asyncImageView.setFitHeight(AUTHOR_HEIGHT);
		}

		asyncImageView.setUrl(getIdentityImageUrl(message));

		return asyncImageView;
	}

	public static String getIdentityImageUrl(ForumMessage message)
	{
		if (message.getAuthorId() != null)
		{
			return RemoteUtils.getControlUrl() + IDENTITIES_PATH + "/image?gxsId=" + message.getAuthorId() + "&find=true";
		}
		return null;
	}
}
