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

package io.xeres.app.service;

import org.apache.commons.lang3.StringUtils;
import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.node.*;
import org.commonmark.renderer.markdown.MarkdownRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class UnHtmlService
{
	private static final Logger log = LoggerFactory.getLogger(UnHtmlService.class);

	private final MarkdownRenderer markdownRenderer;

	public UnHtmlService()
	{
		markdownRenderer = MarkdownRenderer.builder()
				.extensions(List.of(StrikethroughExtension.create()))
				.build();
	}

	public String cleanupMessage(String text)
	{
		// Only process HTML
		if (isBlank(text) ||
				(!StringUtils.startsWithIgnoreCase(text, "<body>") &&
						!StringUtils.startsWithIgnoreCase(text, "<html>") &&
						!StringUtils.startsWithIgnoreCase(text, "<a "))) // Also convert certificate links sent by Xeres to RS. One day we'll send them as Markdown too
		{
			return text;
		}

		var document = Jsoup.parse(text);
		var cleaner = new Cleaner(Safelist.none()
				.addAttributes("img", "src", "title", "alt")
				.addProtocols("img", "src", "data")
				.addAttributes("a", "href", "title")
				.addTags("p", "br", "ul", "li", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "blockquote", "ol", "em", "i", "strong", "b", "code", "pre", "del", "s")
				.preserveRelativeLinks(true));

		document = cleaner.clean(document);

		return convertToMarkdown(document);
	}

	private String convertToMarkdown(org.jsoup.nodes.Document jsoupDocument)
	{
		var commonMarkDocument = new org.commonmark.node.Document();

		convertNodes(jsoupDocument.body().childNodes(), commonMarkDocument);

		return markdownRenderer.render(commonMarkDocument);
	}

	private static void convertNodes(List<Node> jsoupNodes, org.commonmark.node.Node commonMarkParent)
	{
		for (Node jsoupNode : jsoupNodes)
		{
			if (jsoupNode instanceof TextNode textNode)
			{
				String text = textNode.text();
				if (!text.trim().isEmpty())
				{
					commonMarkParent.appendChild(new Text(text));
				}
			}
			else if (jsoupNode instanceof Element element)
			{
				var commonMarkNode = createCommonMarkNode(element);

				if (commonMarkNode != null)
				{
					commonMarkParent.appendChild(commonMarkNode);
					// Recursively convert child nodes
					convertNodes(element.childNodes(), commonMarkNode);
				}
				else
				{
					// If the element isn't converted, just convert its children
					convertNodes(element.childNodes(), commonMarkParent);
				}
			}
		}
	}

	private static org.commonmark.node.Node createCommonMarkNode(Element element)
	{
		String tagName = element.tagName().toLowerCase();

		return switch (tagName)
		{
			case "h1" -> createHeading(1);
			case "h2" -> createHeading(2);
			case "h3" -> createHeading(3);
			case "h4" -> createHeading(4);
			case "h5" -> createHeading(5);
			case "h6" -> createHeading(6);
			case "p" -> new Paragraph();
			case "br" -> new HardLineBreak();
			case "hr" -> new ThematicBreak();
			case "blockquote" -> new BlockQuote();
			case "ul" -> new BulletList();
			case "ol" -> new OrderedList();
			case "li" -> new ListItem();
			case "em", "i" -> new Emphasis();
			case "strong", "b" -> new StrongEmphasis();
			case "s", "del" -> new Strikethrough("~");
			case "code" ->
			{
				if (element.parent() != null && "pre".equals(element.parent().tagName().toLowerCase(Locale.ROOT)))
				{
					// The code block is handled by the "pre" element
					yield null;
				}
				var code = new Code();
				if (element.childNodeSize() == 1)
				{
					var node = element.childNode(0);
					if (node instanceof TextNode textNode)
					{
						// Code doesn't handle children
						code.setLiteral(textNode.text());
					}
				}
				yield code;
			}
			case "pre" ->
			{
				var codeBlock = new FencedCodeBlock();
				// Try to detect language
				if (element.childrenSize() == 1 && "code".equals(element.child(0).tagName().toLowerCase(Locale.ROOT)))
				{
					String classNames = element.child(0).className();
					if (!classNames.isEmpty())
					{
						String language = classNames.split("\\s+")[0];
						codeBlock.setInfo(language);
					}
				}
				if (element.childNodeSize() == 1)
				{
					var node = element.childNode(0);
					if (node instanceof TextNode textNode)
					{
						// FencedCodeBlock doesn't handle children
						codeBlock.setLiteral(textNode.text());
					}
				}
				yield codeBlock;
			}
			case "a" ->
			{
				var link = new Link();
				link.setDestination(element.attr("href"));
				link.setTitle(element.attr("title"));
				yield link;
			}
			case "img" ->
			{
				var image = new Image();
				image.setDestination(element.attr("src"));
				image.setTitle(element.attr("title"));
				var altText = new Text(element.attr("alt"));
				image.appendChild(altText);
				yield image;
			}
			default -> null; // For unsupported elements, return null to skip but still process children
		};
	}

	private static Heading createHeading(int level)
	{
		var heading = new Heading();
		heading.setLevel(level);
		return heading;
	}
}
