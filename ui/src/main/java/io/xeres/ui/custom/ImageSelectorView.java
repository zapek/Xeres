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

package io.xeres.ui.custom;

import io.micrometer.common.util.StringUtils;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.custom.asyncimage.PlaceholderImageView;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ResourceBundle;
import java.util.function.Function;

/**
 * A class that allows to select/remove an image. It can be supplied with a placeholder to show when there's
 * no image selected yet. The placeholder is an iconLiteral from FontIcon (for example mdi2i-image-plus).
 */
public class ImageSelectorView extends StackPane
{
	private static final double BUTTON_OPACITY = 0.8;

	@FXML
	private PlaceholderImageView imageView;

	@FXML
	private Button selectButton;

	@FXML
	private Button deleteButton;

	private boolean deletable = true;

	private final Double fitWidth;
	private final Double fitHeight;
	private final String placeholder;
	private final Boolean preserveRatio;

	private final ResourceBundle bundle;

	private String url;

	public ImageSelectorView(@NamedArg(value = "fitWidth", defaultValue = "64.0") Double fitWidth, @NamedArg(value = "fitHeight", defaultValue = "64.0") Double fitHeight, @NamedArg(value = "placeholder") String placeholder, @NamedArg(value = "preserveRatio", defaultValue = "false") Boolean preserveRatio)
	{
		super();

		bundle = I18nUtils.getBundle();

		this.fitWidth = fitWidth;
		this.fitHeight = fitHeight;
		this.placeholder = placeholder;
		this.preserveRatio = preserveRatio;

		var loader = new FXMLLoader(ImageSelectorView.class.getResource("/view/custom/image_selector_view.fxml"), bundle);
		loader.setRoot(this);
		loader.setController(this);

		try
		{
			loader.load();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public ObjectProperty<Image> imageProperty()
	{
		return imageView.imageProperty();
	}

	/**
	 * Sets the image loader. Only needed when loading from a URL is needed.
	 *
	 * @param loader the loader
	 */
	public void setImageLoader(Function<String, byte[]> loader)
	{
		imageView.setLoader(loader);
	}

	/**
	 * Sets the image cache. Only needed when images are frequently loaded.
	 * @param imageCache the image cache
	 */
	public void setImageCache(ImageCache imageCache)
	{
		imageView.setImageCache(imageCache);
	}

	/**
	 * Sets the image URL. It will be loaded asynchronously.
	 * @param url the url
	 */
	public void setImageUrl(String url)
	{
		this.url = url;
		imageView.setUrl(url);
	}

	/// Sets the file. It will be loaded asynchronously. Very useful when using a requester, for example:
	///
	/// ```java
    /// File selectedFile = fileChooser.showOpenDialog(getWindow(event));
    /// imageSelectorView.setFile(selectedFile);
    /// ```
	///
	/// @param file the file to load
	public void setFile(File file)
	{
		if (file != null && file.canRead())
		{
			url = file.toURI().toASCIIString();
			imageView.setUrl(url);
		}
	}

	/**
	 * Gets the URL that this SelectorView was set to. Also returns file URLs.
	 *
	 * @return the URL, null if not URL was set
	 */
	public String getUrl()
	{
		return url;
	}

	/**
	 * Gets the file that was set to this SelectorView, if any.
	 *
	 * @return the file, null if no file was set or the source image didn't come from any
	 */
	public File getFile()
	{
		if (url == null)
		{
			return null;
		}

		File file;
		try
		{
			file = new File(new URI(url));
		}
		catch (URISyntaxException | IllegalArgumentException _)
		{
			return null;
		}
		if (file.canRead())
		{
			return file;
		}
		return null;
	}

	/**
	 * Sets the image that will be shown.
	 * @param image the image
	 */
	public void setImage(Image image)
	{
		url = null;
		imageView.updateImage(image);
	}

	/**
	 * The action to execute when the image selector button is pressed.
	 * @param value the action event
	 */
	public void setOnSelectAction(EventHandler<ActionEvent> value)
	{
		selectButton.setOnAction(value);
	}

	/**
	 * The action to execute when the image removal button is pressed.
	 * @param value the action event
	 */
	public void setOnDeleteAction(EventHandler<ActionEvent> value)
	{
		deleteButton.setOnAction(value);
	}

	/**
	 * Checks if there's an image set at all.
	 * @return true if there's no image
	 */
	public boolean isEmpty()
	{
		return imageView.getImage() == null;
	}

	/**
	 * Shows the edit buttons.
	 *
	 * @param editable true if the image can be added and removed
	 */
	public void setEditable(boolean editable)
	{
		setEditable(editable, editable);
	}

	/**
	 * Shows the edit buttons.
	 * <p>This version is needed in case an image is not a real image (for example autogenerated)
	 * and hence the delete button would make no sense and has to be set to false.
	 *
	 * @param editable  true if an image can be added
	 * @param deletable true if the image can also be removed
	 */
	public void setEditable(boolean editable, boolean deletable)
	{
		this.deletable = deletable;

		selectButton.setVisible(editable);

		if (deletable)
		{
			if (imageView.getImage() != null && !imageView.getImage().isError())
			{
				deleteButton.setVisible(true);
			}
			else
			{
				deleteButton.setVisible(false);
			}
		}
		else
		{
			deleteButton.setVisible(false);
		}
	}

	private void setImageOpacity(double opacity)
	{
		selectButton.setOpacity(opacity);
		if (imageView.getImage() != null)
		{
			deleteButton.setOpacity(opacity);
		}
	}

	@FXML
	private void initialize()
	{
		if (fitWidth != null && fitWidth != 0)
		{
			imageView.setFitWidth(fitWidth);
		}
		if (fitHeight != null && fitHeight != 0)
		{
			imageView.setFitHeight(fitHeight);
		}
		if (preserveRatio != null)
		{
			imageView.setPreserveRatio(preserveRatio);
		}
		if (StringUtils.isNotBlank(placeholder))
		{
			imageView.setIconLiteral(placeholder);
			imageView.showDefault();
		}

		computeActionText();

		imageView.setOnMouseEntered(_ -> setImageOpacity(BUTTON_OPACITY));
		imageView.setOnMouseExited(_ -> setImageOpacity(0.0));
		selectButton.setOnMouseEntered(_ -> setImageOpacity(BUTTON_OPACITY));
		selectButton.setOnMouseExited(_ -> setImageOpacity(0.0));
		deleteButton.setOnMouseEntered(_ -> setImageOpacity(BUTTON_OPACITY));
		deleteButton.setOnMouseExited(_ -> setImageOpacity(0.0));

		imageView.imageProperty().addListener((_, _, newValue) -> {
			if (newValue != null && !newValue.isError())
			{
				if (deletable)
				{
					deleteButton.setVisible(true);
				}
			}
			else
			{
				if (deletable)
				{
					deleteButton.setVisible(false);
				}
			}
			computeActionText();
		});
	}

	private void computeActionText()
	{
		if (imageView.getImage() == null)
		{
			if (fitWidth <= 64)
			{
				selectButton.setText(bundle.getString("add"));
			}
			else
			{
				selectButton.setText(bundle.getString("image-selector-view.add-image"));
			}
		}
		else
		{
			if (fitWidth <= 64)
			{
				selectButton.setText(bundle.getString("image-selector-view.change-image-short"));
			}
			else
			{
				selectButton.setText(bundle.getString("image-selector-view.change-image"));
			}
		}
	}
}
