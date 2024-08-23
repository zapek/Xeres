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

package io.xeres.ui.support.contentline;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Screen;

public class ContentImage implements Content
{
	private final ImageView node;

	public ContentImage(Image image)
	{
		node = new ImageView();

		// Remove ImageView's output scaling so that it's not zoomed in on 4K monitors.
		node.setFitWidth(image.getWidth() / Screen.getPrimary().getOutputScaleX());
		node.setFitHeight(image.getHeight() / Screen.getPrimary().getOutputScaleY());

		node.setImage(image);
	}

	@Override
	public Node getNode()
	{
		return node;
	}
}
