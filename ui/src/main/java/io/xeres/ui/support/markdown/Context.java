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
import io.xeres.ui.support.markdown.MarkdownService.ParsingMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

class Context
{
	private enum SANITIZE
	{
		NORMAL, // keep text as it is
		EMPTY_LINES, // remove useless empty lines
		CONTINUATION_BREAK // remove line feed to make a continuation break
	}

	private final Set<ParsingMode> options;
	private final Scanner scanner;
	private final List<Content> content = new ArrayList<>();
	private int insertIndex;
	private int completedIndex;
	private int previousIndex = -1;
	private boolean isLine;

	public Context(String input, Set<ParsingMode> options)
	{
		this.options = options;
		scanner = new Scanner(sanitize(input));
	}

	public boolean isEmpty()
	{
		return content.isEmpty();
	}

	public List<Content> getContent()
	{
		return content;
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
		return scanner.nextLine() + getLn();
	}

	public boolean isLine()
	{
		return isLine;
	}

	public void addContent(Content newContent)
	{
		content.add(insertIndex, newContent);
		if (previousIndex == insertIndex && !newContent.isComplete() && newContent instanceof ContentText contentText)
		{
			contentText.setComplete(); // Detect if we're in an infinite loop (e.g. "*" detected in line, but no matching "*foo*" so add ContentText then run again, etc...)
		}
		previousIndex = insertIndex;
		insertIndex++;
	}

	public String getLn()
	{
		return options.contains(ParsingMode.ONE_LINER) ? "" : "\n";
	}

	/**
	 * Currently removes trailing spaces and handles line feeds:
	 * - one line feed makes the next line is a continuation
	 * - two line feeds make a paragraph
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
				// One empty line is treated as a paragraph
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
			}
			else
			{
				// Normal break is treated as continuation
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
					skip = SANITIZE.NORMAL;
					sb.append(s.stripTrailing()).append("\n");
				}
				else
				{
					sb.append(s.stripTrailing());
					skip = SANITIZE.CONTINUATION_BREAK;
				}
			}
		}
		return sb.toString();
	}
}
