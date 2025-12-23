/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.emoji.EmojiService;
import io.xeres.ui.support.uri.UriService;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.parser.Parser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class MarkdownService
{
	public enum ParsingMode
	{
		/**
		 * Editor mode. Convert soft breaks to spaces, like HTML.
		 * Allows headings and horizontal rules. Anything that cannot be abused in a public chat.
		 */
		PARAGRAPH,
	}

	private final EmojiService emojiService;
	private final UriService uriService;
	private final Parser parser;

	public MarkdownService(EmojiService emojiService, UriService uriService)
	{
		this.emojiService = emojiService;
		this.uriService = uriService;

		parser = Parser.builder()
				.extensions(List.of(
						AutolinkExtension.create(),
						StrikethroughExtension.create()))
				.build();
	}

	/**
	 * Parses text and generates a Markdown content from it. A default action will be
	 * performed when clicking on a link.
	 *
	 * @param input the incoming text, possibly annotated with Markdown
	 * @return a list of content nodes
	 */
	public List<Content> parse(String input, Set<ParsingMode> modes)
	{
		return parse(input, modes, uriService);
	}

	/**
	 * Parses text and generates a Markdown content from it.
	 *
	 * @param input the incoming text, possibly annotated with Markdown
	 * @param uriAction the action to perform when clicking on a url, can be null for no action
	 * @return a list of content nodes
	 */
	public List<Content> parse(String input, Set<ParsingMode> modes, UriAction uriAction)
	{
		var contentRenderer = new ContentRenderer(emojiService, modes.contains(ParsingMode.PARAGRAPH), uriAction != null ? uriAction : _ -> {
		});
		return contentRenderer.render(parser.parse(input));
	}
}
