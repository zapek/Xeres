package io.xeres.ui.support.markdown;

import com.vdurmont.emoji.EmojiParser;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentHeader;
import io.xeres.ui.support.contentline.ContentText;
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

		var eatNextEmptyLine = false;
		var scanner = new Scanner(input);
		while (scanner.hasNextLine())
		{
			var line = scanner.nextLine();
			if (eatNextEmptyLine && line.isEmpty())
			{
				continue;
			}
			line = line + "\n";
			line = SmileyUtils.smileysToUnicode(line);
			line = EmojiParser.parseToUnicode(line);

			if (line.startsWith("#"))
			{
				processHeader(line);
				eatNextEmptyLine = true;
			}
			else
			{
				content.add(new ContentText(line));
			}
		}
		scanner.close();
	}

	private void processHeader(String line)
	{
		int size;

		for (size = 1; size < line.length(); size++)
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

	/**
	 * Currently removes consecutive empty lines and trailing spaces.
	 */
	static String sanitize(String input)
	{
		var lines = input.split("\n");
		var sb = new StringBuilder();
		var skip = false;

		for (String s : lines)
		{
			if (s.trim().isEmpty())
			{
				if (!skip)
				{
					sb.append("\n");
					skip = true;
				}
			}
			else
			{
				sb.append(s.stripTrailing()).append("\n");
				skip = false;
			}
		}
		return sb.toString();
	}
}
