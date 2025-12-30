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

import io.micrometer.common.util.StringUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * An AsyncImageView subclass that provides a default image when there's
 * nothing to show.
 */
public class PlaceholderImageView extends StackPane
{
	private static final Logger log = LoggerFactory.getLogger(PlaceholderImageView.class);

	private final AsyncImageView asyncImageView;
	private String iconLiteral;
	private FontIcon defaultIcon;

	public PlaceholderImageView(Function<String, byte[]> loader, String iconLiteral, ImageCache imageCache)
	{
		super();

		this.iconLiteral = iconLiteral;
		asyncImageView = new AsyncImageView(loader, null, imageCache)
		{
			@Override
			public void updateImage(Image image)
			{
				setImageOrDefault(image);
				super.updateImage(image);
			}
		};
		getChildren().add(asyncImageView);
	}

	// Needed for FXML
	public PlaceholderImageView()
	{
		super();

		asyncImageView = new AsyncImageView()
		{
			@Override
			public void updateImage(Image image)
			{
				setImageOrDefault(image);
				super.updateImage(image);
			}
		};
		getChildren().add(asyncImageView);
	}

	public void setIconLiteral(String iconLiteral)
	{
		this.iconLiteral = iconLiteral;
	}

	public void setLoader(Function<String, byte[]> loader)
	{
		asyncImageView.setLoader(loader);
	}

	public void setImageCache(ImageCache imageCache)
	{
		asyncImageView.setImageCache(imageCache);
	}

	public ObjectProperty<Image> imageProperty()
	{
		return asyncImageView.imageProperty();
	}

	public void updateImage(Image image)
	{
		asyncImageView.updateImage(image);
	}

	public void setFitWidth(double value)
	{
		asyncImageView.setFitWidth(value);
	}

	public void setFitHeight(double value)
	{
		asyncImageView.setFitHeight(value);
	}

	public void setPreserveRatio(boolean value)
	{
		asyncImageView.setPreserveRatio(value);
	}

	public void setUrl(String url)
	{
		asyncImageView.setUrl(url);
	}

	private void setImageOrDefault(Image image)
	{
		if (image == null)
		{
			showDefault();
		}
		else
		{
			hideDefault();
		}
	}

	public void showDefault()
	{
		if (StringUtils.isBlank(iconLiteral))
		{
			return; // No default to show
		}

		if (defaultIcon == null)
		{
			defaultIcon = new FontIcon(iconLiteral);
			getChildren().add(defaultIcon);
		}

		var minSize = (int) Math.min(asyncImageView.getFitWidth(), asyncImageView.getFitHeight());
		if (minSize > 0)
		{
			UiUtils.setIconSize(defaultIcon, minSize);
		}
		UiUtils.setPresent(defaultIcon);
	}

	public void hideDefault()
	{
		if (defaultIcon != null)
		{
			UiUtils.setAbsent(defaultIcon);
		}
	}

	public Image getImage()
	{
		return asyncImageView.getImage();
	}
}
