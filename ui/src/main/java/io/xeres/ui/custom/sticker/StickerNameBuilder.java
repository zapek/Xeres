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

package io.xeres.ui.custom.sticker;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

class StickerNameBuilder
{
	private static final Pattern PATTERN_ORDERED_NAME = Pattern.compile("^(\\d{1,3}\\.)?(.*?)(\\.\\w{1,10})?$");
	private static final List<String> HIDDEN_NAMES = List.of("png", "webp", "jpg", "jpeg", "tgs", "lottie", "json");

	private String name;

	public StickerNameBuilder name(String name)
	{
		this.name = name;
		return this;
	}

	public String build()
	{
		var matcher = PATTERN_ORDERED_NAME.matcher(name);
		if (matcher.matches())
		{
			var s = matcher.group(2);
			if (!s.isEmpty() && !isKnownExtension(s))
			{
				if (s.startsWith("!"))
				{
					return s.substring(1);
				}
				return s;
			}
		}
		return "";
	}

	private boolean isKnownExtension(String name)
	{
		var nameLowerCase = name.toLowerCase(Locale.ROOT);
		return HIDDEN_NAMES.contains(nameLowerCase);
	}
}
