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

package io.xeres.ui.custom.asyncimage;

import io.micrometer.common.util.StringUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Function;

/**
 * An AsyncImageView subclass that provides a default image when there's
 * nothing to show.
 */
public class PlaceholderImageView extends StackPane
{
	private final AsyncImageView asyncImageView;
	private String iconLiteral;
	private FontIcon defaultIcon;

	private Double fitWidth;
	private Double fitHeight;
	private Boolean autoResize;

	public PlaceholderImageView(Function<String, byte[]> loader, String iconLiteral, ImageCache imageCache)
	{
		super();

		this.iconLiteral = iconLiteral;
		asyncImageView = new AsyncImageView(loader, imageCache)
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

	// For FXML
	@SuppressWarnings("unused")
	public PlaceholderImageView(@NamedArg(value = "fitWidth") Double fitWidth, @NamedArg(value = "fitHeight") Double fitHeight, @NamedArg(value = "autoResize") Boolean autoResize)
	{
		super();

		this.fitWidth = fitWidth;
		this.fitHeight = fitHeight;
		this.autoResize = autoResize;

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

		initialize();
	}

	private void initialize()
	{
		if (fitWidth != null && fitWidth != 0)
		{
			setFitWidth(fitWidth);
		}
		if (fitHeight != null && fitHeight != 0)
		{
			setFitHeight(fitHeight);
		}
		if (autoResize != null)
		{
			setPreserveRatio(autoResize);
		}
		updateDimensions();
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
		fitWidth = value;
		asyncImageView.setFitWidth(value);
	}

	public void setFitHeight(double value)
	{
		fitHeight = value;
		asyncImageView.setFitHeight(value);
	}

	public void setPreserveRatio(boolean value)
	{
		autoResize = value;
		asyncImageView.setPreserveRatio(value);
	}

	public void setUrl(String url)
	{
		updateDimensions();
		asyncImageView.setUrl(url);
	}

	private void updateDimensions()
	{
		if (autoResize == null || !autoResize)
		{
			var sizeSet = false;
			if (fitWidth != null && fitWidth != 0)
			{
				setMinWidth(fitWidth);
				sizeSet = true;
			}
			if (fitHeight != null && fitHeight != 0)
			{
				setMinHeight(fitHeight);
				sizeSet = true;
			}
			if (sizeSet)
			{
				asyncImageView.setPreserveRatio(true);
			}
		}
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
		updateDimensions();
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
		updateDimensions();
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
