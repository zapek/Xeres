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

package io.xeres.ui.controller.chess;

import io.xeres.common.dto.chess.ChessHistorySummaryDTO;
import io.xeres.common.rest.PathConfig;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

final class ChessHistoryIdentityCell extends TableCell<ChessHistorySummaryDTO, ChessHistorySummaryDTO>
{
	private final boolean white;
	private final AsyncImageView avatar;

	ChessHistoryIdentityCell(boolean white, GeneralClient client, ImageCache cache)
	{
		this.white = white;
		avatar = new AsyncImageView(url -> client.getImage(url).block(), cache);
		avatar.setFitWidth(30);
		avatar.setFitHeight(30);
		avatar.setPreserveRatio(true);
		setGraphicTextGap(8);
	}

	@Override
	protected void updateItem(ChessHistorySummaryDTO item, boolean empty)
	{
		super.updateItem(item, empty);
		if (empty || item == null)
		{
			avatar.setUrl(null);
			setText(null);
			setGraphic(null);
			setTooltip(null);
			return;
		}
		var identity = white ? item.whiteIdentity() : item.blackIdentity();
		setText(white ? item.whiteName() : item.blackName());
		if (identity == null || !identity.matches("[0-9a-fA-F]{32}"))
		{
			avatar.setUrl(null);
			setGraphic(null);
			setTooltip(null);
			return;
		}
		setTooltip(new Tooltip(identity));
		setGraphic(avatar);
		avatar.setUrl(RemoteUtils.getControlUrl() + PathConfig.IDENTITIES_PATH + "/image?find=true&gxsId=" + identity);
	}
}
