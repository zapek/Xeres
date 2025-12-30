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
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.custom.asyncimage.PlaceholderImageView;
import io.xeres.ui.model.board.BoardGroup;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.ImageView;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import static io.xeres.common.rest.PathConfig.BOARDS_PATH;

public class BoardGroupCell extends TreeTableCell<BoardGroup, BoardGroup>
{
	private static final int IMAGE_WIDTH = 32;
	private static final int IMAGE_HEIGHT = 32;

	private final GeneralClient generalClient;
	private final ImageCache imageCache;

	private static final ResourceBundle bundle = I18nUtils.getBundle();

	public BoardGroupCell(GeneralClient generalClient, ImageCache imageCache)
	{
		super();
		this.generalClient = generalClient;
		this.imageCache = imageCache;
		TooltipUtils.install(this,
				() -> MessageFormat.format(bundle.getString("gxs-group.tree.info"),
						getItem().getName(),
						getItem().getGxsId()),
				() -> new ImageView(((PlaceholderImageView) getGraphic()).getImage()));
	}

	@Override
	protected void updateItem(BoardGroup item, boolean empty)
	{
		super.updateItem(item, empty);
		setText(empty ? null : item.getName());
		setGraphic(empty ? null : updateImage((PlaceholderImageView) getGraphic(), item));
	}

	private PlaceholderImageView updateImage(PlaceholderImageView placeholderImageView, BoardGroup item)
	{
		if (placeholderImageView == null)
		{
			placeholderImageView = new PlaceholderImageView(
					url -> generalClient.getImage(url).block(),
					"mdi2v-view-dashboard-outline",
					imageCache);
		}
		if (item.isReal())
		{
			placeholderImageView.setFitWidth(IMAGE_WIDTH);
			placeholderImageView.setFitHeight(IMAGE_HEIGHT);
			placeholderImageView.setUrl(getImageUrl(item));
		}
		else
		{
			placeholderImageView.setFitWidth(0);
			placeholderImageView.setFitHeight(0);
			placeholderImageView.setUrl(null);
			placeholderImageView.hideDefault(); // SetUrl(null) shows a default, but we don't want one as we're tree group nodes
		}
		return placeholderImageView;
	}

	private String getImageUrl(BoardGroup item)
	{
		if (item.isReal() && item.hasImage())
		{
			return RemoteUtils.getControlUrl() + BOARDS_PATH + "/groups/" + item.getId() + "/image";
		}
		return null;
	}
}
