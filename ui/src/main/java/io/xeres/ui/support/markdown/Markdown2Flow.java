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

import static io.xeres.ui.support.contentline.ContentHeader.HeaderStyle.*;

public class Markdown2Flow
{
	private final String input;
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
		var scanner = new Scanner(input);
		while (scanner.hasNextLine())
		{
			var line = scanner.nextLine() + "\n";
			line = SmileyUtils.smileysToUnicode(line);
			line = EmojiParser.parseToUnicode(line);

			if (line.startsWith("######")) // XXX: write something smarter
			{
				content.add(new ContentHeader(line.substring(6).trim(), H6));
			}
			else if (line.startsWith("#####"))
			{
				content.add(new ContentHeader(line.substring(5).trim(), H5));
			}
			else if (line.startsWith("####"))
			{
				content.add(new ContentHeader(line.substring(4).trim(), H4));
			}
			else if (line.startsWith("###"))
			{
				content.add(new ContentHeader(line.substring(3).trim(), H3));
			}
			else if (line.startsWith("##"))
			{
				content.add(new ContentHeader(line.substring(2).trim(), H2));
			}
			else if (line.startsWith("#"))
			{
				content.add(new ContentHeader(line.substring(1).trim(), H1));
			}
			else
			{
				content.add(new ContentText(line));
			}
		}
		scanner.close();
	}
}
