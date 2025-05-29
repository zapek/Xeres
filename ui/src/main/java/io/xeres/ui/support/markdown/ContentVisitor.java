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

import io.micrometer.common.util.StringUtils;
import io.xeres.ui.support.contentline.*;
import io.xeres.ui.support.emoji.EmojiService;
import io.xeres.ui.support.uri.UriFactory;
import io.xeres.ui.support.util.Range;
import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.node.*;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

class ContentVisitor extends AbstractVisitor
{
	private static final Logger log = LoggerFactory.getLogger(ContentVisitor.class);

	private static final Pattern EMOJI_PATTERN = Pattern.compile("(" +
			"[\\x{1F1E6}-\\x{1F1FF}]{2}" + // Regional Indicator
			"|(\\p{IsEmoji}" + // Emoji Sequence
			"(\\p{IsEmoji_Modifier}" +
			"|\\x{FE0F}\\x{20E3}?" + // Emoji Presentation Sequence (with optional Keycap)
			"|[\\x{E0020}-\\x{E007E}]+\\x{E007F}" + // Emoji Tag Sequence
			")" +
			"|\\p{IsEmoji_Presentation}" + // Single Character Emoji
			")" +
			"(\\x{200D}" + // Emoji Zero-Width Joiner Sequence (ZWJ)
			"(\\p{IsEmoji}" +
			"(\\p{IsEmoji_Modifier}" +
			"|\\x{FE0F}\\x{20E3}?" +
			"|[\\x{E0020}-\\x{E007E}]+\\x{E007F}" +
			")" +
			"|\\p{IsEmoji_Presentation}" +
			")" +
			"){0,256}" +
			")");

	private enum ParsingMode
	{
		NORMAL,
		QUOTE,
		ORDERED
	}

	private final EmojiService emojiService;
	private final UriAction uriAction;

	private final List<Content> content = new ArrayList<>();
	private ParsingMode parsingMode = ParsingMode.NORMAL;
	private int quoteLevel;
	private final boolean paragraph;
	private int listCounter = 1;

	ContentVisitor(EmojiService emojiService, boolean paragraph, UriAction uriAction)
	{
		this.emojiService = emojiService;
		this.paragraph = paragraph;
		this.uriAction = uriAction;
	}

	List<Content> getContent()
	{
		if (!content.isEmpty() && content.getLast() instanceof ContentText contentText)
		{
			// Remove useless last line
			if (contentText.asText().equals("\n"))
			{
				content.removeLast();
			}
		}
		return content;
	}

	@Override
	public void visit(Text text)
	{
		if (parsingMode == ParsingMode.QUOTE)
		{
			content.add(new ContentText(">".repeat(quoteLevel) + " "));
		}

		var s = text.getLiteral();

		s = emojiService.toUnicode(s);

		if (emojiService.isColoredEmojis() && mightContainEmojis(s))
		{
			handleEmojis(s, content);
		}
		else
		{
			content.add(new ContentText(s));
		}
	}

	private void handleEmojis(String line, List<Content> content)
	{
		var matcher = EMOJI_PATTERN.matcher(line);
		var previousRange = new Range(0, 0);

		while (matcher.find())
		{
			var currentRange = new Range(matcher);

			// Before/between matches
			var betweenRange = currentRange.outerRange(previousRange);
			if (betweenRange.hasRange())
			{
				content.add(new ContentText(line.substring(betweenRange.start(), betweenRange.end())));
			}

			// Match
			var range = line.substring(currentRange.start(), currentRange.end());
			content.add(new ContentEmoji(emojiService.getEmoji(range), range));

			previousRange = currentRange;
		}

		if (!previousRange.hasRange())
		{
			// If no match at all
			content.add(new ContentText(line));
		}
		else if (previousRange.end() < line.length())
		{
			// After the last match
			content.add(new ContentText(line.substring(previousRange.end())));
		}
	}

	@Override
	public void visit(Heading heading)
	{
		if (paragraph)
		{
			addEmptyLine();
			content.add(new ContentHeader(getFirstTextChild(heading).orElse(""), heading.getLevel()));
			addEmptyLine();
		}
		else
		{
			content.add(new ContentText("#".repeat(Math.max(0, heading.getLevel())) + " " + getFirstTextChild(heading).orElse("")));
		}
	}

	@Override
	public void visit(SoftLineBreak softLineBreak)
	{
		if (paragraph && parsingMode != ParsingMode.QUOTE)
		{
			content.add(new ContentText(" "));
		}
		else
		{
			addEmptyLine();
		}
	}

	@Override
	public void visit(HardLineBreak hardLineBreak)
	{
		addEmptyLine();
	}

	@Override
	public void visit(Paragraph paragraph)
	{
		if (!isInTightList(paragraph))
		{
			addEmptyLine();
		}
		visitChildren(paragraph);
		if (!isInTightList(paragraph))
		{
			addEmptyLine();
		}
	}

	@Override
	public void visit(Link link)
	{
		// XXX: link.getTitle() is only some title that is added after the link (so not what we want)
		var url = link.getDestination();

		var altTextVisitor = new AltTextVisitor();
		link.accept(altTextVisitor);
		var altText = altTextVisitor.getAltText();

		// Only use the altText if it's different from the URL. Otherwise, this can cause problems (URL decoded but no the text, etc...)
		content.add(UriFactory.createContent(url, url.equals(altText) ? null : altText, uriAction));
	}

	@Override
	public void visit(BlockQuote blockQuote)
	{
		parsingMode = ParsingMode.QUOTE;
		quoteLevel++;
		visitChildren(blockQuote);
		parsingMode = ParsingMode.NORMAL;
		quoteLevel--;
	}

	@Override
	public void visit(BulletList bulletList)
	{
		addEmptyLine();
		visitChildren(bulletList);
	}

	@Override
	public void visit(OrderedList orderedList)
	{
		parsingMode = ParsingMode.ORDERED;
		addEmptyLine();
		visitChildren(orderedList);
		parsingMode = ParsingMode.NORMAL;
		listCounter = 1;
	}

	@Override
	public void visit(ListItem listItem)
	{
		if (parsingMode == ParsingMode.ORDERED)
		{
			content.add(new ContentText(String.format("%3d. ", listCounter++)));
		}
		else
		{
			content.add(new ContentText("• "));
		}
		visitChildren(listItem);
		addEmptyLine();
	}

	@Override
	public void visit(Emphasis emphasis)
	{
		content.add(new ContentEmphasis(getFirstTextChild(emphasis).orElse(""), EnumSet.of(ContentEmphasis.Style.ITALIC)));
	}

	@Override
	public void visit(StrongEmphasis strongEmphasis)
	{
		content.add(new ContentEmphasis(getFirstTextChild(strongEmphasis).orElse(""), EnumSet.of(ContentEmphasis.Style.BOLD)));
	}

	@Override
	public void visit(Code code)
	{
		content.add(new ContentCode(code.getLiteral()));
	}

	@Override
	public void visit(IndentedCodeBlock indentedCodeBlock)
	{
		addEmptyLine();
		content.add(new ContentCode(indentedCodeBlock.getLiteral()));
	}

	@Override
	public void visit(FencedCodeBlock fencedCodeBlock)
	{
		addEmptyLine();
		content.add(new ContentCode(fencedCodeBlock.getLiteral()));
	}

	@Override
	public void visit(HtmlInline htmlInline)
	{
		var html = htmlInline.getLiteral();
		// XXX: warning! I think RS can output the dreaded <img> thing... before some links too! check that and skip if so...
		// XXX: maybe it automatically works already by skipping it? just check...
		if (html.startsWith("<a href=\""))
		{
			var parent = htmlInline.getParent();
			var child = parent.getFirstChild();
			while (child != htmlInline)
			{
				child = child.getNext();
			}
			HtmlInline href = (HtmlInline) child;
			child = child.getNext();
			if (child instanceof Text text)
			{
				addHref(href.getLiteral());
				text.setLiteral(""); // The text is in the hyperlink already, so set it to empty to not have it shown twice
			}
		}
	}

	private void addHref(String html)
	{
		var document = Jsoup.parse(html);
		var links = document.getElementsByTag("a");
		for (var link : links)
		{
			var href = link.attr("href");
			content.add(UriFactory.createContent(href, null, uriAction));
		}
	}

	@Override
	public void visit(CustomNode customNode)
	{
		if (customNode instanceof Strikethrough strikeThrough)
		{
			visit(strikeThrough);
		}
		else
		{
			super.visit(customNode);
		}
	}

	public void visit(Strikethrough strikeThrough)
	{
		content.add(new ContentStrikethrough(getFirstTextChild(strikeThrough).orElse("")));
	}

	@Override
	public void visit(ThematicBreak thematicBreak)
	{
		if (paragraph)
		{
			addEmptyLine();
			content.add(new ContentHorizontalRule());
			addEmptyLine();
		}
		else
		{
			content.add(new ContentText(thematicBreak.getLiteral()));
		}
	}

	@Override
	public void visit(Image image)
	{
		var data = image.getDestination();

		if (StringUtils.isNotBlank(data) && !data.startsWith("data:"))
		{
			return;
		}

		var altTextVisitor = new AltTextVisitor();
		image.accept(altTextVisitor);

		var fxImage = getImage(data);

		if (fxImage != null)
		{
			content.add(new ContentImage(fxImage));
		}
	}

	private static javafx.scene.image.Image getImage(String data)
	{
		javafx.scene.image.Image image = null;
		try
		{
			image = new javafx.scene.image.Image(data);
			if (image.isError())
			{
				image = null;
			}
		}
		catch (IllegalArgumentException e)
		{
			log.error("Error while loading image", e);
		}
		return image;
	}

	private static Optional<String> getFirstTextChild(Node parent)
	{
		var node = parent.getFirstChild();
		if (node instanceof Text text)
		{
			return Optional.of(text.getLiteral());
		}
		return Optional.empty();
	}

	private static boolean isInTightList(Paragraph paragraph)
	{
		Node parent = paragraph.getParent();
		if (parent != null)
		{
			Node gramps = parent.getParent();
			if (gramps instanceof ListBlock list)
			{
				return list.isTight();
			}
		}
		return false;
	}

	private void addEmptyLine()
	{
		if (!content.isEmpty()) // Don't add a useless empty line at the top
		{
			content.add(new ContentText("\n"));
		}
	}

	private static boolean mightContainEmojis(String s)
	{
		return !s.chars().allMatch(c -> c < 128); // Detects non-ASCII
	}
}
