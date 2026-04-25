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

package io.xeres.ui.controller.settings;

import io.xeres.ui.support.theme.AppTheme;
import io.xeres.ui.support.util.ImageViewUtils;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;

class ThemeCell extends ListCell<AppTheme>
{
	private final Node parent;

	public ThemeCell(Node parent)
	{
		this.parent = parent;
	}

	@Override
	protected void updateItem(AppTheme appTheme, boolean empty)
	{
		super.updateItem(appTheme, empty);

		if (!empty)
		{
			var imageView = new ImageView("/image/themes/" + appTheme.getName() + ".png");
			ImageViewUtils.disableOutputScaling(imageView, parent);
			setText(appTheme.getName());
			setGraphic(imageView);
		}
		else
		{
			setText(null);
			setGraphic(null);
		}
	}
}
