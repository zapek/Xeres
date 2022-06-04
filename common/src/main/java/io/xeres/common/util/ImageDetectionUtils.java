/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.common.util;

import org.springframework.http.MediaType;

import java.util.stream.IntStream;

public final class ImageDetectionUtils
{
	private ImageDetectionUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static final byte[] JPEG_HEADER = new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff};
	private static final byte[] PNG_HEADER = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};

	public static MediaType getImageMimeType(byte[] image)
	{
		if (isStartingWith(PNG_HEADER, image))
		{
			return MediaType.IMAGE_PNG;
		}
		else if (isStartingWith(JPEG_HEADER, image))
		{
			return MediaType.IMAGE_JPEG;
		}
		return null;
	}

	private static boolean isStartingWith(byte[] header, byte[] image)
	{
		return image.length >= header.length && IntStream.range(0, header.length).allMatch(i -> header[i] == image[i]);
	}
}
