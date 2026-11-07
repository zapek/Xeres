/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.mail;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.model.mail.MailMessage;
import javafx.scene.control.TreeTableCell;

import java.util.ResourceBundle;

import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;

class MailCellAuthor extends TreeTableCell<MailMessage, MailMessage>
{
	private static final int AUTHOR_WIDTH = 24;
	private static final int AUTHOR_HEIGHT = 24;

	private final GeneralClient generalClient;
	private final ImageCache imageCache;
	private static final ResourceBundle bundle = I18nUtils.getBundle();

	public MailCellAuthor(GeneralClient generalClient, ImageCache imageCache)
	{
		this.generalClient = generalClient;
		this.imageCache = imageCache;
		// XXX: add Tooltip
	}

	@Override
	protected void updateItem(MailMessage item, boolean empty)
	{
		super.updateItem(item, empty);
		setText(empty ? null : getAuthorName(item));
		setGraphic(empty ? null : updateAuthor((AsyncImageView) getGraphic(), item));
	}

	private static String getAuthorName(MailMessage item)
	{
		if (item.getAuthorName() != null)
		{
			return item.getAuthorName();
		}
		else
		{
			if (item.getIdentityFrom() != null)
			{
				return item.getIdentityFrom().asString();
			}
		}
		return null;
	}

	private AsyncImageView updateAuthor(AsyncImageView asyncImageView, MailMessage message)
	{
		if (asyncImageView == null)
		{
			asyncImageView = new AsyncImageView(
					url -> generalClient.getImage(url).block(),
					imageCache);
			asyncImageView.setFitWidth(AUTHOR_WIDTH);
			asyncImageView.setFitHeight(AUTHOR_HEIGHT);
		}

		asyncImageView.setUrl(getIdentityImageUrl(message));

		return asyncImageView;
	}

	private static String getIdentityImageUrl(MailMessage message)
	{
		if (message.getIdentityFrom() != null)
		{
			return RemoteUtils.getControlUrl() + IDENTITIES_PATH + "/image?gxsId=" + message.getIdentityFrom().asString() + "&find=true";
		}
		return null;
	}
}
