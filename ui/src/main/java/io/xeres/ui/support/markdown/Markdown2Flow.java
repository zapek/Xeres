package io.xeres.ui.support.markdown;

import com.vdurmont.emoji.EmojiParser;
import io.xeres.ui.support.contentline.*;
import io.xeres.ui.support.emoji.EmojiService;
import io.xeres.ui.support.util.Range;
import io.xeres.ui.support.util.SmileyUtils;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public class Markdown2Flow
{
	private static final Pattern BOLD_AND_ITALIC_PATTERN = Pattern.compile("(?<b1>\\*\\*[^* ]((?!\\*\\*).)*[^* ]\\*\\*)|(?<b2>__[^_ ]((?!__).)*[^_ ]__)|(?<i1>\\*[^* ]((?!\\*).)*[^* ]\\*)|(?<i2>_[^_ ]((?!_).)*[^_ ]_)");
	private static final Pattern CODE_PATTERN = Pattern.compile("(`.*`)");
	private static final Pattern URL_PATTERN = Pattern.compile("\\b(?<u>(?:https?|ftps?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])");
	private static final Pattern COLOR_EMOJI = Pattern.compile("(?<e>(&#\\d+;)+)");

	private String input;
	private final List<Content> content = new ArrayList<>();
	private final EmojiService emojiService;
	private Scanner scanner;

	public Markdown2Flow(String input, EmojiService emojiService)
	{
		this.input = input;
		this.emojiService = emojiService;
	}

	public List<Node> getNodes()
	{
		if (content.isEmpty())
		{
			parse();
		}

		return content.stream()
				.map(Content::getNode)
				.toList();
	}

	private void parse()
	{
		input = sanitize(input);

		scanner = new Scanner(input);

		while (isIncomplete())
		{
			var line = getNextLine();
			line = SmileyUtils.smileysToUnicode(line);
			line = EmojiParser.parseToUnicode(line);
			line = EmojiParser.parseToHtmlDecimal(line); // make smileys into decimal html so that they can be detected and colorized

			if (line.startsWith("#"))
			{
				processHeader(line);
			}
			else if (line.startsWith("    ") || line.startsWith("\t"))
			{
				processCode(line);
			}
			else if (line.contains("`"))
			{
				processPattern(CODE_PATTERN, line, (s, groupName) -> addContent(new ContentCode(s.substring(1, s.length() - 1))));
			}
			else if (line.contains("&#") && line.contains(";"))
			{
				processPattern(COLOR_EMOJI, line,
						(s, groupName) -> addContent(new ContentEmoji(emojiService.getEmoji(s))));
			}
			else if (line.contains("*") || line.contains("_"))
			{
				processPattern(BOLD_AND_ITALIC_PATTERN, line,
						(s, groupName) -> addContent(new ContentEmphasis(s.substring(groupName.startsWith("b") ? 2 : 1, s.length() - (groupName.startsWith("b") ? 2 : 1)), EnumSet.of(groupName.startsWith("b") ? ContentEmphasis.Style.BOLD : ContentEmphasis.Style.ITALIC))));
			}
			else if (line.contains("http") || line.contains("ftp"))
			{
				processPattern(URL_PATTERN, line,
						(s, groupName) -> addContent(new ContentUri(s)));
			}
			else
			{
				var verbatim = new ContentText(line);
				verbatim.setComplete();
				addContent(verbatim);
			}
		}
		scanner.close();
	}

	private int insertIndex;
	private int completedIndex;

	private boolean hasIncompleteContent()
	{
		for (int i = completedIndex + 1; i < content.size(); i++)
		{
			var possibleContent = content.get(i);
			if (!possibleContent.isComplete())
			{
				return true;
			}
			completedIndex++;
		}
		return false;
	}

	/**
	 * Checks if there are still newly created ContentText left to be processed or new lines
	 *
	 * @return true if processing remaining
	 */
	private boolean isIncomplete()
	{
		return hasIncompleteContent() || scanner.hasNextLine();
	}

	/**
	 * Gets the next line, either a newly created ContentText or from the scanner
	 *
	 * @return the next line
	 */
	private String getNextLine()
	{
		var nextIndex = completedIndex + 1;
		if (nextIndex < content.size())
		{
			var possibleContent = content.get(nextIndex);
			if (!possibleContent.isComplete())
			{
				content.remove(nextIndex);
				insertIndex = nextIndex;
				return possibleContent.asText();
			}
		}
		return scanner.nextLine() + "\n";
	}

	private int previousIndex = -1;

	private void addContent(Content newContent)
	{
		content.add(insertIndex, newContent);
		if (previousIndex == insertIndex && !newContent.isComplete() && newContent instanceof ContentText contentText)
		{
			contentText.setComplete(); // Detect if we're in an infinite loop (e.g. "*" detected in line, but no matching "*foo*" so add ContentText then run again, etc...)
		}
		previousIndex = insertIndex;
		insertIndex++;
	}

	private void processHeader(String line)
	{
		int size;

		for (size = 0; size < line.length(); size++)
		{
			if (line.charAt(size) != '#')
			{
				break;
			}
		}
		if (size > 6)
		{
			size = 6;
		}
		addContent(new ContentHeader(line.substring(size).trim() + "\n", size));
	}

	private void processPattern(Pattern pattern, String line, BiConsumer<String, String> match)
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
				addContent(new ContentText(line.substring(betweenRange.start(), betweenRange.end())));
			}

			// Match
			match.accept(line.substring(currentRange.start(), currentRange.end()), currentRange.groupName());

			previousRange = currentRange;
		}

		if (!previousRange.hasRange())
		{
			// If no match at all
			addContent(new ContentText(line));
		}
		else if (previousRange.end() < line.length())
		{
			// After the last match
			addContent(new ContentText(line.substring(previousRange.end())));
		}
	}

	private void processCode(String line)
	{
		addContent(new ContentCode(line.trim() + "\n"));
	}

	private enum SANITIZE_MODE
	{
		NORMAL, // keep text as it is
		EMPTY_LINES, // remove useless empty lines
		CONTINUATION_BREAK // remove line feed to make a continuation break
	}

	/**
	 * Currently removes trailing spaces and handles line feeds:
	 * - one line feed makes the next line is a continuation
	 * - two line feeds make a paragraph
	 */
	static String sanitize(String input)
	{
		var lines = input.split("\n");
		var sb = new StringBuilder();
		var skip = SANITIZE_MODE.NORMAL;

		for (String s : lines)
		{
			if (s.trim().isEmpty())
			{
				// One empty line is treated as a paragraph
				if (skip != SANITIZE_MODE.EMPTY_LINES)
				{
					sb.append("\n\n");
					skip = SANITIZE_MODE.EMPTY_LINES;
				}
			}
			else if (s.startsWith("> ") || s.startsWith(">>") || s.startsWith("    ") || s.startsWith("\t"))
			{
				// We don't process quoted text and code
				skip = SANITIZE_MODE.NORMAL;
				sb.append(s.stripTrailing()).append("\n");
			}
			else
			{
				// Normal break is treated as continuation
				if (skip == SANITIZE_MODE.CONTINUATION_BREAK)
				{
					if (s.stripIndent().startsWith("- ") || s.stripIndent().startsWith("* "))
					{
						// Except quoted text
						sb.append("\n");
					}
					else
					{
						sb.append(" ");
					}
				}
				sb.append(s.stripTrailing());
				skip = SANITIZE_MODE.CONTINUATION_BREAK;
			}
		}
		return sb.toString();
	}
}
