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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.xeres.ui.properties.UiClientProperties;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmojiService
{
	private static final Logger log = LoggerFactory.getLogger(EmojiService.class);

	private static final String DEFAULT_UNICODE = "2753"; // question mark
	private static final String EMOJI_PATH = "/image/emojis/";
	private static final String EMOJI_EXTENSION = ".png";
	private static final String EMOJI_DATABASE = "/emoji.json";

	private final ObjectMapper objectMapper;
	private final UiClientProperties uiClientProperties;
	private final Map<String, String> emojisMap = new HashMap<>();
	private final Map<String, WeakReference<Image>> imageCacheMap = new ConcurrentHashMap<>();

	private record EmojiEntity(String unicode, @JsonProperty("code_decimal") String codeDecimal)
	{
	}

	public EmojiService(ObjectMapper objectMapper, UiClientProperties uiClientProperties)
	{
		this.objectMapper = objectMapper;
		this.uiClientProperties = uiClientProperties;

		if (uiClientProperties.isColoredEmojis())
		{
			log.info("Loading colored Emojis...");
			loadEmojis();
		}
	}

	private void loadEmojis()
	{
		try
		{
			var loadedEmojis = objectMapper.readValue(Objects.requireNonNull(getClass().getResourceAsStream(EMOJI_DATABASE)),
					new TypeReference<List<EmojiEntity>>()
					{
					});

			loadedEmojis.forEach(emoji -> emojisMap.put(emoji.codeDecimal(), emoji.unicode()));
		}
		catch (IOException e)
		{
			log.error("Couldn't load emojis database");
		}
	}

	public boolean isEnabled()
	{
		return uiClientProperties.isColoredEmojis();
	}

	public Image getEmoji(String codeDecimal)
	{
		var unicode = emojisMap.get(codeDecimal);

		if (unicode == null)
		{
			unicode = DEFAULT_UNICODE;
		}
		return getImage(unicode);
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
