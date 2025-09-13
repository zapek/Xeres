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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Handles shortcodes produced by Retroshare. Since they are sent directly by it in the wire,
 * typos in its database should be preserved.
 */
class RsEmojiAlias
{
	private static final Logger log = LoggerFactory.getLogger(RsEmojiAlias.class);

	private static final String EMOTES_DATABASE = "/retroshare-emojis.json";

	private Map<String, String> aliasesMap;
	private int longestAlias;

	private record AliasEntity(String alias, String unicode)
	{
	}

	RsEmojiAlias(ObjectMapper objectMapper)
	{
		try
		{
			var loadedAliases = objectMapper.readValue(Objects.requireNonNull(getClass().getResourceAsStream(EMOTES_DATABASE)),
					new TypeReference<List<AliasEntity>>()
					{
					});

			log.debug("Loaded {} Retroshare emoji aliases", loadedAliases.size());

			aliasesMap = HashMap.newHashMap(loadedAliases.size());

			loadedAliases.forEach(aliasEntity -> aliasesMap.put(aliasEntity.alias(), aliasEntity.unicode()));
			longestAlias = aliasesMap.keySet().stream()
					.max(Comparator.comparingInt(String::length))
					.orElseThrow().length();
		}
		catch (IOException e)
		{
			log.error("Couldn't load Retroshare emoji alias database", e);
			aliasesMap = Map.of();
			longestAlias = 0;
		}
	}

	/**
	 * Gets the Unicode emoji for the alias.
	 *
	 * @param alias the shortcode, for example <i>wink</i>
	 * @return the unicode emoji
	 */
	public String getUnicodeForAlias(String alias)
	{
		return aliasesMap.get(alias);
	}

	/**
	 * Gets the longest alias in the database, for optimization purposes.
	 * @return the longest alias in the database
	 */
	public int getLongestAlias()
	{
		return longestAlias;
	}
}
