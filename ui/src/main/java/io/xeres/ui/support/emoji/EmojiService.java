/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.emoji;

import io.xeres.ui.properties.UiClientProperties;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class EmojiService
{
	private static final Logger log = LoggerFactory.getLogger(EmojiService.class);

	private static final String DEFAULT_UNICODE = "2753"; // question mark
	private static final String EMOJI_PATH = "/image/emojis/";
	private static final String EMOJI_EXTENSION = ".png";
	private static final Pattern CODE_DECIMAL_PATTERN = Pattern.compile("&#(\\d{1,10});");

	private final UiClientProperties uiClientProperties;
	private final Map<String, WeakReference<Image>> imageCacheMap = new ConcurrentHashMap<>();

	public EmojiService(UiClientProperties uiClientProperties)
	{
		this.uiClientProperties = uiClientProperties;
	}

	public boolean isEnabled()
	{
		return uiClientProperties.isColoredEmojis();
	}

	public Image getEmoji(String codeDecimal)
	{
		return getImage(codeDecimalToUnicode(codeDecimal));
	}

	String codeDecimalToUnicode(String codeDecimal)
	{
		StringBuilder result = new StringBuilder();

		var matcher = CODE_DECIMAL_PATTERN.matcher(codeDecimal);
		while (matcher.find())
		{
			if (!result.isEmpty())
			{
				result.append("-");
			}
			result.append(Integer.toHexString(Integer.parseUnsignedInt(matcher.group(1))));
		}
		return result.toString();
	}

	private Image getImage(String unicode)
	{
		var reference = imageCacheMap.get(unicode);
		if (reference == null || reference.get() == null)
		{
			var resource = getClass().getResourceAsStream(EMOJI_PATH + unicode + EMOJI_EXTENSION);
			if (resource == null)
			{
				resource = getClass().getResourceAsStream(EMOJI_PATH + DEFAULT_UNICODE + EMOJI_EXTENSION);
			}
			reference = new WeakReference<>(new Image(Objects.requireNonNull(resource)));
			imageCacheMap.put(unicode, reference);
		}
		return reference.get();
	}
}
