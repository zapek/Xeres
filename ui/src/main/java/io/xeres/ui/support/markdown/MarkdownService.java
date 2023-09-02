package io.xeres.ui.support.markdown;

import io.xeres.ui.support.contentline.*;
import io.xeres.ui.support.emoji.EmojiService;
import io.xeres.ui.support.uri.UriParser;
import io.xeres.ui.support.util.Range;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

@Service
public class MarkdownService
{
	private static final Pattern BOLD_AND_ITALIC_PATTERN = Pattern.compile("(?<b1>\\*\\*[\\p{L}\\p{Z}\\p{N}\\p{Pd}\\p{Pc}\\p{S}]{1,256}\\*\\*)|(?<i1>\\*[\\p{L}\\p{Z}\\p{N}\\p{Pd}\\p{Pc}\\p{S}]{1,256}\\*)|\\b(?<b2>__[\\p{L}\\p{Z}\\p{N}\\p{Pd}\\p{Pc}\\p{S}]{1,256}__)|\\b(?<i2>_[\\p{L}\\p{Z}\\p{N}\\p{Pd}\\p{Pc}\\p{S}]{1,256}_)");
	private static final Pattern CODE_PATTERN = Pattern.compile("(`.*`)");
	private static final Pattern URL_PATTERN = Pattern.compile("\\b(?<u>(?:https?|ftps?)://[-A-Z0-9+&@#/%?=~_|!:,.;]*[-A-Z0-9+&@#/%=~_|])|(?<e>[0-9A-Z._+\\-=]+@[0-9a-z\\-]+\\.[a-z]{2,})", Pattern.CASE_INSENSITIVE);
	private static final Pattern COLOR_EMOJI_PATTERN = Pattern.compile("(?<e>(&#\\d+;)+)");
	private static final Pattern HREF_PATTERN = Pattern.compile("<a href=\".{1,2083}?\">.{1,256}?</a>", Pattern.CASE_INSENSITIVE);

	private final EmojiService emojiService;

	public MarkdownService(EmojiService emojiService)
	{
		this.emojiService = emojiService;
	}

	public List<Content> parse(String input, boolean oneLineMode)
	{
		var context = new Context(input, oneLineMode);
		return getContent(context);
	}

	public List<Content> getContent(Context context)
	{
		if (context.isEmpty())
		{
			parse(context);
		}
		return context.getContent();
	}

	private void parse(Context context)
	{
		while (context.isIncomplete())
		{
			var line = context.getNextLine();
			line = emojiService.toUnicode(line);

			if (line.startsWith("#"))
			{
				processHeader(context, line);
			}
			else if (line.startsWith("    ") || line.startsWith("\t"))
			{
				processCode(context, line);
			}
			else if (line.contains("`"))
			{
				processPattern(CODE_PATTERN, context, line, (s, groupName) -> context.addContent(new ContentCode(s.substring(1, s.length() - 1))));
			}
			else if (line.contains("&#") && line.contains(";"))
			{
				processPattern(COLOR_EMOJI_PATTERN, context, line,
						(s, groupName) -> context.addContent(new ContentEmoji(emojiService.getEmoji(s))));
			}
			else if (line.contains("*") || line.contains("_"))
			{
				processPattern(BOLD_AND_ITALIC_PATTERN, context, line,
						(s, groupName) -> context.addContent(new ContentEmphasis(s.substring(groupName.startsWith("b") ? 2 : 1, s.length() - (groupName.startsWith("b") ? 2 : 1)), EnumSet.of(groupName.startsWith("b") ? ContentEmphasis.Style.BOLD : ContentEmphasis.Style.ITALIC))));
			}
			else if (line.contains("<a href=")) // inline HTML
			{
				processPattern(HREF_PATTERN, context, line,
						(s, groupName) -> parseHrefs(context, s));
			}
			else if (line.contains("http") || line.contains("ftp") || line.contains("@"))
			{
				processPattern(URL_PATTERN, context, line,
						(s, groupName) -> context.addContent(new ContentUri(s)));
			}
			else
			{
				var verbatim = new ContentText(line);
				verbatim.setComplete();
				context.addContent(verbatim);
			}
		}
	}

	private static void processHeader(Context context, String line)
	{
		char space = '#';
		int size;

		for (size = 0; size < line.length(); size++)
		{
			space = line.charAt(size);
			if (space != '#')
			{
				break;
			}
		}

		if (space != ' ')
		{
			// Not a space, this is not a header
			context.addContent(new ContentText(line));
			return;
		}

		if (size > 6)
		{
			size = 6;
		}
		context.addContent(new ContentHeader(line.substring(size).trim() + context.getLn(), size));
	}

	private static void processPattern(Pattern pattern, Context context, String line, BiConsumer<String, String> match)
	{
		var matcher = pattern.matcher(line);
		var previousRange = new Range(0, 0);

		while (matcher.find())
		{
			var currentRange = new Range(matcher);

			// Before/between matches
			var betweenRange = currentRange.outerRange(previousRange);
			if (betweenRange.hasRange())
			{
				context.addContent(new ContentText(line.substring(betweenRange.start(), betweenRange.end())));
			}

			// Match
			match.accept(line.substring(currentRange.start(), currentRange.end()), currentRange.groupName());

			previousRange = currentRange;
		}

		if (!previousRange.hasRange())
		{
			// If no match at all
			context.addContent(new ContentText(line));
		}
		else if (previousRange.end() < line.length())
		{
			// After the last match
			context.addContent(new ContentText(line.substring(previousRange.end())));
		}
	}

	private static void processCode(Context context, String line)
	{
		context.addContent(new ContentCode(line.trim() + context.getLn()));
	}

	private static void parseHrefs(Context context, String s)
	{
		var document = Jsoup.parse(s);
		var links = document.getElementsByTag("a");
		for (var link : links)
		{
			var href = link.attr("href");
			var text = link.text();
			context.addContent(UriParser.parse(href, text));
		}
	}
}
