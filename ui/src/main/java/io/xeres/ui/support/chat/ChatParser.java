/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.chat;

import com.vdurmont.emoji.EmojiParser;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentUtils;
import io.xeres.ui.support.uri.UriParser;
import io.xeres.ui.support.util.SmileyUtils;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

public final class ChatParser
{
	private ChatParser()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static List<Content> parse(String s)
	{
		s = SmileyUtils.smileysToUnicode(s); // ;-)
		s = EmojiParser.parseToUnicode(s); // :wink:

		List<Content> contents = new ArrayList<>();
		s = parseHrefs(s, contents);
		ContentUtils.parseInlineUrls(s, contents);
		return contents;
	}

	public static boolean isActionMe(String s)
	{
		return s.startsWith("/me ");
	}

	public static String parseActionMe(String s, String nickname)
	{
		return nickname + " " + s.substring(4);
	}

	private static String parseHrefs(String s, List<Content> contents)
	{
		var document = Jsoup.parse(s);
		var links = document.getElementsByTag("a");
		for (var link : links)
		{
			var href = link.attr("href");
			var text = link.text();
			contents.add(UriParser.parse(href, text));
			links.remove();
		}
		return document.text();
	}
}
