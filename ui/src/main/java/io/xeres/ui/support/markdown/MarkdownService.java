package io.xeres.ui.support.markdown;

import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.emoji.EmojiService;
import io.xeres.ui.support.util.Range;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

@Service
public class MarkdownService
{
	public enum ParsingMode
	{
		/**
		 * Tries to avoid spurious \n. This is mostly useful for chats where
		 * you want to try to keep it on one line while still allowing to
		 * write multiple lines.
		 */
		ONE_LINER,

		/**
		 * Convert \n at end of lines to spaces.
		 */
		PARAGRAPH, // convert \n at end of lines to spaces
	}

	private final Set<MarkdownDetector> lineDetectors = LinkedHashSet.newLinkedHashSet(2);
	private final Set<MarkdownDetector> substringDetectors = LinkedHashSet.newLinkedHashSet(6);
	private final EmojiService emojiService;

	public MarkdownService(EmojiService emojiService)
	{
		this.emojiService = emojiService;

		lineDetectors.add(new HeaderDetector());
		lineDetectors.add(new CodeIndentDetector());

		substringDetectors.add(new CodeDetector());
		substringDetectors.add(new EmphasisDetector());
		substringDetectors.add(new LinkDetector());
		substringDetectors.add(new HrefDetector());
		substringDetectors.add(new UrlDetector());
		substringDetectors.add(new ImageDetector());
		if (emojiService.isColoredEmojis())
		{
			substringDetectors.add(new EmojiDetector());
		}
	}

	/**
	 * Parses text and generates a markdown content from it.
	 *
	 * @param input the incoming text, possibly annotated with markdown
	 * @param modes the parsing mode
	 * @param uriAction the action to perform when clicking on an url, can be null
	 * @return a list of content nodes
	 */
	public List<Content> parse(String input, Set<ParsingMode> modes, UriAction uriAction)
	{
		var context = new Context(input, emojiService, modes, uriAction != null ? uriAction : uri -> {});
		return getContent(context);
	}

	private List<Content> getContent(Context context)
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
			var line = context.getNextSubstring();

			if (context.isLine())
			{
				line = emojiService.toUnicode(line);

				if (tryProcessor(context, lineDetectors, line))
				{
					continue;
				}
			}

			if (!tryProcessor(context, substringDetectors, line))
			{
				var verbatim = new ContentText(line);
				verbatim.setComplete();
				context.addContent(verbatim);
			}
		}
	}

	private static boolean tryProcessor(Context context, Set<MarkdownDetector> detectors, String line)
	{
		var available = detectors.stream()
				.filter(context::hasNotUsedDetector)
				.toList();

		for (var detector : available)
		{
			if (detector.isPossibly(line))
			{
				context.addDetector(detector);
				detector.process(context, line);
				return true;
			}
		}
		return false;
	}

	static void processPattern(Pattern pattern, Context context, String line, BiConsumer<String, String> match)
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
}
