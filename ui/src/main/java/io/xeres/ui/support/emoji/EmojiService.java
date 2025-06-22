/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.xeres.ui.properties.UiClientProperties;
import io.xeres.ui.support.util.SmileyUtils;
import javafx.scene.image.Image;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EmojiService
{
	private static final String DEFAULT_UNICODE = "2753"; // question mark
	private static final String EMOJI_PATH = "/image/emojis/";
	private static final String EMOJI_EXTENSION = ".png";

	private final UiClientProperties uiClientProperties;
	private RsEmojiAlias rsEmojiAlias;
	private final Map<String, WeakReference<Image>> imageCacheMap = new ConcurrentHashMap<>();
	private final Pattern aliasPattern;

	public EmojiService(UiClientProperties uiClientProperties, ObjectMapper objectMapper)
	{
		this.uiClientProperties = uiClientProperties;

		if (uiClientProperties.isRsEmojisAliases())
		{
			rsEmojiAlias = new RsEmojiAlias(objectMapper);
			aliasPattern = Pattern.compile("\\w{1," + rsEmojiAlias.getLongestAlias() + "}");
		}
		else
		{
			aliasPattern = null;
		}
	}

	public String toUnicode(String input)
	{
		var s = input;
		if (uiClientProperties.isSmileyToUnicode())
		{
			s = SmileyUtils.smileysToUnicode(input); // ;-)
		}
		if (rsEmojiAlias != null)
		{
			s = parseRsEmojiAliases(s); // :wink:
		}
		return s;
	}

	public boolean isColoredEmojis()
	{
		return uiClientProperties.isColoredEmojis();
	}

	private String parseRsEmojiAliases(String s)
	{
		if (s.length() < 3)
		{
			return s;
		}

		var start = 0;
		while ((start = s.indexOf(':', start)) != -1 && s.length() >= start + 2)
		{
			int end = s.indexOf(':', start + 2);
			if (end == -1)
			{
				break;
			}

			if (end - start > rsEmojiAlias.getLongestAlias() + 1)
			{
				// Overshot, keep searching
				start = end;
				continue;
			}

			var range = s.substring(start + 1, end);

			if (!aliasPattern.matcher(range).matches())
			{
				// Not an alias
				start = end;
				continue;
			}

			var alias = rsEmojiAlias.getUnicodeForAlias(range);
			if (alias != null)
			{
				var codePoints = getCodepoints(alias);
				s = s.substring(0, start) + getCodepoints(alias) + s.substring(end + 1);
				start += codePoints.length();
			}
			else
			{
				start = end + 1;
			}
		}
		return s;
	}

	private static String getCodepoints(String unicode)
	{
		return Arrays.stream(unicode.split("-"))
				.map(s -> Character.toString(Integer.parseUnsignedInt(s, 16)))
				.collect(Collectors.joining());
	}

	public Image getEmoji(String emoji)
	{
		return getImage(emojiToFileName(emoji));
	}

	String emojiToFileName(String emoji)
	{
		var fileName = emoji.codePoints()
				.mapToObj(Integer::toHexString)
				.collect(Collectors.joining("-"));
		if (!fileName.contains("-200d")) // Twemoji doesn't use the fully qualified names
		{
			fileName = fileName.replace("-fe0f", "");
		}
		return fileName;
	}

	private Image getImage(String unicode)
	{
		var reference = imageCacheMap.get(unicode);
		if (reference == null || reference.get() == null)
		{
			try (var resource = getExistingUnicodeResource(unicode))
			{
				reference = new WeakReference<>(new Image(Objects.requireNonNull(resource)));
				imageCacheMap.put(unicode, reference);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		return reference.get();
	}

	private InputStream getExistingUnicodeResource(String unicode)
	{
		if (getClass().getResource(EMOJI_PATH + unicode + EMOJI_EXTENSION) == null)
		{
			if (getClass().getResource(EMOJI_PATH + DEFAULT_UNICODE + EMOJI_EXTENSION) == null)
			{
				throw new IllegalArgumentException("Missing emoji default resource");
			}
			return getClass().getResourceAsStream(EMOJI_PATH + DEFAULT_UNICODE + EMOJI_EXTENSION);
		}
		return getClass().getResourceAsStream(EMOJI_PATH + unicode + EMOJI_EXTENSION);
	}
}
