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

package io.xeres.app.util;

import io.xeres.app.util.markdown.Markdown;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import java.util.Locale;

import static io.xeres.app.util.markdown.Markdown.HeaderSize.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class UnHtml
{
	private UnHtml()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static String cleanupChat(String text)
	{
		return Entities.unescape( // &lt; -> <
				Jsoup.clean(text, Safelist.none() // <span> -> nothing
						.addAttributes("img", "src")
						.addProtocols("img", "src", "data")
						.addAttributes("a", "href")
						.addProtocols("a", "href", "retroshare")
						.preserveRelativeLinks(true))
		);
	}

	public static String cleanupMessage(String text)
	{
		// Only process HTML
		if (isBlank(text) ||
				(!text.toLowerCase(Locale.ROOT).startsWith("<body>") &&
						!text.toLowerCase(Locale.ROOT).startsWith("<html>")))
		{
			return text;
		}

		var document = Jsoup.parse(text);
		var cleaner = new Cleaner(Safelist.none()
				.addAttributes("img", "src")
				.addProtocols("img", "src", "data")
				.addAttributes("a", "href")
				.addTags("p", "br", "ul", "li", "h1", "h2", "h3", "h4", "h5", "h6")
				.preserveRelativeLinks(true));

		document = cleaner.clean(document);

		return toMarkdown(document, 0);
	}

	private static String toMarkdown(Element element, int depth)
	{
		if (depth++ > 30)
		{
			throw new IllegalStateException("HTML to markdown depth nesting exceeded");
		}

		var md = new Markdown();

		for (var node : element.childNodes())
		{
			if (node instanceof TextNode textNode)
			{
				md.addText(textNode.text());
			}
			else if (node instanceof Element en)
			{
				var tagName = en.tag().getName();

				switch (tagName)
				{
					case "a" -> md.addUrl(en.text(), en.attr("href"));
					case "p" -> md.addParagraph(toMarkdown(en, depth));
					case "br" -> md.breakLine();
					case "li" -> md.addListItem(toMarkdown(en, depth));
					case "ul" -> md.addList(toMarkdown(en, depth));
					case "h1" -> md.addHeader(toMarkdown(en, depth), H1);
					case "h2" -> md.addHeader(toMarkdown(en, depth), H2);
					case "h3" -> md.addHeader(toMarkdown(en, depth), H3);
					case "h4" -> md.addHeader(toMarkdown(en, depth), H4);
					case "h5" -> md.addHeader(toMarkdown(en, depth), H5);
					case "h6" -> md.addHeader(toMarkdown(en, depth), H6);
					case "img" -> md.addImage(en.text(), en.attr("src"));
					default -> md.addText(toMarkdown(en, depth));
				}
			}
		}
		return md.toString();
	}
}
