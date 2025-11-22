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

package io.xeres.ui.support.util;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

public final class ImageViewUtils
{
	private ImageViewUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Limits the size of an image by scaling it down. The aspect ratio is always preserved.
	 *
	 * @param imageView     the image to modify
	 * @param maximumWidth  the maximum width of the image
	 * @param maximumHeight the maximum height of the image
	 */
	public static void limitMaximumImageSize(ImageView imageView, int maximumWidth, int maximumHeight)
	{
		var width = imageView.getImage().getWidth();
		var height = imageView.getImage().getHeight();

		if (width > maximumWidth || height > maximumHeight)
		{
			var scaleImageView = new ImageView(imageView.getImage());
			scaleImageView.setPreserveRatio(true);
			scaleImageView.setSmooth(true);
			if (width > height)
			{
				scaleImageView.setFitWidth(maximumWidth);
			}
			else
			{
				scaleImageView.setFitHeight(maximumHeight);
			}
			var parameters = new SnapshotParameters();
			parameters.setFill(Color.TRANSPARENT); // Make sure we don't break PNGs
			imageView.setImage(scaleImageView.snapshot(parameters, null));
		}
	}

	/**
	 * Limits the size of an image by scaling it down. The aspect ratio is always preserved.
	 *
	 * @param imageView   the image to modify
	 * @param maximumSize the maximum size of the image in total number of pixels
	 */
	public static void limitMaximumImageSize(ImageView imageView, int maximumSize)
	{
		var width = imageView.getImage().getWidth();
		var height = imageView.getImage().getHeight();

		var actualSize = width * height;

		if (actualSize > maximumSize)
		{
			var ratio = Math.sqrt(maximumSize / actualSize);
			var scaleImageView = new ImageView(imageView.getImage());
			scaleImageView.setFitWidth(width * ratio);
			scaleImageView.setFitHeight(height * ratio);
			scaleImageView.setSmooth(true);

			var parameters = new SnapshotParameters();
			parameters.setFill(Color.TRANSPARENT); // Make sure we don't break PNGs
			imageView.setImage(scaleImageView.snapshot(parameters, null));
		}
	}

	/**
	 * Checks if an image has an exaggerated aspect ratio, that is, excessive horizontal
	 * or vertical length to try to mess up the UI.
	 *
	 * @param image the image to check
	 * @return true if the aspect ratio is excessive
	 */
	public static boolean isExaggeratedAspectRatio(Image image)
	{
		var width = image.getWidth();
		var height = image.getHeight();

		double aspectRatio;

		if (width > height)
		{
			aspectRatio = height / width;
		}
		else
		{
			aspectRatio = width / height;
		}
		return aspectRatio < 0.0014285714;
	}

	/**
	 * Determines the {@link Screen} on which a {@link Node} is displayed.
	 *
	 * @param node the node for which to determine the associated screen, can be null
	 * @return the screen where the node is located, or the primary screen if the node is null or not associated with a specific screen
	 */
	public static Screen getScreen(Node node)
	{
		if (node == null)
		{
			return Screen.getPrimary();
		}
		var bounds = node.localToScreen(node.getLayoutBounds());
		if (bounds == null)
		{
			return Screen.getPrimary();
		}
		var rect = new Rectangle2D(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
		return Screen.getScreens().stream()
				.filter(screen -> screen.getBounds().intersects(rect))
				.findFirst()
				.orElse(Screen.getPrimary());
	}
}
