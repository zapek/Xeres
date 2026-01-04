/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.app.util;

import io.xeres.common.util.image.ImageUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class GxsUtils
{
	public static final long IMAGE_MAX_INPUT_SIZE = 1024 * 1024 * 10L; // 10 MB;

	public static final int MAXIMUM_GXS_MESSAGE_SIZE = 199_000;

	private GxsUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Gets a scaled image for GxS groups.
	 *
	 * @param imageFile the image file
	 * @param sideSize  the side size, usually 64 pixels
	 * @return a scaled image array
	 * @throws IOException if there's an I/O error
	 */
	public static byte[] getScaledGroupImage(MultipartFile imageFile, int sideSize) throws IOException
	{
		if (imageFile == null || imageFile.isEmpty())
		{
			throw new IllegalArgumentException("Image is empty");
		}

		if (imageFile.getSize() >= IMAGE_MAX_INPUT_SIZE)
		{
			throw new IllegalArgumentException("Image file size is bigger than " + IMAGE_MAX_INPUT_SIZE + " bytes");
		}

		var image = ImageUtils.setImageSquareAndCrop(ImageIO.read(imageFile.getInputStream()), sideSize);
		var imageOut = new ByteArrayOutputStream();
		if (ImageUtils.isPossiblyTransparent(imageFile.getContentType()))
		{
			if (!ImageUtils.writeImageAsPng(image, MAXIMUM_GXS_MESSAGE_SIZE - 2000, imageOut))
			{
				throw new IllegalArgumentException("Couldn't write the image. Unsupported format (transparent)?");
			}
		}
		else
		{
			if (!ImageUtils.writeImageAsJpeg(image, MAXIMUM_GXS_MESSAGE_SIZE - 2000, imageOut))
			{
				throw new IllegalArgumentException("Couldn't write the image. Unsupported format (non-transparent)?");
			}
		}
		return imageOut.toByteArray();
	}
}
