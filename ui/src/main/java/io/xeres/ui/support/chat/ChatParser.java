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

package io.xeres.ui.support.chat;

import com.vdurmont.emoji.EmojiParser;
import io.xeres.ui.support.util.SmileyUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class ChatParser
{
	private static final Pattern URL_PATTERN = Pattern.compile("\\b((?:https?|ftps?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])");
	private static final String RS_PROTOCOL = "retroshare";
	private static final String RS_HOST = "certificate";
	private static final String RS_QUERY_PARAM = "radix";

	private ChatParser()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static List<ChatContent> parse(String s)
	{
		s = SmileyUtils.smileysToUnicode(s); // ;-)
		s = EmojiParser.parseToUnicode(s); // :wink:

		List<ChatContent> chatContents = new ArrayList<>();
		s = parseHrefs(s, chatContents);
		parseInlineUrls(s, chatContents);
		return chatContents;
	}

	private static String parseHrefs(String s, List<ChatContent> chatContents)
	{
		var document = Jsoup.parse(s);
		var links = document.getElementsByTag("a");
		for (Element link : links)
		{
			var href = link.attr("href");
			var text = link.text();
			chatContents.add(new ChatContentURI(URI.create(getCertificateFromHref(href)), text));
			links.remove();
		}
		return document.text();
	}

	private static String getCertificateFromHref(String href)
	{
		if (isBlank(href))
		{
			return "";
		}

		try
		{
			var uri = new URI(href);
			if (!RS_PROTOCOL.equals(uri.getScheme()))
			{
				return "";
			}
			if (!RS_HOST.equals(uri.getHost()))
			{
				return "";
			}
			var uriComponents = UriComponentsBuilder.fromPath(uri.getPath())
					.query(uri.getQuery())
					.build();

			var parameter = uriComponents.getQueryParams().getFirst(RS_QUERY_PARAM);
			return defaultString(parameter);
		}
		catch (URISyntaxException e)
		{
			return "";
		}
	}

	private static void parseInlineUrls(String s, List<ChatContent> chatContents)
	{
		var matcher = URL_PATTERN.matcher(s);
		var previousRange = new Range(0, 0);

		// Find URLs
		while (matcher.find())
		{
			var currentRange = new Range(matcher);

			// Text before/between URLs
			var betweenRange = currentRange.textRange(previousRange);
			if (betweenRange.hasRange())
			{
				chatContents.add(new ChatContentText(s.substring(betweenRange.start, betweenRange.end)));
			}

			// URL
			chatContents.add(new ChatContentURI(URI.create(s.substring(currentRange.start, currentRange.end))));

			previousRange = currentRange;
		}

		if (!previousRange.hasRange())
		{
			// Text if no URL at all
			chatContents.add(new ChatContentText(s));
		}
		else if (previousRange.end < s.length())
		{
			// Text after the last URL
			chatContents.add(new ChatContentText(s.substring(previousRange.end)));
		}
	}

	private static class Range
	{
		private final int start;
		private final int end;

		public Range(Matcher matcher)
		{
			start = matcher.start(1);
			end = matcher.end();
		}

		public Range(int start, int end)
		{
			this.start = start;
			this.end = end;
		}

		public boolean hasRange()
		{
			return end > start;
		}

		public Range textRange(Range other)
		{
			if (other.start > start)
			{
				// other is after us
				return new Range(end, other.start);
			}
			else
			{
				// other is before us
				return new Range(other.end, start);
			}
		}
	}
}
