/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.util.image;

import dev.mccue.imgscalr.Scalr;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Base64;

/**
 * Provides utility methods for working with images.
 */
public final class ImageUtils
{
	private static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

	private static final String DATA_IMAGE_PNG_BASE_64 = "data:image/png;base64,";
	private static final String DATA_IMAGE_JPEG_BASE_64 = "data:image/jpeg;base64,";

	private ImageUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Writes an image as a PNG data URL. The image is optimized
	 * trying to fit the size without lowering its quality.
	 *
	 * @param image       the image
	 * @param maximumSize the maximum size of the image in bytes. If 0, no limit is applied.
	 * @return the image as a PNG data URL, or an empty string if the image couldn't be written.
	 */
	public static String writeImageAsPngData(Image image, int maximumSize)
	{
		try
		{
			byte[] out;
			var quality = 4;
			var bufferedImage = SwingFXUtils.fromFXImage(image, null);
			var baseOut = PngUtils.compressBufferedImageToPngArray(bufferedImage);

			do
			{
				out = baseOut;
				if (quality < 4)
				{
					out = PngUtils.optimizePng(baseOut, quality);
				}
				quality -= 1;
			}
			while (canCompressionPossiblyBeImproved(maximumSize, out, quality));

			return DATA_IMAGE_PNG_BASE_64 + Base64.getEncoder().encodeToString(out);
		}
		catch (IOException e)
		{
			log.error("Couldn't save image as PNG: {}", e.getMessage());
			return "";
		}
	}

	/**
	 * Writes a buffered image as a PNG data URL. The image is optimized
	 * trying to fit the size. If needed, the image is converted to indexed PNG.
	 *
	 * @param bufferedImage the buffered image
	 * @param maximumSize   the maximum size of the image in bytes. If 0, no limit is applied.
	 * @return the image as a PNG data URL, or an empty string if the image couldn't be written.
	 */
	public static String writeImageAsPngData(BufferedImage bufferedImage, int maximumSize)
	{
		try
		{
			byte[] out;
			var quality = 4;
			var baseOut = PngUtils.compressBufferedImageToPngArray(bufferedImage);

			// Try true color reduction first
			do
			{
				out = baseOut;
				if (quality < 4)
				{
					out = PngUtils.optimizePng(baseOut, quality);
				}
				quality -= 1;
			}
			while (canCompressionPossiblyBeImproved(maximumSize, out, quality));

			quality = 4;

			// If still too big, try to convert to indexed PNG and then optimize it again
			if (canCompressionPossiblyBeImproved(maximumSize, out, quality))
			{
				bufferedImage = PngUtils.convertToIndexedPng(bufferedImage);
				baseOut = PngUtils.compressBufferedImageToPngArray(bufferedImage);

				do
				{
					out = baseOut;
					if (quality < 4)
					{
						out = PngUtils.optimizePng(baseOut, quality);
					}
					quality -= 1;
				}
				while (canCompressionPossiblyBeImproved(maximumSize, out, quality));

			}
			return DATA_IMAGE_PNG_BASE_64 + Base64.getEncoder().encodeToString(out);
		}
		catch (IOException e)
		{
			log.error("Couldn't save buffered image as PNG: {}", e.getMessage());
			return "";
		}
	}

	/**
	 * Writes an image as a JPEG data URL. The image is optimized and its quality reduced
	 * until it fits the size.
	 *
	 * @param image       the image
	 * @param maximumSize the maximum size of the image in bytes. If 0, no limit is applied.
	 * @return the image as a JPEG data URL, or an empty string if the image couldn't be written.
	 */
	public static String writeImageAsJpegData(Image image, int maximumSize)
	{
		try
		{
			byte[] out;
			var quality = 0.7f;
			var bufferedImage = JpegUtils.stripAlphaIfNeeded(SwingFXUtils.fromFXImage(image, null));
			do
			{
				out = JpegUtils.compressBufferedImageToJpegArray(bufferedImage, quality);
				quality -= 0.1f;
			}
			while (canCompressionPossiblyBeImproved(maximumSize, out, quality));

			return DATA_IMAGE_JPEG_BASE_64 + Base64.getEncoder().encodeToString(out);
		}
		catch (IOException e)
		{
			log.error("Couldn't save image as JPEG: {}", e.getMessage());
			return "";
		}
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
	 * Limits the size of an image by scaling it down. The aspect ratio is always preserved.
	 * It uses a high-quality incremental scaling algorithm.
	 *
	 * @param image       the image
	 * @param maximumSize the maximum size of the image in total number of pixels
	 * @return the scaled image
	 */
	public static BufferedImage limitMaximumImageSize(BufferedImage image, int maximumSize)
	{
		var width = image.getWidth();
		var height = image.getHeight();

		var actualSize = width * height;

		if (actualSize > maximumSize)
		{
			var ratio = Math.sqrt((double) maximumSize / actualSize);
			var destWidth = (int) (width * ratio);

			// This uses incremental scaling, which is the best one
			return Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, destWidth);
		}
		return image;
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

	private static boolean canCompressionPossiblyBeImproved(int maximumSize, byte[] array, float quality)
	{
		return maximumSize != 0 && Math.ceil((double) array.length / 3) * 4 > maximumSize - 200 && quality > 0; // 200 bytes to be safe as the message might contain tags and so on
	}
}
