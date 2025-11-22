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

package io.xeres.common.util.image;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This class contains private utility methods for working with JPEG images.
 */
final class JpegUtils
{
	private JpegUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static byte[] compressBufferedImageToJpegArray(BufferedImage image, float quality) throws IOException
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

	static BufferedImage stripAlphaIfNeeded(BufferedImage originalImage)
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
}
