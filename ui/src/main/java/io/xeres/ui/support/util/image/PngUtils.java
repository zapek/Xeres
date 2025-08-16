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

package io.xeres.ui.support.util.image;

import com.googlecode.pngtastic.core.PngImage;
import com.googlecode.pngtastic.core.PngOptimizer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PackedColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This class contains private utility methods for working with PNG images.
 */
final class PngUtils
{
	private PngUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static BufferedImage convertToIndexedPng(BufferedImage image)
	{
		var indexedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_INDEXED, getOrCreateIndexedColorModel(image));
		// Using drawImage with an indexed color model always produces dithering which looks ugly for stickers, so we copy manually
		for (var x = 0; x < image.getWidth(); x++)
		{
			for (var y = 0; y < image.getHeight(); y++)
			{
				indexedImage.setRGB(x, y, image.getRGB(x, y));
			}
		}
		return indexedImage;
	}

	/**
	 * Creates or uses an indexed color model for the given image.
	 *
	 * @param image the image
	 * @return and indexed color model
	 */
	private static IndexColorModel getOrCreateIndexedColorModel(BufferedImage image)
	{
		var colorModel = image.getColorModel();

		if (colorModel instanceof IndexColorModel indexColorModel)
		{
			return indexColorModel;
		}
		else if (colorModel instanceof PackedColorModel || colorModel instanceof ComponentColorModel)
		{
			return createOptimizedPalette(image, 256);
		}
		else
		{
			throw new IllegalArgumentException("Unsupported color model: " + colorModel.getClass().getSimpleName());
		}
	}

	/**
	 * Creates an optimized palette for the given image.
	 * Uses a median-cut algorithm to create a palette with the given number of colors.
	 * Transparency is preserved, but only one level is used, so it can still give
	 * some rough edges.
	 *
	 * @param image       the image
	 * @param paletteSize the size of the palette. Should be a power of 2 or colors will be wasted.
	 * @return the optimized palette
	 */
	static IndexColorModel createOptimizedPalette(BufferedImage image, int paletteSize)
	{
		if (paletteSize < 1 || paletteSize > 256)
		{
			throw new IllegalArgumentException("Palette size must be between 1 and 256");
		}

		// Extract ARGB pixels and check if we have transparency
		int width = image.getWidth();
		int height = image.getHeight();
		var argbPixels = new int[width * height];
		image.getRGB(0, 0, width, height, argbPixels, 0, width);

		List<int[]> opaquePixels = new ArrayList<>();
		var hasTransparency = false;

		for (int argb : argbPixels)
		{
			int a = (argb >> 24) & 0xff;
			if (a < 128)
			{
				hasTransparency = true;
			}
			else
			{
				opaquePixels.add(new int[]{
						(argb >> 16) & 0xff, // R
						(argb >> 8) & 0xff,  // G
						argb & 0xff          // B
				});
			}
		}

		// Adjust palette size for transparency
		int colorSlots = hasTransparency ? paletteSize - 1 : paletteSize;
		colorSlots = Math.max(1, colorSlots);  // Ensure at least 1 color

		List<int[]> palette = medianCut(opaquePixels, colorSlots);

		// Create IndexColorModel
		var r = new byte[paletteSize];
		var g = new byte[paletteSize];
		var b = new byte[paletteSize];
		var a = new byte[paletteSize];

		// Set transparent entry if needed
		var firstColorIdx = 0;
		if (hasTransparency)
		{
			firstColorIdx = 1; // The first color is transparent
		}

		// Fill palette colors
		for (var i = 0; i < palette.size(); i++)
		{
			int idx = firstColorIdx + i;
			if (idx >= paletteSize)
			{
				break;  // Safety check
			}

			int[] color = palette.get(i);
			r[idx] = (byte) color[0];
			g[idx] = (byte) color[1];
			b[idx] = (byte) color[2];
			a[idx] = (byte) 0xff;  // Opaque
		}

		return new IndexColorModel(
				Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1),
				paletteSize,
				r, g, b, a
		);
	}

	/**
	 * Creates a palette by performing a median-cut algorithm, given all pixels from an image:
	 * <ul>
	 *     <li>Find the color channel (R, G, or B) with the greatest range
	 *     <li>Sort pixels by that channel and split them into two buckets at the median
	 *     <li>Repeat recursively until the desired number of buckets (colors) is reached
	 *     <li>Average each bucket to create the final palette
	 * </ul>
	 * <p>
	 *
	 * @param pixels    a list of pixels
	 * @param maxColors the maximum number of colors in the palette
	 * @return the palette
	 */
	private static List<int[]> medianCut(List<int[]> pixels, int maxColors)
	{
		List<List<int[]>> buckets = new ArrayList<>();
		buckets.add(pixels);

		while (buckets.size() < maxColors)
		{
			List<List<int[]>> newBuckets = new ArrayList<>();
			for (List<int[]> bucket : buckets)
			{
				if (bucket.size() <= 1)
				{
					newBuckets.add(bucket);
					continue;
				}

				int channel = findChannel(bucket);

				bucket.sort(Comparator.comparingInt(pixel -> pixel[channel]));
				int median = bucket.size() / 2;
				newBuckets.add(bucket.subList(0, median));
				newBuckets.add(bucket.subList(median, bucket.size()));
			}
			buckets = newBuckets;
		}

		// Average buckets to create a palette
		List<int[]> palette = new ArrayList<>();
		for (List<int[]> bucket : buckets)
		{
			if (bucket.isEmpty())
			{
				continue;
			}
			palette.add(getAverage(bucket));
		}
		return palette;
	}

	/**
	 * Finds the channel with the greatest range.
	 *
	 * @param bucket the bucket of pixels
	 * @return the channel with the greatest range (0, 1 or 2)
	 */
	private static int findChannel(List<int[]> bucket)
	{
		int[] min = {255, 255, 255};
		int[] max = {0, 0, 0};
		for (int[] pixel : bucket)
		{
			for (var i = 0; i < 3; i++)
			{
				min[i] = Math.min(min[i], pixel[i]);
				max[i] = Math.max(max[i], pixel[i]);
			}
		}

		// Sort and split
		if (max[0] - min[0] >= max[1] - min[1])
		{
			if (max[0] - min[0] >= max[2] - min[2])
			{
				return 0;
			}
			else
			{
				return 2;
			}
		}
		else
		{
			if (max[1] - min[1] >= max[2] - min[2])
			{
				return 1;
			}
			else
			{
				return 2;
			}
		}
	}

	private static int[] getAverage(List<int[]> bucket)
	{
		int[] avg = {0, 0, 0};
		for (int[] pixel : bucket)
		{
			for (var i = 0; i < 3; i++)
			{
				avg[i] += pixel[i];
			}
		}
		for (var i = 0; i < 3; i++)
		{
			avg[i] /= bucket.size();
		}
		return avg;
	}

	private static int qualityToCompressionLevel(int quality)
	{
		return switch (quality)
		{
			case 3 -> 3;
			case 2 -> 6;
			case 1 -> 9;
			default -> throw new IllegalStateException("Unexpected value: " + quality);
		};
	}

	/**
	 * Optimizes the given PNG image using the given compression level.
	 *
	 * @param in      the PNG image as a byte array
	 * @param quality the quality (3, 2 and 1, 1 being best but most CPU intensive)
	 * @return the optimized PNG image as a byte array
	 * @throws IOException if an I/O error occurs
	 */
	static byte[] optimizePng(byte[] in, int quality) throws IOException
	{
		int compressionLevel = qualityToCompressionLevel(quality);
		var pngImage = new PngImage(in);
		var optimizer = new PngOptimizer();
		var pngOut = optimizer.optimize(pngImage, true, compressionLevel);
		var outputStream = new ByteArrayOutputStream();
		pngOut.writeDataOutputStream(outputStream);
		return outputStream.toByteArray();
	}

	/**
	 * Compresses the given image to PNG using the default compression level.
	 * <p>
	 * This compressor doesn't compress very well, and it's a good idea to run it through an optimizer.
	 *
	 * @param image the image to compress
	 * @return the compressed image as a byte array
	 * @throws IOException if an I/O error occurs
	 */
	static byte[] compressBufferedImageToPngArray(BufferedImage image) throws IOException
	{
		var pngWriter = ImageIO.getImageWritersByFormatName("PNG").next();
		var pngWriteParam = pngWriter.getDefaultWriteParam();
		pngWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		pngWriteParam.setCompressionQuality(0.5f); // This compressor is actually pretty bad and doesn't change much depending on the quality

		var out = new ByteArrayOutputStream();

		var ios = ImageIO.createImageOutputStream(out);
		pngWriter.setOutput(ios);
		var outputImage = new IIOImage(image, null, null);
		pngWriter.write(null, outputImage, pngWriteParam);
		var result = out.toByteArray();
		pngWriter.dispose();
		return result;
	}
}
