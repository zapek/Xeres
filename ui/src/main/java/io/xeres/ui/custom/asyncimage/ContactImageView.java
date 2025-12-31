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

package io.xeres.ui.custom.asyncimage;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.util.function.Function;

/**
 * A round image with subtle shadows.
 */
public class ContactImageView extends StackPane
{
	private final Circle circle;
	private final AsyncImageView asyncImageView;

	public ContactImageView(Function<String, byte[]> loader, ImageCache imageCache, int size)
	{
		super();

		circle = new Circle((double) size / 2);
		circle.setVisible(false);
		asyncImageView = new AsyncImageView(loader, imageCache);
		asyncImageView.setFitWidth(size);
		asyncImageView.setFitHeight(size);
		asyncImageView.setVisible(false);
		asyncImageView.setOnSuccess(() -> {
			circle.setFill(new ImagePattern(asyncImageView.getImage()));
			circle.setVisible(true);
		});
		if (Platform.isSupported(ConditionalFeature.EFFECT))
		{
			circle.setEffect(new DropShadow((double) size / 8, Color.rgb(0, 0, 0, 0.7)));
		}
		getChildren().addAll(circle, asyncImageView);
	}

	public void setUrl(String url)
	{
		circle.setVisible(false);
		asyncImageView.setUrl(url);
	}
}
