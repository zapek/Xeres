/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.channel;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.custom.asyncimage.PlaceholderImageView;
import io.xeres.ui.model.channel.ChannelGroup;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.ImageView;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import static io.xeres.common.rest.PathConfig.CHANNELS_PATH;

public class ChannelGroupCell extends TreeTableCell<ChannelGroup, ChannelGroup>
{
	private static final int IMAGE_WIDTH = 32;
	private static final int IMAGE_HEIGHT = 32;

	private final GeneralClient generalClient;
	private final ImageCache imageCache;

	private static final ResourceBundle bundle = I18nUtils.getBundle();

	public ChannelGroupCell(GeneralClient generalClient, ImageCache imageCache)
	{
		super();
		this.generalClient = generalClient;
		this.imageCache = imageCache;
		TooltipUtils.install(this,
				() -> MessageFormat.format(bundle.getString("gxs-group.tree.info"),
						getItem().getName(),
						getItem().getGxsId()),
				() -> new ImageView(((PlaceholderImageView) super.getGraphic()).getImage()));
	}

	@Override
	protected void updateItem(ChannelGroup item, boolean empty)
	{
		super.updateItem(item, empty);
		setText(empty ? null : item.getName());
		setGraphic(empty ? null : updateImage((PlaceholderImageView) getGraphic(), item));
	}

	private PlaceholderImageView updateImage(PlaceholderImageView placeholderImageView, ChannelGroup item)
	{
		if (placeholderImageView == null)
		{
			placeholderImageView = new PlaceholderImageView(
					url -> generalClient.getImage(url).block(),
					"mdi2p-play-box",
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

	private String getImageUrl(ChannelGroup item)
	{
		if (item.isReal() && item.hasImage())
		{
			return RemoteUtils.getControlUrl() + CHANNELS_PATH + "/groups/" + item.getId() + "/image";
		}
		return null;
	}
}
