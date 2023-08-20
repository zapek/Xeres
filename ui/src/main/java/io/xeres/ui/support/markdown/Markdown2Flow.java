package io.xeres.ui.support.markdown;

import com.vdurmont.emoji.EmojiParser;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentHeader;
import io.xeres.ui.support.contentline.ContentUtils;
import io.xeres.ui.support.util.SmileyUtils;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Markdown2Flow
{
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
			else if (line.contains("*"))
			{
				processBoldAndItalic(line);
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

	private void processBoldAndItalic(String line)
	{
		// we can have **hello *my* world** and also *hello **my** world*. a space after * or ** makes it fail

		// XXX: try bold for now
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
		var skip = 0; // XXX: use an enum or so... 0 = normal, 1 = empty lines, 2 = continuation break

		for (String s : lines)
		{
			if (s.trim().isEmpty())
			{
				// One empty line is treated as a paragraph
				if (skip != 1)
				{
					sb.append("\n\n");
					skip = 1;
				}
			}
			else if (s.startsWith("> "))
			{
				// We don't process quoted text
				skip = 0;
				sb.append(s.stripTrailing()).append("\n");
			}
			else
			{
				// Normal break is treated as continuation
				if (skip == 2)
				{
					sb.append(" ");
				}
				sb.append(s.stripTrailing());
				skip = 2;
			}
		}
		return sb.toString();
	}
}
