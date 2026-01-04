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

package io.xeres.common.util.image;

import dev.mccue.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * Provides utility methods for working with images.
 */
public final class ImageUtils
{
	private static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

	private static final String DATA_IMAGE_PNG_BASE_64 = "data:image/png;base64,";
	private static final String DATA_IMAGE_JPEG_BASE_64 = "data:image/jpeg;base64,";

	private static final byte[] JPEG_HEADER = new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff};
	private static final byte[] PNG_HEADER = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
	private static final byte[] GIF_HEADER = new byte[]{'G', 'I', 'F'};
	private static final byte[] RIFF_HEADER = new byte[]{'R', 'I', 'F', 'F'};
	private static final byte[] WEBP_SIGNATURE = new byte[]{'W', 'E', 'B', 'P'};

	private ImageUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Writes a buffered image as a PNG or JPEG data URL, depending on the needs,
	 * that is, a transparent image will be PNG and the rest will be JPEG. A transparent
	 * image that is effectively opaque will still result as a JPEG.
	 *
	 * @param bufferedImage the image
	 * @param maximumSize   the maximum size of the image in bytes. If 0, no limit is applied.
	 * @return the image as a PNG or JPEG data URL, or an empty string if the image couldn't be written.
	 */
	public static String writeImage(BufferedImage bufferedImage, int maximumSize)
	{
		if (isTransparent(bufferedImage))
		{
			return writeImageAsPngData(bufferedImage, maximumSize);
		}
		else
		{
			return writeImageAsJpegData(bufferedImage, maximumSize);
		}
	}

	/**
	 * Writes a buffered image as a PNG file. The image is optimized
	 * trying to fit the size. If needed, the image is converted to indexed PNG.
	 *
	 * @param bufferedImage the buffered image
	 * @param maximumSize   the maximum size of the image in bytes. If 0, no limit is applied.
	 * @param outputStream  the output stream
	 * @return true if the image could be written, false otherwise
	 */
	public static boolean writeImageAsPng(BufferedImage bufferedImage, int maximumSize, OutputStream outputStream)
	{
		try
		{
			var baseOut = new ByteArrayOutputStream();
			var quality = 4;
			PngUtils.writeBufferedImageToPng(bufferedImage, baseOut);

			// Try true color reduction first
			ByteArrayOutputStream newOut;
			do
			{
				newOut = new ByteArrayOutputStream();
				if (quality < 4)
				{
					PngUtils.optimizePng(baseOut.toByteArray(), quality, newOut);
				}
				quality -= 1;
			}
			while (canCompressionPossiblyBeImproved(maximumSize, newOut.toByteArray(), quality));

			quality = 4;

			if (newOut.size() == 0)
			{
				newOut = baseOut;
			}

			ByteArrayOutputStream newIndexedOut = null;

			// If still too big, try to convert to indexed PNG and then optimize it again
			if (canCompressionPossiblyBeImproved(maximumSize, newOut.toByteArray(), quality))
			{
				newOut = new ByteArrayOutputStream();
				bufferedImage = PngUtils.convertToIndexedPng(bufferedImage);
				PngUtils.writeBufferedImageToPng(bufferedImage, newOut);


				do
				{
					newIndexedOut = new ByteArrayOutputStream();
					if (quality < 4)
					{
						PngUtils.optimizePng(newOut.toByteArray(), quality, newIndexedOut);
					}
					quality -= 1;
				}
				while (canCompressionPossiblyBeImproved(maximumSize, newIndexedOut.toByteArray(), quality));

			}
			outputStream.write((newIndexedOut != null && newIndexedOut.size() > 0) ? newIndexedOut.toByteArray() : newOut.toByteArray());
			return true;
		}
		catch (IOException e)
		{
			log.error("Couldn't save buffered image as PNG: {}", e.getMessage());
			return false;
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
		var out = new ByteArrayOutputStream();
		if (writeImageAsPng(bufferedImage, maximumSize, out))
		{
			return DATA_IMAGE_PNG_BASE_64 + Base64.getEncoder().encodeToString(out.toByteArray());
		}
		else
		{
			return "";
		}
	}

	/**
	 * Writes an image as a JPEG file. The image is optimized and its quality reduced
	 * until it fits the size.
	 *
	 * @param bufferedImage the image
	 * @param maximumSize   the maximum size of the image in bytes. If 0, no limit is applied.
	 * @param outputStream  the output stream
	 * @return true if the image could be written, false otherwise
	 */
	public static boolean writeImageAsJpeg(BufferedImage bufferedImage, int maximumSize, OutputStream outputStream)
	{
		try
		{
			var out = new ByteArrayOutputStream();
			var quality = 0.7f;
			byte[] array;
			bufferedImage = JpegUtils.stripAlphaIfNeeded(bufferedImage);
			do
			{
				JpegUtils.writeBufferedImageToJpeg(bufferedImage, quality, out);
				array = out.toByteArray();
				quality -= 0.1f;
			}
			while (canCompressionPossiblyBeImproved(maximumSize, array, quality));

			outputStream.write(array);

			return true;
		}
		catch (IOException e)
		{
			log.error("Couldn't save image as JPEG: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Writes an image as a JPEG data URL. The image is optimized and its quality reduced
	 * until it fits the size.
	 *
	 * @param bufferedImage the image
	 * @param maximumSize   the maximum size of the image in bytes. If 0, no limit is applied.
	 * @return the image as a JPEG data URL, or an empty string if the image couldn't be written.
	 */
	public static String writeImageAsJpegData(BufferedImage bufferedImage, int maximumSize)
	{
		var out = new ByteArrayOutputStream();
		if (writeImageAsJpeg(bufferedImage, maximumSize, out))
		{
			return DATA_IMAGE_JPEG_BASE_64 + Base64.getEncoder().encodeToString(out.toByteArray());
		}
		else
		{
			return "";
		}
	}

	/**
	 * Limits the size of an image by scaling it down. The aspect ratio is always preserved.
	 * Uses a highquality incremental scaling algorithm.
	 *
	 * @param image       the image
	 * @param maximumSize the maximum size of the image in total number of pixels
	 * @return the scaled image
	 */
	public static BufferedImage limitMaximumImageSize(BufferedImage image, int maximumSize)
	{
		var width = image.getWidth();
		var height = image.getHeight();

		var size = width * height;

		if (size > maximumSize)
		{
			var reductionRatio = Math.sqrt((double) maximumSize / size);
			var destWidth = (int) (width * reductionRatio);
			var destHeight = (int) (height * reductionRatio);

			// This uses incremental scaling, which is the best one
			return Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, destWidth, destHeight);
		}
		return image;
	}

	/**
	 * Scales an image up or down. The aspect ratio is always preserved, thus the target width and height might be smaller than
	 * the parameters given. Uses a high quality incremental scaling algorithm.
	 *
	 * @param image        the image
	 * @param targetWidth  the target width
	 * @param targetHeight the target height
	 * @return the scaled image
	 */
	public static BufferedImage setImageSize(BufferedImage image, int targetWidth, int targetHeight)
	{
		var width = image.getWidth();
		var height = image.getHeight();

		// Calculate the scaling factor to fit within target dimensions
		double scaleX = (double) targetWidth / width;
		double scaleY = (double) targetHeight / height;

		// Use the smaller scale to ensure the image fits within both dimensions
		double scale = Math.min(scaleX, scaleY);

		// Calculate the resulting dimensions
		int newWidth = (int) Math.round(width * scale);
		int newHeight = (int) Math.round(height * scale);

		// Apply the scaling using imgscalr
		return Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, newWidth, newHeight);
	}

	/**
	 * Scales an image up or down. The aspect ratio is always preserved. If the source image is not square, the
	 * background is filled either by a transparent background or a white background depending on the source image.
	 *
	 * @param image    the image
	 * @param sideSize the side size
	 * @return the scaled image
	 */
	public static BufferedImage setImageSquareAndFill(BufferedImage image, int sideSize)
	{
		var scaledImage = setImageSize(image, sideSize, sideSize);

		// Determine if we need a transparent background
		boolean hasTransparency = isTransparent(image);

		// Create a new image with the specified side size
		int imageType = hasTransparency ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
		var result = new BufferedImage(sideSize, sideSize, imageType);

		// Create a graphics context to draw on the new image
		var graphics = result.createGraphics();

		// Set background color based on transparency
		if (hasTransparency)
		{
			// For transparent backgrounds, we'll use alpha to create transparency
			graphics.setComposite(AlphaComposite.Clear);
			graphics.fillRect(0, 0, sideSize, sideSize);
			graphics.setComposite(AlphaComposite.SrcOver);
		}
		else
		{
			// For non-transparent images, use white background
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, sideSize, sideSize);
		}

		// Calculate position to center the scaled image
		int x = (sideSize - scaledImage.getWidth()) / 2;
		int y = (sideSize - scaledImage.getHeight()) / 2;

		// Draw the scaled image centered on the new image
		graphics.drawImage(scaledImage, x, y, null);

		// Clean up
		graphics.dispose();

		return result;
	}

	/**
	 * Scales an image up or down. The aspect ratio is always preserved. If the source image is not square, its
	 * largest dimension is cropped.
	 *
	 * @param image    the image
	 * @param sideSize the side size
	 * @return the scaled image
	 */
	public static BufferedImage setImageSquareAndCrop(BufferedImage image, int sideSize)
	{
		// Determine if we need a transparent background
		boolean hasTransparency = isTransparent(image);

		var width = image.getWidth();
		var height = image.getHeight();

		// Calculate the scaling factor to fit within target dimensions
		double scaleX = (double) sideSize / width;
		double scaleY = (double) sideSize / height;

		// Use the smaller scale to ensure the image fits within both dimensions
		double scale = Math.max(scaleX, scaleY);

		// Calculate the resulting dimensions
		int newWidth = (int) Math.round(width * scale);
		int newHeight = (int) Math.round(height * scale);

		// Apply the scaling using imgscalr
		var scaledImage = Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, newWidth, newHeight);

		// Create a new image with the specified side size
		int imageType = hasTransparency ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
		var result = new BufferedImage(sideSize, sideSize, imageType);

		// Create a graphics context to draw on the new image
		var graphics = result.createGraphics();

		// Calculate position to center the scaled image
		int x = (sideSize - scaledImage.getWidth()) / 2;
		int y = (sideSize - scaledImage.getHeight()) / 2;

		// Draw the scaled image centered on the new image
		graphics.drawImage(scaledImage, x, y, null);

		// Clean up
		graphics.dispose();

		return result;
	}

	/**
	 * Detects the format of the image without decoding the whole.
	 * <p>
	 * Currently supported:
	 * <ul>
	 *     <li>PNG</li>
	 *     <li>JPEG</li>
	 *     <li>GIF</li>
	 *     <li>WebP</li>
	 * </ul>
	 *
	 * @param image the byte array containing the image data
	 * @return the {@link MediaType} of the image or null if unknown
	 */
	public static MediaType getImageMimeType(byte[] image)
	{
		if (image == null)
		{
			return null;
		}

		if (isStartingWith(PNG_HEADER, image))
		{
			return MediaType.IMAGE_PNG;
		}
		else if (isStartingWith(JPEG_HEADER, image))
		{
			return MediaType.IMAGE_JPEG;
		}
		else if (isStartingWith(GIF_HEADER, image))
		{
			return MediaType.IMAGE_GIF;
		}
		else if (isStartingWith(RIFF_HEADER, image) && contains(WEBP_SIGNATURE, 8, image))
		{
			return MediaType.parseMediaType("image/webp");
		}
		return null;
	}

	/**
	 * Gets an image dimension without decoding the image data.
	 *
	 * @param inputStream the input stream
	 * @return the image dimension or null if there was an error
	 */
	public static Dimension getImageDimension(InputStream inputStream)
	{
		try (var in = ImageIO.createImageInputStream(inputStream))
		{
			Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (readers.hasNext())
			{
				ImageReader reader = readers.next();
				try
				{
					reader.setInput(in);
					int width = reader.getWidth(0);
					int height = reader.getHeight(0);
					return new Dimension(width, height);
				}
				finally
				{
					reader.dispose();
				}
			}
			log.warn("Unsupported image format");
		}
		catch (IOException e)
		{
			log.warn("Invalid image file: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * Finds out if a media format is possibly transparent. This focuses more on the intent
	 * of the format and is thus unreliable because some formats support both modes.
	 *
	 * @param contentType the MIME content type (for example "image/jpeg")
	 * @return true if possibly transparent
	 */
	public static boolean isPossiblyTransparent(String contentType)
	{
		return MediaType.IMAGE_PNG_VALUE.equals(contentType) ||
				MediaType.IMAGE_GIF_VALUE.equals(contentType) ||
				"image/webp".equals(contentType) ||
				"image/svg+xml".equals(contentType) ||
				"image/x-icon".equals(contentType);
	}

	private static boolean isStartingWith(byte[] header, byte[] image)
	{
		return contains(header, 0, image);
	}

	private static boolean contains(byte[] signature, int offset, byte[] image)
	{
		return image.length >= signature.length + offset && IntStream.range(offset, signature.length).allMatch(i -> signature[i] == image[i]);
	}

	private static boolean canCompressionPossiblyBeImproved(int maximumSize, byte[] array, float quality)
	{
		return maximumSize != 0 && Math.ceil((double) array.length / 3) * 4 > maximumSize - 200 && quality > 0; // 200 bytes to be safe as the message might contain tags and so on
	}

	private static boolean isTransparent(BufferedImage bufferedImage)
	{
		var cm = bufferedImage.getColorModel();

		// IndexColorModel may define a single transparent palette index
		if (cm instanceof IndexColorModel icm)
		{
			if (icm.getTransparentPixel() != -1)
			{
				return true;
			}
			// if no transparent palette index and no alpha, it's opaque
			if (!icm.hasAlpha())
			{
				return false;
			}
		}
		else
		{
			// If color model reports no alpha channel, image is opaque
			if (!cm.hasAlpha())
			{
				return false;
			}
		}

		// Now check pixel alpha values. Use a single getRGB call for speed.
		int w = bufferedImage.getWidth();
		int h = bufferedImage.getHeight();
		int[] pixels = bufferedImage.getRGB(0, 0, w, h, null, 0, w);
		for (int argb : pixels)
		{
			int alpha = (argb >>> 24) & 0xff;
			if (alpha != 0xff)
			{ // any value less than 255 means transparent or semi-transparent
				return true;
			}
		}
		return false;
	}
}
