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

package io.xeres.common.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public final class LottieUtils
{
	private static final Logger log = LoggerFactory.getLogger(LottieUtils.class);

	private static final String DATA_VIDEO_LOTTIE_BASE_64 = "data:video/lottie+gzip;base64,";

	private LottieUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static String writeLottieData(byte[] lottie)
	{
		return DATA_VIDEO_LOTTIE_BASE_64 + Base64.getEncoder().encodeToString(lottie);
	}

	public static boolean isLottieData(String dataUri)
	{
		return StringUtils.isNotEmpty(dataUri) && dataUri.startsWith(DATA_VIDEO_LOTTIE_BASE_64);
	}

	public static boolean isLottieSizeSmallEnough(long size, int maximumSize)
	{
		return maximumSize == 0 || Math.ceil((double) size / 3) * 4 <= maximumSize - 200;
	}

	public static byte[] readLottieData(String dataUri)
	{
		var base64Data = dataUri.substring(dataUri.indexOf(',') + 1);
		try
		{
			return Base64.getDecoder().decode(base64Data);
		}
		catch (IllegalArgumentException e)
		{
			log.debug("Base64 decoding error: {}", e.getMessage());
			return new byte[0];
		}
	}
}
