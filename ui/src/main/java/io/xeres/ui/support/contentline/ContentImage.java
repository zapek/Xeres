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

package io.xeres.ui.support.contentline;

import io.xeres.ui.support.util.ImageViewUtils;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

public class ContentImage implements Content
{
	private final ImageView node;

	public ContentImage(Image image)
	{
		this(image, null);
	}

	public ContentImage(Image image, Region parent)
	{
		node = new ImageView();
		var screen = ImageViewUtils.getScreen(parent);

		// Remove ImageView's output scaling so that it's not zoomed in on 4K monitors.
		node.setFitWidth(image.getWidth() / screen.getOutputScaleX());
		node.setFitHeight(image.getHeight() / screen.getOutputScaleY());

		node.setImage(image);
		ImageViewUtils.addImageContextMenuActions(node);

		if (parent != null)
		{
			syncImageWidth(node, parent.getWidth());
			parent.widthProperty().addListener((_, _, newValue) -> syncImageWidth(node, newValue.doubleValue()));

			node.setPreserveRatio(true);
		}
	}

	private static void syncImageWidth(ImageView imageView, double width)
	{
		imageView.setFitWidth(width - 24); // margins of 12 on each side
	}

	@Override
	public Node getNode()
	{
		return node;
	}
}
