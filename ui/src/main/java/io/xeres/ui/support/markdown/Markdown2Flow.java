package io.xeres.ui.support.markdown;

import com.vdurmont.emoji.EmojiParser;
import io.xeres.ui.support.contentline.*;
import io.xeres.ui.support.util.Range;
import io.xeres.ui.support.util.SmileyUtils;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class Markdown2Flow
{
	private static final Pattern BOLD_PATTERN = Pattern.compile("(\\*\\*[^* ]((?!\\*\\*).)*[^* ]\\*\\*)");
	private static final Pattern ITALIC_PATTERN = Pattern.compile("(\\*[^* ]((?!\\*).)*[^* ]\\*)");
	private static final Pattern CODE_PATTERN = Pattern.compile("(`.*`)");

	private String input;
	private final List<Content> content = new ArrayList<>();

	public Markdown2Flow(String input)
	{
		this.input = input;
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

		var scanner = new Scanner(input);
		while (scanner.hasNextLine())
		{
			var line = scanner.nextLine();
			line = line + "\n";
			line = SmileyUtils.smileysToUnicode(line);
			line = EmojiParser.parseToUnicode(line);

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
				processPattern(CODE_PATTERN, line,
						lineCode -> content.add(new ContentCode(lineCode)), 1,
						lineCode -> content.add(new ContentText(lineCode)));
			}
			else if (line.contains("*"))
			{
				processPattern(BOLD_PATTERN, line,
						lineBold -> content.add(new ContentEmphasis(lineBold, EnumSet.of(ContentEmphasis.Style.BOLD))), 2,
						lineBold -> processPattern(ITALIC_PATTERN, lineBold,
								lineItalic -> content.add(new ContentEmphasis(lineItalic, EnumSet.of(ContentEmphasis.Style.ITALIC))), 1,
								lineItalic -> content.add(new ContentText(lineItalic))));
			}
			else
			{
				ContentUtils.parseInlineUrls(line, content);
			}
		}
		scanner.close();
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
		content.add(new ContentHeader(line.substring(size).trim() + "\n", size));
	}

	private void processPattern(Pattern pattern, String line, Consumer<String> match, int matchStrip, Consumer<String> noMatch)
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
				noMatch.accept(line.substring(betweenRange.start(), betweenRange.end()));
			}

			// Match
			match.accept(line.substring(currentRange.start() + matchStrip, currentRange.end() - matchStrip));

			previousRange = currentRange;
		}

		if (!previousRange.hasRange())
		{
			// If no match at all
			noMatch.accept(line);
		}
		else if (previousRange.end() < line.length())
		{
			// After the last match
			noMatch.accept(line.substring(previousRange.end()));
		}
	}

	private void processCode(String line)
	{
		content.add(new ContentCode(line.trim() + "\n"));
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
