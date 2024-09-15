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

package io.xeres.ui.controller.identity;

import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.AsyncImageView;
import io.xeres.ui.model.identity.Identity;
import javafx.scene.control.TableCell;
import javafx.scene.image.ImageView;

import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;

public class ImageCell extends TableCell<Identity, Long>
{
	private final GeneralClient generalClient;

	public ImageCell(GeneralClient generalClient)
	{
		super();
		this.generalClient = generalClient;
	}

	@Override
	protected void updateItem(Long item, boolean empty)
	{
		super.updateItem(item, empty);
		setGraphic(empty ? null : updateAvatarImage((AsyncImageView) getGraphic(), item));
	}

	private ImageView updateAvatarImage(AsyncImageView imageView, Long id)
	{
		if (imageView == null)
		{
			imageView = new AsyncImageView();
			imageView.setFitWidth(128);
			imageView.setFitHeight(128);
		}
		imageView.setUrl(IDENTITIES_PATH + "/" + id + "/image", url -> generalClient.getImage(url).block());
		return imageView;
	}
}
