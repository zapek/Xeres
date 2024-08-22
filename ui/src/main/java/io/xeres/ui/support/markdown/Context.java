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

import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.emoji.EmojiService;
import io.xeres.ui.support.markdown.MarkdownService.ParsingMode;

import java.util.*;
import java.util.stream.Stream;

class Context
{
	private enum SANITIZE
	{
		NORMAL, // keep text as it is
		EMPTY_LINES, // remove useless empty lines
		CONTINUATION_BREAK // remove line feed to make a continuation break
	}

	private final EmojiService emojiService;
	private final Set<ParsingMode> options;
	private final UriAction uriAction;
	private final Scanner scanner;
	private final List<Content> content = new ArrayList<>();
	private int insertIndex;
	private int completedIndex;
	private int previousIndex = -1;
	private boolean isLine;
	private final Set<MarkdownDetector> usedDetectors = new HashSet<>();
	private int previousDetectorNum;
	private String finalChar = "";

	public Context(String input, EmojiService emojiService, Set<ParsingMode> options, UriAction uriAction)
	{
		this.options = options;
		this.emojiService = emojiService;
		this.uriAction = uriAction;
		if (input.endsWith("\n"))
		{
			finalChar = "\n"; // Do not strip any final \n
		}
		scanner = new Scanner(sanitize(input));
	}

	public boolean isEmpty()
	{
		return content.isEmpty();
	}

	public List<Content> getContent()
	{
		// Remove useless trailing \n, if any
		if (options.contains(ParsingMode.ONE_LINER) && !isEmpty())
		{
			content.getLast().stripTrailingLn();
		}
		return content;
	}

	public EmojiService getEmojiService()
	{
		return emojiService;
	}

	public UriAction getUriAction()
	{
		return uriAction;
	}

	/**
	 * Checks if there are still newly created ContentText left to be processed or new lines
	 *
	 * @return true if processing remaining
	 */
	public boolean isIncomplete()
	{
		return hasIncompleteContent() || scanner.hasNextLine();
	}

	private boolean hasIncompleteContent()
	{
		for (int i = completedIndex; i < content.size(); i++)
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
	 * Gets the next line, either a newly created ContentText or from the scanner
	 *
	 * @return the next line
	 */
	public String getNextSubstring()
	{
		var nextIndex = completedIndex;
		if (nextIndex < content.size())
		{
			var possibleContent = content.get(nextIndex);
			if (!possibleContent.isComplete())
			{
				content.remove(nextIndex);
				insertIndex = nextIndex;
				isLine = false;
				return possibleContent.asText();
			}
		}
		isLine = true;
		return scanner.nextLine() + (scanner.hasNextLine() ? "\n" : finalChar);
	}

	public boolean isLine()
	{
		return isLine;
	}

	public void addDetector(MarkdownDetector detector)
	{
		usedDetectors.add(detector);
	}

	public boolean hasUsedDetector(MarkdownDetector detector)
	{
		return usedDetectors.contains(detector);
	}

	public boolean hasNotUsedDetector(MarkdownDetector detector)
	{
		return !hasUsedDetector(detector);
	}

	public void addContent(Content newContent)
	{
		content.add(insertIndex, newContent);

		// Detect if we're in an infinite loop (e.g. "*" detected in line, but no matching "*foo*" so add ContentText then run again, etc...)
		// We also check that no other detector can run anymore
		if (previousIndex == insertIndex && !newContent.isComplete() && previousDetectorNum == usedDetectors.size() && newContent instanceof ContentText contentText)
		{
			contentText.setComplete();
		}
		previousIndex = insertIndex;
		previousDetectorNum = usedDetectors.size();
		insertIndex++;
		if (newContent.isComplete())
		{
			usedDetectors.clear(); // We're done here
			previousDetectorNum = 0;
		}
	}

	/**
	 * Currently removes trailing spaces and handles line feeds:
	 * <ul>
	 * <li>one line feed makes the next line is a continuation
	 * <li>two line feeds make a paragraph
	 * </ul>
	 */
	private String sanitize(String input)
	{
		var lines = input.split("\n");
		var sb = new StringBuilder();
		var skip = SANITIZE.NORMAL;

		for (String s : lines)
		{
			if (s.trim().isEmpty())
			{
				skip = sanitizeEmptyLine(skip, sb);
			}
			else
			{
				skip = sanitizeContinuation(s, skip, sb);
			}
		}
		return sb.toString();
	}

	/**
	 * Sanitize an empty line by treating it as a paragraph.
	 */
	private static SANITIZE sanitizeEmptyLine(SANITIZE skip, StringBuilder sb)
	{
		if (skip != SANITIZE.EMPTY_LINES)
		{
			if (skip == SANITIZE.CONTINUATION_BREAK)
			{
				sb.append("\n\n");
			}
			else
			{
				sb.append("\n");
			}
			skip = SANITIZE.EMPTY_LINES;
		}
		return skip;
	}

	/**
	 * Sanitize a break by treating it as continuation.
	 */
	private SANITIZE sanitizeContinuation(String s, SANITIZE skip, StringBuilder sb)
	{
		if (skip == SANITIZE.CONTINUATION_BREAK)
		{
			if (!options.contains(ParsingMode.PARAGRAPH) || (s.stripIndent().startsWith("- ") || s.stripIndent().startsWith("* ")))
			{
				// Except quoted text
				sb.append("\n");
			}
			else
			{
				sb.append(" ");
			}
		}
		if (Stream.of("> ", ">>", "    ", "\t").anyMatch(s::startsWith))
		{
			// We don't process quoted text and code
			sb.append(s.stripTrailing()).append("\n");
			skip = SANITIZE.NORMAL;
		}
		else
		{
			sb.append(s.stripTrailing());
			skip = SANITIZE.CONTINUATION_BREAK;
		}
		return skip;
	}
}
