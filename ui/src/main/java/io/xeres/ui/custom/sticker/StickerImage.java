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

package io.xeres.ui.custom.sticker;

import io.xeres.ui.support.util.ImageViewUtils;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

class StickerImage implements Sticker
{
	private static final Logger log = LoggerFactory.getLogger(StickerImage.class);

	private Image image;
	private Path filePath;

	public StickerImage(Path filePath)
	{
		try (var inputStream = new FileInputStream(filePath.toFile()))
		{
			this.filePath = filePath;
			var sticker = new Image(inputStream);
			image = sticker.isError() ? null : sticker;
		}
		catch (IOException e)
		{
			log.debug("Couldn't open image {}: {}", filePath, e.getMessage());
		}
	}

	@Override
	public boolean hasNode()
	{
		return image != null;
	}

	@Override
	public Node createMainNode(Node parent)
	{
		Objects.requireNonNull(image);

		var imageView = new ImageView(image);
		imageView.setPickOnBounds(true); // make transparent areas clickable
		ImageViewUtils.limitMaximumImageSize(imageView, 48, 48);
		ImageViewUtils.disableOutputScaling(imageView, parent);
		return imageView;
	}

	@Override
	public Node createNode(Node parent)
	{
		Objects.requireNonNull(image);

		var imageView = new ImageView(image);
		imageView.setPickOnBounds(true); // make transparent areas clickable
		ImageViewUtils.limitMaximumImageSize(imageView, 192, 192);
		ImageViewUtils.disableOutputScaling(imageView, parent);
		imageView.setUserData(filePath);
		imageView.getStyleClass().add("sticker-image");
		TooltipUtils.install(imageView, StickerView.buildStickerName(filePath.getFileName().toString()));
		return imageView;
	}
}
