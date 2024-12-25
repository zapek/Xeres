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

package io.xeres.ui.support.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class ImageUtils
{
	private static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

	private ImageUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static String writeImageAsPngData(Image image)
	{
		var out = new ByteArrayOutputStream();
		try
		{
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "PNG", out);
		}
		catch (IOException e)
		{
			log.error("Couldn't save image as PNG: {}", e.getMessage());
		}
		return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
	}

	public static String writeImageAsJpegData(Image image, int maximumSize)
	{
		try
		{
			byte[] out;
			var quality = 0.7f;
			var bufferedImage = stripAlphaIfNeeded(SwingFXUtils.fromFXImage(image, null));
			do
			{
				out = compressBufferedImageToJpegArray(bufferedImage, quality);
				quality -= 0.1f;
			}
			while (maximumSize != 0 && Math.ceil((double) out.length / 3) * 4 > maximumSize - 200 && quality > 0); // 200 bytes to be safe as the message might contain tags and so on

			return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(out);
		}
		catch (IOException e)
		{
			log.error("Couldn't save image as JPEG: {}", e.getMessage());
			return "";
		}
	}

	private static byte[] compressBufferedImageToJpegArray(BufferedImage image, float quality) throws IOException
	{
		var jpegWriter = ImageIO.getImageWritersByFormatName("JPEG").next();
		var jpegWriteParam = jpegWriter.getDefaultWriteParam();
		jpegWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpegWriteParam.setCompressionQuality(quality);

		var out = new ByteArrayOutputStream();

		var ios = ImageIO.createImageOutputStream(out);
		jpegWriter.setOutput(ios);
		var outputImage = new IIOImage(image, null, null);
		jpegWriter.write(null, outputImage, jpegWriteParam);
		var result = out.toByteArray();
		jpegWriter.dispose();
		return result;
	}

	private static BufferedImage stripAlphaIfNeeded(BufferedImage originalImage)
	{
		if (originalImage.getTransparency() == Transparency.OPAQUE)
		{
			return originalImage;
		}

		var w = originalImage.getWidth();
		var h = originalImage.getHeight();
		var newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		var rgb = originalImage.getRGB(0, 0, w, h, null, 0, w);
		newImage.setRGB(0, 0, w, h, rgb, 0, w);
		return newImage;
	}

	public static void limitMaximumImageSize(ImageView imageView, int maximumWidth, int maximumHeight)
	{
		var width = imageView.getImage().getWidth();
		var height = imageView.getImage().getHeight();

		if (width > maximumWidth || height > maximumHeight)
		{
			var scaleImageView = new ImageView(imageView.getImage());
			if (width > height)
			{
				scaleImageView.setFitWidth(maximumWidth);
			}
			else
			{
				scaleImageView.setFitHeight(maximumHeight);
			}
			scaleImageView.setPreserveRatio(true);
			scaleImageView.setSmooth(true);
			imageView.setImage(scaleImageView.snapshot(null, null));
		}
	}

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
			imageView.setImage(scaleImageView.snapshot(null, null));
		}
	}
}
