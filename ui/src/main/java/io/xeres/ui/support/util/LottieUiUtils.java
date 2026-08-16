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

package io.xeres.ui.support.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lottie4j.core.model.animation.Animation;
import io.xeres.common.util.LottieUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public final class LottieUiUtils
{
	private static final Logger log = LoggerFactory.getLogger(LottieUiUtils.class);

	private static final JsonMapper JSON_MAPPER = createJsonMapper();

	private LottieUiUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Animation decodeLottie(String dataUri)
	{
		return decodeLottie(new ByteArrayInputStream(LottieUtils.readLottieData(dataUri)));
	}

	public static Animation decodeLottie(InputStream in)
	{
		try (var gzipInputStream = new GZIPInputStream(in))
		{
			var bytes = gzipInputStream.readAllBytes();

			return JSON_MAPPER.readValue(bytes, Animation.class);
		}
		catch (IOException | JacksonException e)
		{
			log.debug("Couldn't decode lottie: {}", e.getMessage());
			return null;
		}
	}

	private static JsonMapper createJsonMapper()
	{
		// See ObjectMapperFactory of Lottie
		return JsonMapper.builder()
				.enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
				.changeDefaultPropertyInclusion(i -> i.withValueInclusion(JsonInclude.Include.NON_NULL))
				.build();
	}
}
