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

package io.xeres.ui.support.util;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.util.OsUtils;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.preference.PreferenceUtils;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Dimension2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static io.xeres.ui.support.preference.PreferenceUtils.IMAGE_VIEW;
import static io.xeres.ui.support.util.DateUtils.DATE_TIME_FILENAME_FORMAT;
import static io.xeres.ui.support.util.UiUtils.getWindow;
import static javafx.scene.control.Alert.AlertType.ERROR;

public final class ImageViewUtils
{
	private static final Logger log = LoggerFactory.getLogger(ImageViewUtils.class);

	private ImageViewUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static final ContextMenu contextMenu;
	private static final ResourceBundle bundle = I18nUtils.getBundle();
	private static final String FULL_SCREEN_HINT_DISPLAYED = "FullScreenHintDisplayed";

	static
	{
		var viewMenuItem = new MenuItem(bundle.getString("view-fullscreen"));
		viewMenuItem.setGraphic(new FontIcon(MaterialDesignI.IMAGE));
		viewMenuItem.setOnAction(ImageViewUtils::view);

		var copyMenuItem = new MenuItem(bundle.getString("copy-image"));
		copyMenuItem.setGraphic(new FontIcon(MaterialDesignC.CONTENT_COPY));
		copyMenuItem.setOnAction(ImageViewUtils::copyToClipboard);

		var saveAsMenuItem = new MenuItem(bundle.getString("save-image-as"));
		saveAsMenuItem.setGraphic(new FontIcon(MaterialDesignC.CONTENT_SAVE));
		saveAsMenuItem.setOnAction(ImageViewUtils::saveAs);

		contextMenu = new ContextMenu(viewMenuItem, new SeparatorMenuItem(), copyMenuItem, saveAsMenuItem);
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
			if (imageView instanceof AsyncImageView asyncImageView)
			{
				asyncImageView.updateImage(scaleImageView.snapshot(parameters, null));
			}
			else
			{
				imageView.setImage(scaleImageView.snapshot(parameters, null));
			}
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
			if (imageView instanceof AsyncImageView asyncImageView)
			{
				asyncImageView.updateImage(scaleImageView.snapshot(parameters, null));
			}
			else
			{
				imageView.setImage(scaleImageView.snapshot(parameters, null));
			}
		}
	}

	/**
	 * Limits the size of an image by scaling it down. The aspect ratio is always preserved.
	 *
	 * @param width         the image width
	 * @param height        the image height
	 * @param maximumWidth  the width constraint
	 * @param maximumHeight the height constraint
	 * @return a dimension that doesn't exceed the maximum width nor the maximum height
	 */
	public static Dimension2D limitMaximumImageSize(double width, double height, int maximumWidth, int maximumHeight)
	{
		var ratio = width / height;
		if (width > maximumWidth)
		{
			width = maximumWidth;
			height = width / ratio;
		}
		if (height > maximumHeight)
		{
			height = maximumHeight;
			width = ratio * height;
		}
		return new Dimension2D(width, height);
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

	/**
	 * Removes ImageView's output scaling so that it's not zoomed in on 4K monitors.
	 *
	 * @param imageView the imageview
	 * @param parent    the parent's node. can be null, in that case the primary screen is used, but this should be avoided
	 */
	public static void disableOutputScaling(ImageView imageView, Node parent)
	{
		Objects.requireNonNull(imageView);

		var screen = getScreen(parent);
		if (screen == null)
		{
			log.warn("Failed to get screen while trying to disable output scaling");
			return;
		}

		var image = imageView.getImage();
		if (image == null)
		{
			log.warn("Failed to get image while trying to disable output scaling");
			return;
		}

		imageView.setFitWidth(image.getWidth() / screen.getOutputScaleX());
		imageView.setFitHeight(image.getHeight() / screen.getOutputScaleY());
	}

	/**
	 * Adds a context menu action to an image with view fullscreen, save as and copy to clipboard.
	 * @param node the node to add the context menu to
	 */
	public static void addImageContextMenuActions(Node node)
	{
		node.setOnContextMenuRequested(event -> {
			contextMenu.show(node, event.getScreenX(), event.getScreenY());
			event.consume();
		});
		UiUtils.setOnPrimaryMouseClicked(node, ImageViewUtils::view);
	}

	private static void copyToClipboard(ActionEvent event)
	{
		var selectedMenuItem = (MenuItem) event.getTarget();

		var popup = Objects.requireNonNull(selectedMenuItem.getParentPopup());
		ClipboardUtils.copyImageToClipboard(((ImageView) popup.getOwnerNode()).getImage());
	}

	private static void saveAs(ActionEvent event)
	{
		SaveFormat saveFormat;

		var selectedMenuItem = (MenuItem) event.getTarget();

		var popup = Objects.requireNonNull(selectedMenuItem.getParentPopup());
		var bufferedImage = SwingFXUtils.fromFXImage(((ImageView) popup.getOwnerNode()).getImage(), null);
		if (bufferedImage == null)
		{
			UiUtils.showAlert(ERROR, "Unsupported image format");
			return;
		}
		if (bufferedImage.getColorModel().hasAlpha())
		{
			saveFormat = new SaveFormat("PNG", List.of("*.png"));
		}
		else
		{
			saveFormat = new SaveFormat("JPG", List.of("*.jpg", "*.jpeg", "*.jfif"));
		}

		var fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("file-requester.save-image-title"));
		ChooserUtils.setInitialDirectory(fileChooser, OsUtils.getDownloadDir());
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(saveFormat.format(), saveFormat.extensions()));
		fileChooser.setInitialFileName("Image_" + DATE_TIME_FILENAME_FORMAT.format(Instant.now()) + saveFormat.getPrimaryExtension());

		var selectedFile = fileChooser.showSaveDialog(getWindow(event));
		if (selectedFile != null)
		{
			try
			{
				if (!ImageIO.write(bufferedImage, saveFormat.format(), selectedFile))
				{
					UiUtils.showAlert(ERROR, "Couldn't find a writer");
				}
			}
			catch (IOException e)
			{
				UiUtils.showAlert(ERROR, e.getMessage());
			}
		}
	}

	private static void view(ActionEvent event)
	{
		var selectedMenuItem = (MenuItem) event.getTarget();

		var popup = Objects.requireNonNull(selectedMenuItem.getParentPopup());
		view((ImageView) popup.getOwnerNode());
	}

	private static void view(MouseEvent event)
	{
		if (event.getButton() != MouseButton.PRIMARY)
		{
			return;
		}
		view((ImageView) event.getTarget());
	}

	private static void view(ImageView imageView)
	{
		var fullImageView = new ImageView();
		fullImageView.setPreserveRatio(true);
		fullImageView.setPickOnBounds(true);
		fullImageView.setImage(imageView.getImage());

		var hbox = new HBox(fullImageView);
		HBox.setHgrow(fullImageView, Priority.ALWAYS);
		hbox.setAlignment(Pos.CENTER);

		var vbox = new VBox(hbox);
		VBox.setVgrow(hbox, Priority.ALWAYS);

		var scene = new Scene(vbox, imageView.getImage().getWidth(), imageView.getImage().getHeight());
		var stage = new Stage();
		stage.setScene(scene);
		stage.setFullScreen(true);
		var prefNode = PreferenceUtils.getPreferences().node(IMAGE_VIEW);
		if (prefNode.getBoolean(FULL_SCREEN_HINT_DISPLAYED, false))
		{
			stage.setFullScreenExitHint(""); // Don't show the hint anymore
		}
		else
		{
			prefNode.putBoolean(FULL_SCREEN_HINT_DISPLAYED, true);
			stage.setFullScreenExitHint(bundle.getString("content-image.exit"));
		}
		scene.setOnMouseClicked(mouseEvent -> {
			if (mouseEvent.getButton() == MouseButton.PRIMARY)
			{
				stage.hide();
			}
		});
		scene.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ESCAPE)
			{
				stage.hide();
			}
		});
		stage.show();
	}

	private record SaveFormat(String format, List<String> extensions)
	{
		String getPrimaryExtension()
		{
			return extensions.getFirst().substring(1);
		}
	}
}
