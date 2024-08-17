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

package io.xeres.ui.support.markdown;

import io.xeres.ui.support.uri.UriFactory;
import org.jsoup.Jsoup;

import java.util.regex.Pattern;

class HrefDetector implements MarkdownDetector
{
	private static final Pattern HREF_PATTERN = Pattern.compile("<a href=\".{1,2083}?\">.{1,256}?</a>", Pattern.CASE_INSENSITIVE);

	@Override
	public boolean isPossibly(String line)
	{
		return line.contains("<a href=");
	}

	@Override
	public void process(Context context, String line)
	{
		MarkdownService.processPattern(HREF_PATTERN, context, line,
				(s, groupName) -> parseHrefs(context, s));
	}

	private static void parseHrefs(Context context, String s)
	{
		var document = Jsoup.parse(s);
		var links = document.getElementsByTag("a");
		for (var link : links)
		{
			var href = link.attr("href");
			var text = link.text();
			context.addContent(UriFactory.createContent(href, text, context.getUriAction()));
		}
	}
}
