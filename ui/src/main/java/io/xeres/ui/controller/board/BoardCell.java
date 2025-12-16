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

package io.xeres.ui.controller.board;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.model.board.BoardGroup;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.ImageView;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import static io.xeres.common.rest.PathConfig.BOARDS_PATH;

public class BoardCell extends TreeTableCell<BoardGroup, BoardGroup>
{
	private static final int IMAGE_WIDTH = 32;
	private static final int IMAGE_HEIGHT = 32;

	private final GeneralClient generalClient;
	private final ImageCache imageCache;

	private static final ResourceBundle bundle = I18nUtils.getBundle();

	public BoardCell(GeneralClient generalClient, ImageCache imageCache)
	{
		super();
		this.generalClient = generalClient;
		this.imageCache = imageCache;
		TooltipUtils.install(this,
				() -> MessageFormat.format(bundle.getString("board.tree.info"), super.getItem().getName(), super.getItem().getDescription(), super.getItem().getGxsId()),
				() -> new ImageView(((ImageView) super.getGraphic()).getImage()));
	}

	@Override
	protected void updateItem(BoardGroup item, boolean empty)
	{
		super.updateItem(item, empty);
		setText(empty ? null : item.getName());
		setGraphic(empty ? null : updateImage((AsyncImageView) getGraphic(), item));
	}

	private AsyncImageView updateImage(AsyncImageView asyncImageView, BoardGroup item)
	{
		if (asyncImageView == null)
		{
			asyncImageView = new AsyncImageView(
					url -> generalClient.getImage(url).block(),
					null,
					imageCache);
		}
		asyncImageView.setFitWidth(item.isReal() ? IMAGE_WIDTH : 0);
		asyncImageView.setFitHeight(item.isReal() ? IMAGE_HEIGHT : 0);

		asyncImageView.setUrl(getImageUrl(item));

		return asyncImageView;
	}

	private String getImageUrl(BoardGroup item)
	{
		if (item.isReal())
		{
			return RemoteUtils.getControlUrl() + BOARDS_PATH + "/groups/" + item.getId() + "/image";
		}
		return null;
	}
}
