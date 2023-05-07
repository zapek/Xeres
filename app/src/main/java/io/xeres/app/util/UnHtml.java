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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

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
		var document = Jsoup.parse(text);
		var cleaner = new Cleaner(Safelist.none()
				.addAttributes("a", "href")
				.addTags("p", "br", "ul", "li")
				.preserveRelativeLinks(true));

		document = cleaner.clean(document);

		return toMarkdown(document);
	}

	private static String toMarkdown(Element element)
	{
		StringBuilder sb = new StringBuilder();

		element.childNodes().forEach(node -> {
			if (node instanceof TextNode textNode)
			{
				if (!sb.isEmpty()) // XXX: probably not ok (should only happend between textnodes
				{
					sb.append(" ");
				}
				sb.append(textNode.text());
			}
			else if (node instanceof Element elementNode)
			{
				var tagName = elementNode.tag().getName();

				switch (tagName)
				{
					case "p" ->
					{
						if (!sb.isEmpty())
						{
							sb.append("\n\n");
						}
						sb.append(toMarkdown(elementNode));
					}
					case "br" ->
					{
						sb.append("\n");
						sb.append(toMarkdown(elementNode));
					}
					case "ul" ->
					{
						sb.append("\n");
						sb.append(toMarkdown(elementNode));
					}
					case "li" ->
					{
						sb.append("\n  - ");
						sb.append(toMarkdown(elementNode));
					}
					default ->
					{
						sb.append(toMarkdown(elementNode));
					}
				}
			}
		});

		return sb.toString();
	}
}
